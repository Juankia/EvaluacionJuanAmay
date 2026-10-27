package ec.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class InvocationOutcomeTest {

  @Test
  void confirmedAndRejectedOutcomesAreNeverRetried() {
    assertFalse(InvocationOutcome.CONFIRMED.allowsAutomaticReadRetry());
    assertFalse(InvocationOutcome.REJECTED_CONFIRMED.allowsAutomaticReadRetry());
  }
}
