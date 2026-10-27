package ec.digitalbanking.notifications.adapters.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.digitalbanking.integration.events.EventEnvelope;
import ec.digitalbanking.integration.events.EventEnvelopeJson;
import ec.digitalbanking.integration.events.EventEnvelopeJson.InvalidEventException;
import ec.digitalbanking.notifications.application.HandleNotificationEvent;
import ec.digitalbanking.notifications.application.HandleNotificationEvent.DeliveryFailedException;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/** Consumes notification-relevant events. Failures are dead-lettered with an alert log. */
@ApplicationScoped
public class NotificationEventConsumer {

  private static final Logger LOG = Logger.getLogger(NotificationEventConsumer.class);

  private final HandleNotificationEvent handler;
  private final ObjectMapper mapper;

  public NotificationEventConsumer(HandleNotificationEvent handler, ObjectMapper mapper) {
    this.handler = handler;
    this.mapper = mapper;
  }

  @Incoming("notification-events")
  @Blocking
  public CompletionStage<Void> consume(Message<Object> message) {
    EventEnvelope event;
    try {
      event = EventEnvelopeJson.decode(asText(message.getPayload()), mapper).envelope();
    } catch (InvalidEventException e) {
      LOG.warnf("ALERT poison message sent to DLQ reason=%s", e.getMessage());
      return message.nack(e);
    }
    try {
      HandleNotificationEvent.Outcome outcome = handler.handle(event);
      LOG.debugf("notification outcome=%s eventId=%s", outcome, event.eventId());
      return message.ack();
    } catch (DeliveryFailedException | IllegalStateException e) {
      LOG.errorf(
          "ALERT notification not delivered eventId=%s type=%s; sent to DLQ",
          event.eventId(), e.getClass().getSimpleName());
      return message.nack(e);
    }
  }

  private static String asText(Object payload) {
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
}
