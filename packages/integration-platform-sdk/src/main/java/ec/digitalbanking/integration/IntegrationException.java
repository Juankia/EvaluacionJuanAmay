package ec.digitalbanking.integration;

import java.util.Objects;

/** Technical integration failure carrying its outcome class. Never wraps business rules. */
public class IntegrationException extends RuntimeException {

  private final InvocationOutcome outcome;
  private final String dependency;

  public IntegrationException(
      String dependency, InvocationOutcome outcome, String message, Throwable cause) {
    super(message, cause);
    this.dependency = Objects.requireNonNull(dependency);
    this.outcome = Objects.requireNonNull(outcome);
  }

  public InvocationOutcome outcome() {
    return outcome;
  }

  public String dependency() {
    return dependency;
  }
}
