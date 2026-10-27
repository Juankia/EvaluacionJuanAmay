package ec.digitalbanking.integration;

/**
 * Closed taxonomy of external invocation outcomes (ADR-0005 §2). Shared convention only: deciding
 * what to do with each outcome is a local policy of every ACL (ADR-0003).
 */
public enum InvocationOutcome {
  /** Local evidence that no byte left towards the destination or any intermediary. */
  NOT_SENT,
  /** The external system answered this request with a contract-defined no-effect rejection. */
  REJECTED_CONFIRMED,
  /** Anything else. When in doubt, the outcome is UNKNOWN. */
  UNKNOWN,
  /** Definitive success according to the external contract. */
  CONFIRMED;

  /** Only reads may be retried automatically; writes are never retried (ADR-0005 INV-T3). */
  public boolean allowsAutomaticReadRetry() {
    return this == NOT_SENT || this == UNKNOWN;
  }
}
