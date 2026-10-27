package ec.digitalbanking.audit.application;

import ec.digitalbanking.audit.domain.AuditRecord;
import ec.digitalbanking.integration.events.AuditCategory;
import java.util.List;
import java.util.Optional;

/** Output port for audit persistence. Implementations must run inside the caller's transaction. */
public interface AuditStore {

  boolean exists(String eventId);

  /** Locks and returns the chain head of {@code source}, creating it at genesis when absent. */
  String lockChainHead(String source);

  Optional<Long> lastSequence(String source, String subjectId);

  void append(AuditRecord record);

  List<AuditRecord> findBySubject(String subjectId, Optional<AuditCategory> category, int limit);

  /** Records of {@code source} in chain order. */
  List<AuditRecord> chain(String source);

  String chainHead(String source);
}
