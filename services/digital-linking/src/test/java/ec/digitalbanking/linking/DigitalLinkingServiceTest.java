package ec.digitalbanking.linking;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.digitalbanking.contracttesting.OpenApiContract;
import ec.digitalbanking.integration.events.EventEnvelope;
import ec.digitalbanking.integration.events.EventEnvelopeJson;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DigitalLinkingServiceTest {

  private static final String CONTRACT = "digital-linking-v1.yaml";
  private static final String BASE = "/v1/onboarding-requests";

  @Inject DataSource dataSource;
  @Inject Flyway flyway;
  @Inject ObjectMapper mapper;
  @Inject TestEventObserver observer;

  private final OpenApiContract contract = OpenApiContract.fromBuild();

  private static String newKey() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  private static String newIdentification() {
    return String.format("%010d", ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
  }

  private static String startBody(String identification) {
    return "{\"identificationType\":\"CEDULA\",\"identificationNumber\":\""
        + identification
        + "\",\"consentAccepted\":true}";
  }

  private Response start(String key, String body) {
    var request = given().contentType(ContentType.JSON).body(body);
    if (key != null) {
      request = request.header("Idempotency-Key", key);
    }
    return request.when().post(BASE);
  }

  private Response verify(String requestId, String key, String capture) {
    return given()
        .contentType(ContentType.JSON)
        .header("Idempotency-Key", key)
        .body("{\"captureReference\":\"" + capture + "\"}")
        .when()
        .post(BASE + "/" + requestId + "/identity-verifications");
  }

  private JsonNode json(Response response) throws Exception {
    return mapper.readTree(response.asString());
  }

  private String startNew() throws Exception {
    Response r = start(newKey(), startBody(newIdentification()));
    assertEquals(201, r.statusCode());
    return json(r).get("requestId").asText();
  }

  private void assertError(Response r, int status, String code) throws Exception {
    assertEquals(status, r.statusCode(), r.asString());
    JsonNode body = json(r);
    contract.assertValid("common-v1.yaml", "ErrorResponse", body);
    assertEquals(code, body.get("code").asText());
  }

  @Test
  void migrationsAreApplied() {
    assertEquals(0, flyway.info().pending().length);
    flyway.validate();
  }

  @Test
  void startCreatesAndReplaysWithTheSameKey() throws Exception {
    String key = newKey();
    String body = startBody(newIdentification());
    Response created = start(key, body);
    assertEquals(201, created.statusCode());
    assertTrue(contract.declaresResponse(CONTRACT, BASE, "post", 201));
    contract.assertValid(CONTRACT, "OnboardingStatusResponse", json(created));
    assertEquals("IN_PROGRESS", json(created).get("channelStatus").asText());

    Response replay = start(key, body);
    assertEquals(200, replay.statusCode());
    assertEquals(json(created).get("requestId"), json(replay).get("requestId"));
  }

  @Test
  void sameKeyWithDifferentContentIsAConflict() throws Exception {
    String key = newKey();
    start(key, startBody(newIdentification()));
    assertError(start(key, startBody(newIdentification())), 409, "IDEMPOTENCY_CONFLICT");
  }

  @Test
  void onlyOneActiveRequestPerIdentification() throws Exception {
    String identification = newIdentification();
    assertEquals(201, start(newKey(), startBody(identification)).statusCode());
    assertError(
        start(newKey(), startBody(identification)), 409, "EQUIVALENT_OPERATION_IN_PROGRESS");
  }

  @Test
  void idempotencyKeyIsRequiredAndValidated() throws Exception {
    assertError(start(null, startBody(newIdentification())), 400, "IDEMPOTENCY_KEY_REQUIRED");
    assertError(start("short", startBody(newIdentification())), 400, "VALIDATION_ERROR");
  }

  @Test
  void invalidBodiesAreRejectedWithoutEchoingValues() throws Exception {
    Response r =
        start(
            newKey(),
            "{\"identificationType\":\"PASSPORT\",\"identificationNumber\":\"12AB\","
                + "\"consentAccepted\":false}");
    assertError(r, 400, "VALIDATION_ERROR");
    assertEquals(3, json(r).get("details").size());
    assertFalse(r.asString().contains("12AB"));
  }

  @Test
  void verifiedIdentityStaysInProgressAndGoesNoFurther() throws Exception {
    String id = startNew();
    Response r = verify(id, newKey(), "ok-capture-1");
    assertEquals(200, r.statusCode());
    contract.assertValid(CONTRACT, "OnboardingStatusResponse", json(r));
    assertEquals("IN_PROGRESS", json(r).get("channelStatus").asText());
    assertEquals("IDENTITY_VERIFIED", stateOf(id));
  }

  @Test
  void rejectedIdentityCannotContinueDigitally() throws Exception {
    String id = startNew();
    Response r = verify(id, newKey(), "reject-capture");
    assertEquals("CANNOT_CONTINUE_DIGITALLY", json(r).get("channelStatus").asText());
  }

  @Test
  void providerUnavailabilityIsNotARejectionAndCanBeRetried() throws Exception {
    String id = startNew();
    Response unavailable = verify(id, newKey(), "unavailable-capture");
    assertError(unavailable, 503, "IDENTITY_PROVIDER_UNAVAILABLE");
    assertEquals("30", unavailable.header("Retry-After"));
    assertEquals(
        "ACTION_REQUIRED",
        json(given().when().get(BASE + "/" + id)).get("channelStatus").asText());

    Response retried = verify(id, newKey(), "ok-capture-2");
    assertEquals(200, retried.statusCode());
    assertEquals("IDENTITY_VERIFIED", stateOf(id));
  }

  @Test
  void providerTimeoutIsTreatedAsUnavailability() throws Exception {
    String id = startNew();
    assertError(verify(id, newKey(), "slow-capture"), 503, "IDENTITY_PROVIDER_UNAVAILABLE");
    assertEquals("IDENTITY_VERIFICATION_UNAVAILABLE", stateOf(id));
  }

  @Test
  void verificationReplayDoesNotCallTheProviderAgain() throws Exception {
    String id = startNew();
    String key = newKey();
    verify(id, key, "ok-capture-3");
    long version = versionOf(id);
    Response replay = verify(id, key, "ok-capture-3");
    assertEquals(200, replay.statusCode());
    assertEquals(version, versionOf(id));
    assertError(verify(id, key, "ok-capture-other"), 409, "IDEMPOTENCY_CONFLICT");
  }

  @Test
  void unknownRequestsAndBlockedStagesAreNotFound() throws Exception {
    assertError(given().when().get(BASE + "/" + UUID.randomUUID()), 404, "RESOURCE_NOT_FOUND");
    assertError(given().when().get(BASE + "/not-a-uuid"), 404, "RESOURCE_NOT_FOUND");
    String id = startNew();
    for (String stage : List.of("customer", "customer-creations", "activation", "activations")) {
      assertError(
          given().contentType(ContentType.JSON).body("{}").when().post(BASE + "/" + id + "/" + stage),
          404,
          "RESOURCE_NOT_FOUND");
    }
  }

  @Test
  void identificationIsNeverStoredInClear() throws Exception {
    String identification = newIdentification();
    String id = json(start(newKey(), startBody(identification))).get("requestId").asText();
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT r.identification_digest, i.fingerprint, o.body"
                    + " FROM onboarding_request r"
                    + " JOIN idempotency_record i ON i.request_id = r.request_id"
                    + " JOIN outbox_event o ON o.body LIKE '%' || r.request_id::text || '%'"
                    + " WHERE r.request_id = ?::uuid")) {
      ps.setString(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        for (int column = 1; column <= 3; column++) {
          assertFalse(rs.getString(column).contains(identification), "column " + column);
        }
      }
    }
  }

  @Test
  void transitionsArePublishedThroughTheOutboxInOrder() throws Exception {
    String id = startNew();
    verify(id, newKey(), "ok-capture-4");
    await()
        .atMost(Duration.ofSeconds(20))
        .until(() -> eventsFor(id).size() == 3 && pendingOutbox(id) == 0);
    List<EventEnvelope> events = eventsFor(id);
    assertEquals(
        List.of(1L, 2L, 3L), events.stream().map(EventEnvelope::sequence).sorted().toList());
    for (EventEnvelope e : events) {
      assertEquals("OnboardingStatusChanged", e.eventType());
      assertEquals("digital-linking", e.source());
      assertEquals("A3", e.auditCategory().name());
      assertNotEquals(null, e.payload().get("channelStatus"));
    }
  }

  private List<EventEnvelope> eventsFor(String requestId) {
    // A shared broker may carry other publishers' messages (including deliberate poison ones).
    return observer.received().stream()
        .flatMap(
            body -> {
              try {
                return java.util.stream.Stream.of(EventEnvelopeJson.decode(body, mapper).envelope());
              } catch (EventEnvelopeJson.InvalidEventException ignored) {
                return java.util.stream.Stream.empty();
              }
            })
        .filter(e -> e.subjectId().equals(requestId))
        .toList();
  }

  private long pendingOutbox(String requestId) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT count(*) FROM outbox_event WHERE published_at IS NULL AND body LIKE ?")) {
      ps.setString(1, "%" + requestId + "%");
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private String stateOf(String requestId) throws Exception {
    return column(requestId, "state");
  }

  private long versionOf(String requestId) throws Exception {
    return Long.parseLong(column(requestId, "version"));
  }

  private String column(String requestId, String name) throws Exception {
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT " + name + "::text FROM onboarding_request WHERE request_id = ?::uuid")) {
      ps.setString(1, requestId);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        return rs.getString(1);
      }
    }
  }
}
