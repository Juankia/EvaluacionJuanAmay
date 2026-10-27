package ec.digitalbanking.integration.events;

/** Audit categories (ADR-0020). Technical classification, not business logic. */
public enum AuditCategory {
  /** Financial command authorization and execution. Fail closed. */
  A1,
  /** Security-sensitive account action. Fail closed. */
  A2,
  /** Identity and digital linking. Fail closed. */
  A3,
  /** Sensitive data access. Fail closed (dual durable path). */
  A4,
  /** Ordinary read. Fails only when both durable paths fail. */
  A5,
  /** Privileged operation. Fail closed. */
  A6,
  /** Authentication event. Asynchronous; loss must be detectable. */
  A7,
  /** Technical observability. Not audit evidence. */
  A8;

  public boolean failClosed() {
    return this != A7 && this != A8;
  }
}
