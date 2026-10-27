package ec.digitalbanking.integration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Propagated business deadline (docs/05 §5.3, conceptual header {@code X-Request-Deadline}). Each
 * hop uses the minimum between its local timeout and the remaining time.
 */
public record RequestDeadline(Instant at) {

  public static final String HEADER = "X-Request-Deadline";

  public static Optional<RequestDeadline> parse(String headerValue) {
    if (headerValue == null || headerValue.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(new RequestDeadline(Instant.parse(headerValue.trim())));
    } catch (DateTimeParseException invalid) {
      return Optional.empty();
    }
  }

  public Duration budget(Duration localTimeout, Clock clock) {
    Duration remaining = Duration.between(clock.instant(), at);
    if (remaining.isNegative()) {
      return Duration.ZERO;
    }
    return remaining.compareTo(localTimeout) < 0 ? remaining : localTimeout;
  }

  public boolean expired(Clock clock) {
    return !clock.instant().isBefore(at);
  }
}
