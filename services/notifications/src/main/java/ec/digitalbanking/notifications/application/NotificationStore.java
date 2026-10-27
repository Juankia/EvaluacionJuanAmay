package ec.digitalbanking.notifications.application;

import ec.digitalbanking.notifications.domain.Channel;
import java.util.List;
import java.util.Optional;

/** Output port for consumer state. */
public interface NotificationStore {

  record DeliveryRecord(String eventId, Channel channel, String status, int attempts) {}

  boolean alreadyProcessed(String eventId);

  Optional<Long> lastSequence(String source, String subjectId);

  /** Records the outcome, the deliveries and the subject sequence in one transaction. */
  void complete(
      String eventId,
      String outcome,
      String source,
      String subjectId,
      long sequence,
      List<DeliveryRecord> deliveries);

  List<DeliveryRecord> deliveries(String eventId);
}
