package ec.digitalbanking.integration.events;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Common event envelope (docs/25-contratos-eventos-y-errores.md §4.1). {@code sequence} is
 * monotonic per (source, subjectId) so consumers can order and discard stale state events.
 * Payloads must be minimised: no contact data, no biometric data.
 */
public record EventEnvelope(
    String eventId,
    String eventType,
    int schemaVersion,
    String source,
    String subjectType,
    String subjectId,
    long sequence,
    Instant occurredAt,
    String correlationId,
    AuditCategory auditCategory,
    Map<String, Object> payload) {

  public EventEnvelope {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(eventType, "eventType");
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(subjectType, "subjectType");
    Objects.requireNonNull(subjectId, "subjectId");
    Objects.requireNonNull(occurredAt, "occurredAt");
    Objects.requireNonNull(auditCategory, "auditCategory");
    if (schemaVersion < 1) {
      throw new IllegalArgumentException("schemaVersion must be >= 1");
    }
    if (sequence < 1) {
      throw new IllegalArgumentException("sequence must be >= 1");
    }
    payload = payload == null ? Map.of() : Map.copyOf(payload);
  }
}
