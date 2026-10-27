package ec.digitalbanking.linking.application.port;

import ec.digitalbanking.linking.domain.OnboardingRequest;
import java.util.Optional;
import java.util.UUID;

/** Output port for onboarding persistence. Methods join the caller's transaction. */
public interface OnboardingRepository {

  record IdempotencyRecord(String scope, String key, String fingerprint, UUID requestId) {}

  /** Raised when a uniqueness rule is violated. */
  class UniquenessViolation extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public enum Rule {
      IDEMPOTENCY_KEY,
      ACTIVE_IDENTIFICATION
    }

    private final Rule rule;

    public UniquenessViolation(Rule rule) {
      super("uniqueness violated: " + rule);
      this.rule = rule;
    }

    public Rule rule() {
      return rule;
    }
  }

  Optional<OnboardingRequest> find(UUID requestId);

  void insert(OnboardingRequest request);

  /** Compare-and-set on version. Returns false when another writer won. */
  boolean update(OnboardingRequest updated, long expectedVersion);

  Optional<IdempotencyRecord> findIdempotency(String scope, String key);

  void insertIdempotency(IdempotencyRecord record);

  void appendOutbox(String eventId, String eventType, String body);
}
