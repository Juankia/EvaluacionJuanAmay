package ec.digitalbanking.contracttesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {

  private final ObjectMapper json = new ObjectMapper();
  private final OpenApiContract contract = OpenApiContract.fromBuild();

  @Test
  void acceptsValidErrorResponse() throws Exception {
    var body =
        json.readTree(
            "{\"code\":\"VALIDATION_ERROR\",\"messageKey\":\"error.validation_error\","
                + "\"correlationId\":\"abc12345\",\"retryable\":false,\"details\":[]}");
    assertTrue(contract.validate("common-v1.yaml", "ErrorResponse", body).isEmpty());
  }

  @Test
  void reportsMissingFieldsUnknownEnumAndCrossFileReferences() throws Exception {
    var body =
        json.readTree(
            "{\"items\":[{\"movementId\":\"m1\",\"bookingDate\":\"2026-10-01\","
                + "\"valueDate\":\"2026-10-01\",\"amount\":{\"value\":\"1\",\"currency\":\"USD\"},"
                + "\"direction\":\"SIDEWAYS\"}],\"asOf\":\"x\",\"freshness\":\"FRESH\"}");
    var errors = contract.validate("bff-v1.yaml", "SessionResponse", body);
    assertEquals(3, errors.size(), errors.toString());
    var movementErrors = contract.validate("inquiries-v1.yaml", "MovementPageResponse", body);
    assertEquals(3, movementErrors.size(), movementErrors.toString());
  }

  @Test
  void transferBoundaryIsMarkedBlocked() {
    assertEquals(
        "BLOCKED", contract.document("transfers-boundary-v1.yaml").path("x-status").asText());
  }
}
