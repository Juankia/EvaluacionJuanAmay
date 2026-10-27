package ec.digitalbanking.audit.domain;

import ec.digitalbanking.integration.events.EventEnvelope;
import java.time.Instant;

/** An event as recorded by the audit service, with its position in the source's hash chain. */
public record AuditRecord(
    EventEnvelope event,
    String payloadJson,
    Instant recordedAt,
    boolean sequenceGap,
    String previousHash,
    String chainHash) {}
