package ec.digitalbanking.observability;

import java.util.List;

/**
 * Uniform technical error contract (docs/25-contratos-eventos-y-errores.md §3). The message shown
 * to the user is resolved by the channel from {@code messageKey}; no internal detail is exposed.
 */
public record ErrorResponse(
    String code,
    String messageKey,
    String correlationId,
    boolean retryable,
    Integer retryAfterSeconds,
    List<FieldViolation> details) {

  public record FieldViolation(String field, String reason) {}

  public static ErrorResponse of(String code, boolean retryable) {
    return new ErrorResponse(
        code,
        "error." + code.toLowerCase(java.util.Locale.ROOT),
        CorrelationId.current().orElse(null),
        retryable,
        null,
        List.of());
  }

  public ErrorResponse withRetryAfter(int seconds) {
    return new ErrorResponse(code, messageKey, correlationId, retryable, seconds, details);
  }

  public ErrorResponse withDetails(List<FieldViolation> violations) {
    return new ErrorResponse(
        code, messageKey, correlationId, retryable, retryAfterSeconds, List.copyOf(violations));
  }
}
