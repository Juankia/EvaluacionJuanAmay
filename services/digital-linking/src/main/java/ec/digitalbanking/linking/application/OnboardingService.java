package ec.digitalbanking.linking.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.digitalbanking.integration.events.AuditCategory;
import ec.digitalbanking.integration.events.EventEnvelope;
import ec.digitalbanking.integration.events.EventEnvelopeJson;
import ec.digitalbanking.linking.application.port.IdentityVerificationProvider;
import ec.digitalbanking.linking.application.port.IdentityVerificationProvider.ProviderUnavailableException;
import ec.digitalbanking.linking.application.port.OnboardingRepository;
import ec.digitalbanking.linking.application.port.OnboardingRepository.IdempotencyRecord;
import ec.digitalbanking.linking.application.port.OnboardingRepository.UniquenessViolation;
import ec.digitalbanking.linking.domain.OnboardingRequest;
import ec.digitalbanking.linking.domain.OnboardingState;
import ec.digitalbanking.observability.ApiException;
import ec.digitalbanking.observability.CorrelationId;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Safe subset of digital linking: start a request and run identity verification (E1) against the
 * provider port. Each state transition, its idempotency record and its A3 event are written in one
 * local transaction (outbox). The service has no dependency on the blocked ports (customer creation,
 * digital access activation).
 */
@ApplicationScoped
public class OnboardingService {

  public record Result(OnboardingRequest request, boolean created) {}

  static final String SOURCE = "digital-linking";
  static final String SCOPE_START = "start";

  private final OnboardingRepository repository;
  private final IdentityVerificationProvider identityProvider;
  private final Digests digests;
  private final ObjectMapper mapper;
  private final Clock clock;

  public OnboardingService(
      OnboardingRepository repository,
      IdentityVerificationProvider identityProvider,
      Digests digests,
      ObjectMapper mapper,
      Clock clock) {
    this.repository = repository;
    this.identityProvider = identityProvider;
    this.digests = digests;
    this.mapper = mapper;
    this.clock = clock;
  }

  public Result start(String idempotencyKey, String identificationType, String identificationNumber) {
    String fingerprint = digests.digest(SCOPE_START, identificationType, identificationNumber);
    try {
      return QuarkusTransaction.requiringNew()
          .call(() -> startInTransaction(idempotencyKey, identificationType, identificationNumber, fingerprint));
    } catch (UniquenessViolation e) {
      if (e.rule() == UniquenessViolation.Rule.IDEMPOTENCY_KEY) {
        // A concurrent request with the same key won the race: answer as a replay.
        return QuarkusTransaction.requiringNew()
            .call(() -> replay(SCOPE_START, idempotencyKey, fingerprint).orElseThrow());
      }
      throw new ApiException(
          409, "EQUIVALENT_OPERATION_IN_PROGRESS", false, "active request for identification");
    }
  }

  private Result startInTransaction(
      String key, String identificationType, String identificationNumber, String fingerprint) {
    Optional<Result> replay = replay(SCOPE_START, key, fingerprint);
    if (replay.isPresent()) {
      return replay.get();
    }
    Instant now = Instant.now(clock);
    OnboardingRequest request =
        new OnboardingRequest(
            UUID.randomUUID(),
            digests.digest("identification", identificationType, identificationNumber),
            OnboardingState.STARTED,
            1,
            now);
    repository.insertIdempotency(
        new IdempotencyRecord(SCOPE_START, key, fingerprint, request.requestId()));
    repository.insert(request);
    recordEvent(request);
    return new Result(request, true);
  }

