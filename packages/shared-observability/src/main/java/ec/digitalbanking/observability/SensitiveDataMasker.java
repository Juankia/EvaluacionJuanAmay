package ec.digitalbanking.observability;

/**
 * Masks identifiers before they reach logs or non-essential responses (00-estandares §5.6). Never
 * log credentials, tokens, biometric data or full account numbers.
 */
public final class SensitiveDataMasker {

  private SensitiveDataMasker() {}

  public static String maskIdentifier(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    int visible = Math.min(4, value.length() / 3);
    return "*".repeat(value.length() - visible) + value.substring(value.length() - visible);
  }
}
