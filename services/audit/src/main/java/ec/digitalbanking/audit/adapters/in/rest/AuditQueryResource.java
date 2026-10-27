package ec.digitalbanking.audit.adapters.in.rest;

import ec.digitalbanking.audit.application.AppendAuditEvent;
import ec.digitalbanking.audit.application.AuditStore;
import ec.digitalbanking.audit.domain.AuditRecord;
import ec.digitalbanking.integration.events.AuditCategory;
import ec.digitalbanking.integration.events.EventEnvelope;
import ec.digitalbanking.observability.CorrelationId;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Read API for auditors (docs/25 §2.6). Every query is itself recorded as A6. */
@Path("/audit/v1/events")
@Produces(MediaType.APPLICATION_JSON)
public class AuditQueryResource {

  public record AuditRecordResponse(
      String eventId,
      String eventType,
      String source,
      String subjectId,
      String auditCategory,
      Instant occurredAt,
      Instant recordedAt,
      boolean sequenceGap,
      String chainHash) {

    static AuditRecordResponse from(AuditRecord r) {
      EventEnvelope e = r.event();
      return new AuditRecordResponse(
          e.eventId(),
          e.eventType(),
          e.source(),
          e.subjectId(),
          e.auditCategory().name(),
          e.occurredAt(),
          r.recordedAt(),
          r.sequenceGap(),
          r.chainHash());
    }
  }

  public record AuditSearchResponse(List<AuditRecordResponse> items) {}

  static final String SOURCE = "audit-service";

  private final AuditStore store;
  private final AppendAuditEvent appendAuditEvent;
  private final SecurityIdentity identity;
  private final Clock clock;

  public AuditQueryResource(
      AuditStore store, AppendAuditEvent appendAuditEvent, SecurityIdentity identity, Clock clock) {
    this.store = store;
    this.appendAuditEvent = appendAuditEvent;
    this.identity = identity;
    this.clock = clock;
  }

  @GET
  @RolesAllowed("auditor")
  public AuditSearchResponse search(
      @QueryParam("subjectId") @NotBlank @Size(max = 64) String subjectId,
      @QueryParam("category") @Pattern(regexp = "A[1-8]") String category,
      @QueryParam("limit") @DefaultValue("50") @Min(1) @Max(100) int limit) {
    Optional<AuditCategory> filter = Optional.ofNullable(category).map(AuditCategory::valueOf);
    // The privileged query is recorded before results are returned (A6, fail closed).
    String auditor = identity.getPrincipal().getName();
    appendAuditEvent.append(
        new EventEnvelope(
            UUID.randomUUID().toString(),
            "AuditQueryPerformed",
            1,
            SOURCE,
            "SESSION",
            auditor,
            1,
            Instant.now(clock),
            CorrelationId.current().orElse(null),
            AuditCategory.A6,
            Map.of()),
        "{\"filterCategory\":\"" + (category == null ? "" : category) + "\"}");
    return new AuditSearchResponse(
        store.findBySubject(subjectId, filter, limit).stream()
            .map(AuditRecordResponse::from)
            .toList());
  }
}