  public OnboardingRequest verifyIdentity(UUID requestId, String idempotencyKey, String captureReference) {
    String scope = "verify:" + requestId;
    String fingerprint = digests.digest(scope, captureReference);
    OnboardingRequest inProgress;
    try {
      inProgress =
          QuarkusTransaction.requiringNew()
              .call(() -> beginVerification(requestId, scope, idempotencyKey, fingerprint));
    } catch (UniquenessViolation e) {
      return QuarkusTransaction.requiringNew()
          .call(() -> replay(scope, idempotencyKey, fingerprint).orElseThrow().request());
    }
    if (inProgress.state() != OnboardingState.IDENTITY_VERIFICATION_IN_PROGRESS) {
      return inProgress; // replay or already concluded: the provider is not called again
    }
    OnboardingState outcome;
    try {
      outcome =
          identityProvider.verify(captureReference) == IdentityVerificationProvider.Result.VERIFIED
              ? OnboardingState.IDENTITY_VERIFIED
              : OnboardingState.IDENTITY_REJECTED;
    } catch (ProviderUnavailableException | org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException e) {
      outcome = OnboardingState.IDENTITY_VERIFICATION_UNAVAILABLE;
    }
    OnboardingState target = outcome;
    OnboardingRequest concluded =
        QuarkusTransaction.requiringNew().call(() -> transition(inProgress, target));
    if (target == OnboardingState.IDENTITY_VERIFICATION_UNAVAILABLE) {
      throw new ApiException(
          503, "IDENTITY_PROVIDER_UNAVAILABLE", true, 30, "identity provider unavailable", null);
    }
    return concluded;
  }

  private OnboardingRequest beginVerification(
      UUID requestId, String scope, String key, String fingerprint) {
    Optional<Result> replay = replay(scope, key, fingerprint);
    if (replay.isPresent()) {
      return replay.get().request();
    }
    OnboardingRequest current = repository.find(requestId).orElseThrow(ApiException::notFound);
    if (current.state() == OnboardingState.IDENTITY_VERIFICATION_IN_PROGRESS) {
      throw new ApiException(
          409, "EQUIVALENT_OPERATION_IN_PROGRESS", false, "verification already in progress");
    }
    repository.insertIdempotency(new IdempotencyRecord(scope, key, fingerprint, requestId));
    if (!current.state().canTransitionTo(OnboardingState.IDENTITY_VERIFICATION_IN_PROGRESS)) {
      return current; // already verified or rejected: nothing to do
    }
    return transition(current, OnboardingState.IDENTITY_VERIFICATION_IN_PROGRESS);
  }

  private OnboardingRequest transition(OnboardingRequest current, OnboardingState target) {
    OnboardingRequest next = current.transitionTo(target, Instant.now(clock));
    if (!repository.update(next, current.version())) {
      throw new ApiException(
          409, "EQUIVALENT_OPERATION_IN_PROGRESS", false, "concurrent update of the request");
    }
    recordEvent(next);
    return next;
  }

  public OnboardingRequest status(UUID requestId) {
    return repository.find(requestId).orElseThrow(ApiException::notFound);
  }

  private Optional<Result> replay(String scope, String key, String fingerprint) {
    return repository
        .findIdempotency(scope, key)
        .map(
            existing -> {
              if (!existing.fingerprint().equals(fingerprint)) {
                throw new ApiException(
                    409, "IDEMPOTENCY_CONFLICT", false, "same key with different content");
              }
              return new Result(
                  repository.find(existing.requestId()).orElseThrow(ApiException::notFound), false);
            });
  }

  private void recordEvent(OnboardingRequest request) {
    EventEnvelope event =
        new EventEnvelope(
            UUID.randomUUID().toString(),
            "OnboardingStatusChanged",
            1,
            SOURCE,
            "ONBOARDING_REQUEST",
            request.requestId().toString(),
            request.version(),
            request.updatedAt(),
            CorrelationId.current().orElse(null),
            AuditCategory.A3,
            Map.of(
                "state", request.state().name(),
                "channelStatus", request.state().channelStatus().name()));
    repository.appendOutbox(event.eventId(), event.eventType(), EventEnvelopeJson.encode(event, mapper));
  }
}
