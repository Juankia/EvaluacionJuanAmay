package ec.digitalbanking.integration.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * JSON codec for {@link EventEnvelope} (packages/api-contracts/schemas/event-envelope-v1). Invalid
 * envelopes raise {@link InvalidEventException} so that consumers route them to the dead-letter
 * queue instead of retrying.
 */
public final class EventEnvelopeJson {

  /** Decoded envelope plus the exact JSON text of its payload. */
  public record Decoded(EventEnvelope envelope, String payloadJson) {}

  public static final class InvalidEventException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidEventException(String message) {
      super(message);
    }
  }

  private EventEnvelopeJson() {}

  public static String encode(EventEnvelope e, ObjectMapper mapper) {
    ObjectNode node = mapper.createObjectNode();
    node.put("eventId", e.eventId());
    node.put("eventType", e.eventType());
    node.put("schemaVersion", e.schemaVersion());
    node.put("source", e.source());
    node.put("subjectType", e.subjectType());
    node.put("subjectId", e.subjectId());
    node.put("sequence", e.sequence());
    node.put("occurredAt", e.occurredAt().toString());
    node.put("correlationId", e.correlationId());
    node.put("auditCategory", e.auditCategory().name());
    node.set("payload", mapper.valueToTree(e.payload()));
    try {
      return mapper.writeValueAsString(node);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("event cannot be encoded", ex);
    }
  }

  @SuppressWarnings("unchecked")
  public static Decoded decode(String json, ObjectMapper mapper) {
    JsonNode node;
    try {
      node = mapper.readTree(json);
    } catch (JsonProcessingException ex) {
      throw new InvalidEventException("malformed JSON");
    }
    if (node == null || !node.isObject()) {
      throw new InvalidEventException("envelope is not an object");
    }
    JsonNode payload = node.get("payload");
    if (payload == null || !payload.isObject()) {
      throw new InvalidEventException("payload must be an object");
    }
    try {
      EventEnvelope envelope =
          new EventEnvelope(
              text(node, "eventId"),
              text(node, "eventType"),
              number(node, "schemaVersion"),
              text(node, "source"),
              text(node, "subjectType"),
              text(node, "subjectId"),
              number(node, "sequence"),
              Instant.parse(text(node, "occurredAt")),
              node.hasNonNull("correlationId") ? node.get("correlationId").asText() : null,
              AuditCategory.valueOf(text(node, "auditCategory")),
              mapper.convertValue(payload, Map.class));
      return new Decoded(envelope, mapper.writeValueAsString(payload));
    } catch (IllegalArgumentException | NullPointerException | DateTimeParseException ex) {
      throw new InvalidEventException("invalid envelope: " + ex.getClass().getSimpleName());
    } catch (JsonProcessingException ex) {
      throw new InvalidEventException("payload cannot be serialised");
    }
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || !value.isTextual() || value.asText().isBlank()) {
      throw new InvalidEventException("missing or invalid field " + field);
    }
    return value.asText();
  }

  private static int number(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || !value.canConvertToInt()) {
      throw new InvalidEventException("missing or invalid field " + field);
    }
    return value.asInt();
  }
}
