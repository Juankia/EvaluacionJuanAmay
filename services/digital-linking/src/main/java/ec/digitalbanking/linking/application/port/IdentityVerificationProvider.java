package ec.digitalbanking.linking.application.port;

/** E1 provider port (KYC). Receives an opaque capture reference, never biometric data. */
public interface IdentityVerificationProvider {

  enum Result {
    VERIFIED,
    REJECTED
  }

  /**
   * @throws ProviderUnavailableException when the provider did not answer or failed technically
   *     (VD-09: never treated as a rejection or an approval)
   */
  Result verify(String captureReference);

  class ProviderUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ProviderUnavailableException(String message) {
      super(message);
    }
  }
}
