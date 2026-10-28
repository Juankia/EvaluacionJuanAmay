package ec.digitalbanking.linking.adapters.out.kyc;

import ec.digitalbanking.linking.application.port.IdentityVerificationProvider;
import ec.digitalbanking.observability.SensitiveDataMasker;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

/**
 * MOCK KYC provider: never contacts a real provider (the SDK and server contract are PF4-07). The
 * outcome is driven by the prefix of the opaque capture reference, to exercise rejection,
 * unavailability and timeout locally. There is no retry: a capture already sent is never resent.
 */
@ApplicationScoped
public class MockIdentityVerificationProvider implements IdentityVerificationProvider {

  private static final Logger LOG = Logger.getLogger(MockIdentityVerificationProvider.class);

  private final List<String> rejectedPrefixes;
  private final List<String> unavailablePrefixes;
  private final List<String> slowPrefixes;
  private final long slowMillis;

  public MockIdentityVerificationProvider(
      @ConfigProperty(name = "linking.mock-kyc.rejected-prefixes") List<String> rejectedPrefixes,
      @ConfigProperty(name = "linking.mock-kyc.unavailable-prefixes")
          List<String> unavailablePrefixes,
      @ConfigProperty(name = "linking.mock-kyc.slow-prefixes") List<String> slowPrefixes,
      @ConfigProperty(name = "linking.mock-kyc.slow-millis", defaultValue = "5000")
          long slowMillis) {
    this.rejectedPrefixes = List.copyOf(rejectedPrefixes);
    this.unavailablePrefixes = List.copyOf(unavailablePrefixes);
    this.slowPrefixes = List.copyOf(slowPrefixes);
    this.slowMillis = slowMillis;
  }

  @Override
  @Timeout(2000)
  public Result verify(String captureReference) {
    LOG.infof(
        "MOCK KYC verification (no real provider) capture=%s",
        SensitiveDataMasker.maskIdentifier(captureReference));
    if (matches(unavailablePrefixes, captureReference)) {
      throw new ProviderUnavailableException("mock provider unavailable");
    }
    if (matches(slowPrefixes, captureReference)) {
      sleep(slowMillis);
    }
    return matches(rejectedPrefixes, captureReference) ? Result.REJECTED : Result.VERIFIED;
  }

  private static boolean matches(List<String> prefixes, String value) {
    return prefixes.stream().anyMatch(value::startsWith);
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
