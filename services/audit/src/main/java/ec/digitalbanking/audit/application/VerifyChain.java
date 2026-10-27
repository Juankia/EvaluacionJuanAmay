package ec.digitalbanking.audit.application;

import ec.digitalbanking.audit.domain.AuditRecord;
import ec.digitalbanking.audit.domain.ChainHasher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;

/** Recomputes a source's chain and reports the first record that no longer matches. */
@ApplicationScoped
public class VerifyChain {

  public record Outcome(boolean intact, long verifiedRecords, Optional<String> firstBrokenEventId) {}

  private final AuditStore store;

  public VerifyChain(AuditStore store) {
    this.store = store;
  }

  @Transactional
  public Outcome verify(String source) {
    String expectedPrevious = ChainHasher.GENESIS;
    long count = 0;
    for (AuditRecord record : store.chain(source)) {
      String recomputed = ChainHasher.next(expectedPrevious, record.event(), record.payloadJson());
      if (!record.previousHash().equals(expectedPrevious) || !record.chainHash().equals(recomputed)) {
        return new Outcome(false, count, Optional.of(record.event().eventId()));
      }
      expectedPrevious = record.chainHash();
      count++;
    }
    boolean headMatches = expectedPrevious.equals(store.chainHead(source));
    return new Outcome(headMatches, count, Optional.empty());
  }
}
