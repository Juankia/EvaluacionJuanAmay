package ec.digitalbanking.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.digitalbanking.integration.events.AuditCategory;
import ec.digitalbanking.integration.events.EventEnvelope;
import ec.digitalbanking.integration.events.EventEnvelopeJson;
import ec.digitalbanking.integration.events.EventEnvelopeJson.InvalidEventException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventEnvelopeJsonTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void roundTrips() {
    var event =
        new EventEnvelope(
            "evt-00000001",
            "OnboardingStatusChanged",
            1,
            "digital-linking",
            "ONBOARDING_REQUEST",
            "req-1",
            2,
            Instant.parse("2026-10-27T10:00:00Z"),
            "corr-1234",
            AuditCategory.A3,
            Map.of("channelStatus", "IN_PROGRESS"));
    var decoded = EventEnvelopeJson.decode(EventEnvelopeJson.encode(event, mapper), mapper);
    assertEquals(event, decoded.envelope());
    assertEquals("{\"channelStatus\":\"IN_PROGRESS\"}", decoded.payloadJson());
  }

  @Test
  void rejectsUnknownCategoryAndMalformedInput() {
    assertThrows(InvalidEventException.class, () -> EventEnvelopeJson.decode("{oops", mapper));
    String badCategory =
        "{\"eventId\":\"e1\",\"eventType\":\"T\",\"schemaVersion\":1,\"source\":\"s\","
            + "\"subjectType\":\"CUSTOMER\",\"subjectId\":\"c\",\"sequence\":1,"
            + "\"occurredAt\":\"2026-10-27T10:00:00Z\",\"auditCategory\":\"A9\",\"payload\":{}}";
    assertThrows(InvalidEventException.class, () -> EventEnvelopeJson.decode(badCategory, mapper));
  }
}
