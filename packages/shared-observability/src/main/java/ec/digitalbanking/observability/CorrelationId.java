package ec.digitalbanking.observability;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jboss.logging.MDC;

/** Correlation identifier conventions shared by every container (ADR-0003). */
public final class CorrelationId {

  public static final String HEADER = "X-Correlation-Id";
  public static final String MDC_KEY = "correlationId";

  private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

  private CorrelationId() {}

  /**
   * Accepts a caller supplied identifier only when it matches a safe format; otherwise a new one
   * is generated so untrusted input never reaches logs verbatim.
   */
  public static String sanitizeOrGenerate(String candidate) {
    if (candidate != null && VALID.matcher(candidate).matches()) {
      return candidate;
    }
    return UUID.randomUUID().toString();
  }

  public static Optional<String> current() {
    Object value = MDC.get(MDC_KEY);
    return value == null ? Optional.empty() : Optional.of(value.toString());
  }
}
