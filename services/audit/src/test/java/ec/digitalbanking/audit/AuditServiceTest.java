package ec.digitalbanking.audit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.digitalbanking.audit.application.AppendAuditEvent;
import ec.digitalbanking.audit.application.AuditStore;
import ec.digitalbanking.audit.application.VerifyChain;
import ec.digitalbanking.contracttesting.OpenApiContract;
import ec.digitalbanking.integration.events.AuditCategory;
import ec.digitalbanking.integration.events.EventEnvelope;
import ec.digitalbanking.integration.events.EventEnvelopeJson;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuditServiceTest {

  @Inject AppendAuditEvent appendAuditEvent;
  @Inject VerifyChain verifyChain;
  @Inject AuditStore store;
  @Inject DataSource dataSource;
  @Inject Flyway flyway;
  @Inject TestChannels channels;
  @Inject ObjectMapper mapper;

  private final OpenApiContract contract = OpenApiContract.fromBuild();

  private static EventEnvelope event(String source, String subject, long sequence) {
    return new EventEnvelope(
        UUID.randomUUID().toString(),
        "OnboardingStatusChanged",
        1,
        source,
        "ONBOARDING_REQUEST",
        subject,
        sequence,
        Instant.parse("2026-10-27T10:00:00Z"),
        "corr-12345678",
        AuditCategory.A3,
        Map.of("channelStatus", "IN_PROGRESS"));
  }

  @Test
  void migrationsAreAppliedAndValid() {
    assertEquals(0, flyway.info().pending().length);
    assertEquals("1", flyway.info().current().getVersion().getVersion());
    flyway.validate();
  }

  @Test
  void appendIsIdempotentByEventId() {
    var e = event("svc-idem", "subj-idem", 1);
    assertEquals(AppendAuditEvent.Result.APPENDED, appendAuditEvent.append(e, "{}"));
    assertEquals(AppendAuditEvent.Result.DUPLICATE, appendAuditEvent.append(e, "{}"));
    assertEquals(1, store.chain("svc-idem").size());
  }

  @Test
  void sequenceGapIsFlaggedNotDropped() {
    appendAuditEvent.append(event("svc-gap", "subj-gap", 1), "{}");
    appendAuditEvent.append(event("svc-gap", "subj-gap", 3), "{}");
    var chain = store.chain("svc-gap");
    assertEquals(2, chain.size());
    assertFalse(chain.get(0).sequenceGap());
    assertTrue(chain.get(1).sequenceGap());
  }

  @Test
  void chainDetectsModificationOfARecord() throws Exception {
    for (int i = 1; i <= 3; i++) {
      appendAuditEvent.append(event("svc-chain", "subj-chain", i), "{\"n\":" + i + "}");
    }
    assertTrue(verifyChain.verify("svc-chain").intact());
    try (Connection c = dataSource.getConnection();
        Statement s = c.createStatement()) {
      s.executeUpdate(
          "UPDATE audit_record SET payload_json = '{\"n\":99}'"
              + " WHERE source = 'svc-chain' AND sequence = 2");
    }
    var outcome = verifyChain.verify("svc-chain");
    assertFalse(outcome.intact());
    assertEquals(1, outcome.verifiedRecords());
  }

  @Test
  void consumesPublishedEventsAndDeadLettersPoisonMessages() throws Exception {
    var e = event("svc-bus", "subj-bus", 1);
    channels.publish(EventEnvelopeJson.encode(e, mapper));
    channels.publish(EventEnvelopeJson.encode(e, mapper)); // duplicate delivery
    channels.publish("{\"eventId\":\"poison-1\",\"auditCategory\":\"A9\"}");

    await().atMost(Duration.ofSeconds(20)).until(() -> store.exists(e.eventId()));
    List<String> deadLetters = new ArrayList<>();
    await()
        .atMost(Duration.ofSeconds(20))
        .until(
            () -> {
              deadLetters.addAll(channels.drainDeadLetters("audit.events.dlq"));
              return deadLetters.stream().anyMatch(m -> m.contains("poison-1"));
            });
    assertEquals(1, store.chain("svc-bus").size());
  }

  @Test
  @TestSecurity(user = "auditor-1", roles = "auditor")
  void auditorQueryMatchesContractAndIsItselfAudited() throws Exception {
    appendAuditEvent.append(event("svc-query", "subj-query", 1), "{}");
    String body =
        given()
            .queryParam("subjectId", "subj-query")
            .when()
            .get("/audit/v1/events")
            .then()
            .statusCode(200)
            .extract()
            .asString();
    var json = mapper.readTree(body);
    assertEquals(1, json.get("items").size());
    contract.assertValid("audit-v1.yaml", "AuditRecord", json.get("items").get(0));
    assertFalse(
        store.findBySubject("auditor-1", Optional.of(AuditCategory.A6), 10).isEmpty(),
        "the query must be recorded as A6");
  }

  @Test
  @TestSecurity(user = "auditor-1", roles = "auditor")
  void invalidQueryReturnsUniformValidationError() throws Exception {
    String body =
        given()
            .queryParam("subjectId", "s")
            .queryParam("limit", 1000)
            .when()
            .get("/audit/v1/events")
            .then()
            .statusCode(400)
            .extract()
            .asString();
    var json = mapper.readTree(body);
    contract.assertValid("common-v1.yaml", "ErrorResponse", json);
    assertEquals("VALIDATION_ERROR", json.get("code").asText());
  }

  @Test
  @TestSecurity(user = "someone", roles = "customer")
  void nonAuditorIsForbidden() {
    given().queryParam("subjectId", "x").when().get("/audit/v1/events").then().statusCode(403);
  }

  @Test
  void anonymousIsUnauthenticated() {
    given().queryParam("subjectId", "x").when().get("/audit/v1/events").then().statusCode(401);
  }

  @Test
  void healthAndMetricsAreExposed() {
    given().when().get("/q/health/ready").then().statusCode(200);
    given().when().get("/q/metrics").then().statusCode(200);
  }
}
