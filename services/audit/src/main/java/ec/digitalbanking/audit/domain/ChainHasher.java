package ec.digitalbanking.audit.domain;

import ec.digitalbanking.integration.events.EventEnvelope;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 chain per source (PF4-06: partition granularity provisional). Detects modification or
 * deletion of a record when the chain is re-verified. Anyone with write access to the database
 * can recompute the chain, so this is not tamper resistance.
 */
public final class ChainHasher {

  public static final String GENESIS = "0".repeat(64);

  private ChainHasher() {}

  public static String next(String previousHash, EventEnvelope event, String payloadJson) {
    String canonical =
        String.join(
            "|",
            previousHash,
            event.eventId(),
            event.eventType(),
            String.valueOf(event.schemaVersion()),
            event.source(),
            event.subjectType(),
            event.subjectId(),
            String.valueOf(event.sequence()),
            event.auditCategory().name(),
            event.occurredAt().toString(),
            payloadJson);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
