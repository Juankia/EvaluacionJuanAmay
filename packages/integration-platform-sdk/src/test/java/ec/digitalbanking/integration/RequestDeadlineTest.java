package ec.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RequestDeadlineTest {

  private final Clock clock = Clock.fixed(Instant.parse("2026-10-27T10:00:00Z"), ZoneOffset.UTC);

  @Test
  void budgetIsTheMinimumOfRemainingAndLocalTimeout() {
    RequestDeadline deadline = RequestDeadline.parse("2026-10-27T10:00:02Z").orElseThrow();
    assertEquals(Duration.ofSeconds(2), deadline.budget(Duration.ofSeconds(5), clock));
    assertEquals(Duration.ofSeconds(1), deadline.budget(Duration.ofSeconds(1), clock));
  }

  @Test
  void expiredDeadlineHasNoBudget() {
    RequestDeadline deadline = RequestDeadline.parse("2026-10-27T09:59:59Z").orElseThrow();
    assertTrue(deadline.expired(clock));
    assertEquals(Duration.ZERO, deadline.budget(Duration.ofSeconds(5), clock));
  }

  @Test
  void invalidHeaderIsIgnored() {
    assertTrue(RequestDeadline.parse("tomorrow").isEmpty());
  }
}
