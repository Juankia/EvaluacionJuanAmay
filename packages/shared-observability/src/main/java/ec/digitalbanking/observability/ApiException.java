package ec.digitalbanking.observability;

/**
 * Technical carrier for an error of the uniform contract. Services throw it from adapters or
 * application code; {@link ApiExceptionMappers} renders it. The message is for logs only and must
 * not contain sensitive data.
 */
public class ApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int status;
  private final String code;
  private final boolean retryable;
  private final Integer retryAfterSeconds;

  public ApiException(int status, String code, boolean retryable, String message) {
    this(status, code, retryable, null, message, null);
  }

  public ApiException(
      int status,
      String code,
      boolean retryable,
      Integer retryAfterSeconds,
      String message,
      Throwable cause) {
    super(message, cause);
    this.status = status;
    this.code = code;
    this.retryable = retryable;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public static ApiException notFound() {
    return new ApiException(404, "RESOURCE_NOT_FOUND", false, "resource not found or not owned");
  }

  public static ApiException validation(String message) {
    return new ApiException(400, "VALIDATION_ERROR", false, message);
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public boolean retryable() {
    return retryable;
  }

  public Integer retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
