package ec.digitalbanking.notifications;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.digitalbanking.contracttesting.OpenApiContract;
import ec.digitalbanking.integration.events.AuditCategory;
import ec.digitalbanking.integration.events.EventEnvelope;
import ec.digitalbanking.integration.events.EventEnvelopeJson;
import ec.digitalbanking.notifications.application.NotificationStore;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

@QuarkusTest
class NotificationsServiceTest {

  @Inject TestChannels channels;
  @Inject NotificationStore store;
  @Inject ObjectMapper mapper;
  @Inject Flyway flyway;

  private final OpenApiContract contract = OpenApiContract.fromBuild();

  private String publish(String subject, long sequence) {
    String id = UUID.randomUUID().toString();
    channels.publish(
        EventEnvelopeJson.encode(
            new EventEnvelope(
                id,
                "OnboardingStatusChanged",
                1,
                "digital-linking",
                "ONBOARDING_REQUEST",
                subject,
                sequence,
                Instant.now(),
                "corr-12345678",
                AuditCategory.A3,
                Map.of("channelStatus", "IN_PROGRESS")),
            mapper));
    return id;
  }

  @Test
  void migrationsAreApplied() {
    assertEquals(0, flyway.info().pending().length);
    flyway.validate();
  }

  @Test
  void deliversEventFromBrokerAndExposesContractValidRecord() throws Exception {
    String id = publish("subj-it-1", 1);
    await().atMost(Duration.ofSeconds(20)).until(() -> store.alreadyProcessed(id));
    String body =
        given().queryParam("eventId", id).when().get("/ops/v1/deliveries").then().statusCode(200)
            .extract().asString();
    var json = mapper.readTree(body);
    assertEquals(1, json.get("items").size());
    contract.assertValid("notifications-v1.yaml", "Delivery", json.get("items").get(0));
    assertEquals("PUSH", json.get("items").get(0).get("channel").asText());
  }

  @Test
  void undeliverableAndPoisonMessagesAreDeadLettered() throws Exception {
    String id = publish("fail-all-it", 1);
    channels.publish("not json at all");
    List<String> deadLetters = new ArrayList<>();
    await()
        .atMost(Duration.ofSeconds(30))
        .until(
            () -> {
              deadLetters.addAll(channels.drainDeadLetters("notifications.events.dlq"));
              return deadLetters.stream().anyMatch(m -> m.contains(id))
                  && deadLetters.stream().anyMatch(m -> m.contains("not json"));
            });
    assertTrue(!store.alreadyProcessed(id));
  }
}
