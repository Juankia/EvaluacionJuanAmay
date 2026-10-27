package ec.digitalbanking.linking.application;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Keyed digests (HMAC-SHA256) so that identification numbers and request fingerprints are never
 * stored in clear and cannot be brute-forced from the database alone. The key comes from the
 * environment; production custody (HSM or KMS) is pending (PF4-05).
 */
@ApplicationScoped
public class Digests {

  private final SecretKeySpec key;

  public Digests(@ConfigProperty(name = "linking.digest-key") String key) {
    if (key.length() < 32) {
      throw new IllegalStateException("linking.digest-key must have at least 32 characters");
    }
    this.key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  public String digest(String... parts) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(key);
      return HexFormat.of()
          .formatHex(mac.doFinal(String.join("\u001f", parts).getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC not available", e);
    }
  }
}
