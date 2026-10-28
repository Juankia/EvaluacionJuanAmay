package ec.digitalbanking.linking.adapters.out.messaging;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import javax.sql.DataSource;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.jboss.logging.Logger;

/**
 * Transactional outbox relay (ADR-0015). Publishes pending rows to the {@code domain-events}
 * exchange with the event type as routing key, and marks a row as published only after the broker
 * confirms it. Delivery is at-least-once: consumers deduplicate by eventId. Rows are claimed with
 * {@code SKIP LOCKED} so several instances can run the relay concurrently.
 */
@ApplicationScoped
public class OutboxRelay {

  private static final Logger LOG = Logger.getLogger(OutboxRelay.class);

  @Inject DataSource dataSource;

  @Inject
  @Channel("domain-events")
  MutinyEmitter<String> emitter;

  @ConfigProperty(name = "linking.outbox.batch-size", defaultValue = "100")
  int batchSize;

  @ConfigProperty(name = "linking.outbox.publish-timeout", defaultValue = "PT5S")
  Duration publishTimeout;

  // The initial delay lets reactive messaging wire the channel before the first run.
  @Scheduled(
      every = "${linking.outbox.interval}",
      delayed = "${linking.outbox.initial-delay}",
      concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
  void scheduled() {
    publishPending();
  }

  /** Returns the number of events published in this run. */
  public int publishPending() {
    return QuarkusTransaction.requiringNew().call(this::publishBatch);
  }

  private int publishBatch() throws SQLException {
    int published = 0;
    try (Connection c = dataSource.getConnection();
        PreparedStatement select =
            c.prepareStatement(
                "SELECT event_id, event_type, body FROM outbox_event WHERE published_at IS NULL"
                    + " ORDER BY created_at LIMIT ? FOR UPDATE SKIP LOCKED");
        PreparedStatement mark =
            c.prepareStatement("UPDATE outbox_event SET published_at = ? WHERE event_id = ?")) {
      select.setInt(1, batchSize);
      try (ResultSet rs = select.executeQuery()) {
        while (rs.next()) {
          String eventId = rs.getString(1);
          String eventType = rs.getString(2);
          try {
            send(eventId, eventType, rs.getString(3));
          } catch (RuntimeException e) {
            // Keep what was already confirmed; the rest is retried on the next run.
            LOG.warnf(
                "outbox publish failed eventId=%s type=%s",
                eventId, e.getClass().getSimpleName());
            break;
          }
          mark.setTimestamp(1, Timestamp.from(Instant.now()));
          mark.setString(2, eventId);
          mark.executeUpdate();
          published++;
        }
      }
    }
    return published;
  }

  private void send(String eventId, String eventType, String body) {
    OutgoingRabbitMQMetadata metadata =
        OutgoingRabbitMQMetadata.builder()
            .withRoutingKey(eventType)
            .withMessageId(eventId)
            .withContentType("application/json")
            .build();
    emitter
        .sendMessage(Message.of(body, Metadata.of(metadata)))
        .await()
        .atMost(publishTimeout);
  }
}
