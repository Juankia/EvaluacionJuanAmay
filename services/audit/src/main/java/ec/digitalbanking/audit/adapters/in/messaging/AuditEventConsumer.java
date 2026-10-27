package ec.digitalbanking.audit.adapters.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.digitalbanking.audit.application.AppendAuditEvent;
import ec.digitalbanking.integration.events.EventEnvelopeJson;
import ec.digitalbanking.integration.events.EventEnvelopeJson.InvalidEventException;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Consumes every domain event. Invalid envelopes go straight to the dead-letter queue; transient
 * store failures are retried a bounded number of times and then dead-lettered with an alert log.
 * Nothing is dropped silently (docs/25 §4.3).
 */
@ApplicationScoped
public class AuditEventConsumer {

  private static final Logger LOG = Logger.getLogger(AuditEventConsumer.class);

  private final AppendAuditEvent appendAuditEvent;
  private final ObjectMapper mapper;
  private final int maxAttempts;

  public AuditEventConsumer(
      AppendAuditEvent appendAuditEvent,
      ObjectMapper mapper,
      @ConfigProperty(name = "audit.consumer.max-attempts", defaultValue = "3") int maxAttempts) {
    this.appendAuditEvent = appendAuditEvent;
    this.mapper = mapper;
    this.maxAttempts = maxAttempts;
  }

  @Incoming("audit-events")
  @Blocking
  public CompletionStage<Void> consume(Message<Object> message) {
    EventEnvelopeJson.Decoded decoded;
    try {
      decoded = EventEnvelopeJson.decode(asText(message.getPayload()), mapper);
    } catch (InvalidEventException e) {
      LOG.warnf("ALERT poison message sent to DLQ reason=%s", e.getMessage());
      return message.nack(e);
    }
    for (int attempt = 1; ; attempt++) {
      try {
        AppendAuditEvent.Result result =
            appendAuditEvent.append(decoded.envelope(), decoded.payloadJson());
        LOG.debugf(
            "audit event %s eventId=%s category=%s",
            result, decoded.envelope().eventId(), decoded.envelope().auditCategory());
        return message.ack();
      } catch (RuntimeException e) {
        if (attempt >= maxAttempts) {
          LOG.errorf(
              "ALERT audit append failed after %d attempts eventId=%s; sent to DLQ",
              attempt, decoded.envelope().eventId());
          return message.nack(e);
        }
        sleep(100L * attempt);
      }
    }
  }

  static String asText(Object payload) {
    if (payload instanceof String s) {
      return s;
    }
    if (payload instanceof byte[] bytes) {
      return new String(bytes, StandardCharsets.UTF_8);
    }
    if (payload instanceof Buffer buffer) {
      return buffer.toString(StandardCharsets.UTF_8);
    }
    if (payload instanceof JsonObject json) {
      return json.encode();
    }
    throw new InvalidEventException("unsupported payload type");
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
