package ec.digitalbanking.linking.adapters.in.rest;

import ec.digitalbanking.linking.application.OnboardingService;
import ec.digitalbanking.linking.domain.OnboardingRequest;
import ec.digitalbanking.observability.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.UUID;

/**
 * Channel API of the safe subset (packages/api-contracts/openapi/digital-linking-v1.yaml). There
 * is deliberately no route for customer creation (E3) or digital access activation (E5). The
 * applicant session is not authenticated yet (PF4-01); see docs/implementacion/05.
 */
@Path("/v1/onboarding-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OnboardingResource {

  public record StartOnboardingRequest(
      @NotNull @Pattern(regexp = "CEDULA") String identificationType,
      @NotNull @Pattern(regexp = "^[0-9]{10}$") String identificationNumber,
      @NotNull @AssertTrue Boolean consentAccepted) {}

  public record IdentityVerificationRequest(
      @NotNull @Pattern(regexp = "^[A-Za-z0-9_-]{4,64}$") String captureReference) {}

  /** Channel projection only: internal states are never exposed (VD-07). */
  public record OnboardingStatusResponse(UUID requestId, String channelStatus, Instant updatedAt) {

    static OnboardingStatusResponse from(OnboardingRequest r) {
      return new OnboardingStatusResponse(
          r.requestId(), r.state().channelStatus().name(), r.updatedAt());
    }
  }

  static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
  private static final java.util.regex.Pattern IDEMPOTENCY_KEY =
      java.util.regex.Pattern.compile("^[A-Za-z0-9_-]{16,128}$");

  private final OnboardingService service;

  public OnboardingResource(OnboardingService service) {
    this.service = service;
  }

  @POST
  public Response start(
      @HeaderParam(IDEMPOTENCY_HEADER) String idempotencyKey,
      @Valid @NotNull StartOnboardingRequest body) {
    OnboardingService.Result result =
        service.start(
            requireKey(idempotencyKey), body.identificationType(), body.identificationNumber());
    return Response.status(result.created() ? 201 : 200)
        .entity(OnboardingStatusResponse.from(result.request()))
        .build();
  }

  @POST
  @Path("/{requestId}/identity-verifications")
  public OnboardingStatusResponse verifyIdentity(
      @PathParam("requestId") UUID requestId,
      @HeaderParam(IDEMPOTENCY_HEADER) String idempotencyKey,
      @Valid @NotNull IdentityVerificationRequest body) {
    return OnboardingStatusResponse.from(
        service.verifyIdentity(requestId, requireKey(idempotencyKey), body.captureReference()));
  }

  @GET
  @Path("/{requestId}")
  public OnboardingStatusResponse status(@PathParam("requestId") UUID requestId) {
    return OnboardingStatusResponse.from(service.status(requestId));
  }

  static String requireKey(String key) {
    if (key == null || key.isBlank()) {
      throw new ApiException(400, "IDEMPOTENCY_KEY_REQUIRED", false, "missing idempotency key");
    }
    if (!IDEMPOTENCY_KEY.matcher(key).matches()) {
      throw ApiException.validation("malformed idempotency key");
    }
    return key;
  }
}
