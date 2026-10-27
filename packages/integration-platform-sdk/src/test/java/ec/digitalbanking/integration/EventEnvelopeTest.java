package ec.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.digitalbanking.integration.events.AuditCategory;
import ec.digitalbanking.integration.events.EventEnvelope;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

  @Test
  void rejectsNonPositiveSequence() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new EventEnvelope(
                "e1", "T", 1, "s", "X", "id", 0, Instant.now(), null, AuditCategory.A3, Map.of()));
  }

  @Test
  void authenticationAndObservabilityAreTheOnlyNonFailClosedCategories() {
    assertFalse(AuditCategory.A7.failClosed());
    assertFalse(AuditCategory.A8.failClosed());
    assertTrue(AuditCategory.A1.failClosed());
    assertTrue(AuditCategory.A5.failClosed());
  }
}
