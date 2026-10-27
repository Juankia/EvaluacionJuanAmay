package ec.digitalbanking.audit.application;

import ec.digitalbanking.audit.domain.AuditRecord;
import ec.digitalbanking.audit.domain.ChainHasher;
import ec.digitalbanking.integration.events.EventEnvelope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;

/** Appends an event to the source's chain. Idempotent by {@code eventId}. */
@ApplicationScoped
public class AppendAuditEvent {

  public enum Result {
    APPENDED,
    DUPLICATE
  }

  private final AuditStore store;
  private final Clock clock;

  public AppendAuditEvent(AuditStore store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  @Transactional
  public Result append(EventEnvelope event, String payloadJson) {
    String previous = store.lockChainHead(event.source());
    // Checked after the lock: concurrent duplicates of the same source are serialised.
    if (store.exists(event.eventId())) {
      return Result.DUPLICATE;
    }
    boolean gap =
        store
            .lastSequence(event.source(), event.subjectId())
            .map(last -> event.sequence() > last + 1)
            .orElse(event.sequence() > 1);
    String hash = ChainHasher.next(previous, event, payloadJson);
    store.append(new AuditRecord(event, payloadJson, Instant.now(clock), gap, previous, hash));
    return Result.APPENDED;
  }
}
