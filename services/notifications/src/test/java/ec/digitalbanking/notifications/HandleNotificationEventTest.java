package ec.digitalbanking.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ec.digitalbanking.integration.events.AuditCategory;
import ec.digitalbanking.integration.events.EventEnvelope;
import ec.digitalbanking.notifications.adapters.out.channels.FakePushProvider;
import ec.digitalbanking.notifications.adapters.out.channels.FakeSmsProvider;
import ec.digitalbanking.notifications.adapters.out.channels.SyntheticRecipientResolver;
import ec.digitalbanking.notifications.application.HandleNotificationEvent;
import ec.digitalbanking.notifications.application.HandleNotificationEvent.DeliveryFailedException;
import ec.digitalbanking.notifications.application.HandleNotificationEvent.Outcome;
import ec.digitalbanking.notifications.application.NotificationStore;
import ec.digitalbanking.notifications.domain.Channel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests with in-memory adapters (no container). */
class HandleNotificationEventTest {

  static class InMemoryStore implements NotificationStore {
    final Set<String> processed = new HashSet<>();
    final Map<String, Long> sequences = new HashMap<>();
    final List<DeliveryRecord> deliveries = new ArrayList<>();

    @Override
    public boolean alreadyProcessed(String eventId) {
      return processed.contains(eventId);
    }

    @Override
    public Optional<Long> lastSequence(String source, String subjectId) {
      return Optional.ofNullable(sequences.get(source + "/" + subjectId));
    }

    @Override
    public void complete(
        String eventId,
        String outcome,
        String source,
        String subjectId,
        long sequence,
        List<DeliveryRecord> d) {
      processed.add(eventId);
      sequences.merge(source + "/" + subjectId, sequence, Math::max);
      deliveries.addAll(d);
    }

    @Override
    public List<DeliveryRecord> deliveries(String eventId) {
      return deliveries.stream().filter(d -> d.eventId().equals(eventId)).toList();
    }
  }

  private final InMemoryStore store = new InMemoryStore();
  private final FakePushProvider push = new FakePushProvider(List.of("fail-push-", "fail-all-"));
  private final FakeSmsProvider sms = new FakeSmsProvider(List.of("fail-all-"));
  private final HandleNotificationEvent handler =
      new HandleNotificationEvent(store, new SyntheticRecipientResolver(), List.of(push, sms), 3, 0);

  private static EventEnvelope event(String id, String subject, long sequence) {
    return new EventEnvelope(
        id,
        "OnboardingStatusChanged",
        1,
        "digital-linking",
        "ONBOARDING_REQUEST",
        subject,
        sequence,
        Instant.now(),
        null,
        AuditCategory.A3,
        Map.of("channelStatus", "ACTION_REQUIRED"));
  }

  @Test
  void deliversOnceAndDeduplicates() {
    assertEquals(Outcome.NOTIFIED, handler.handle(event("e-1", "subj-1", 1)));
    assertEquals(Outcome.DUPLICATE, handler.handle(event("e-1", "subj-1", 1)));
    assertEquals(1, push.sent().size());
    assertEquals("onboarding.status.action_required", push.sent().get(0).templateKey());
  }

  @Test
  void discardsStaleStateEvents() {
    handler.handle(event("e-2", "subj-2", 2));
    assertEquals(Outcome.STALE_DISCARDED, handler.handle(event("e-3", "subj-2", 1)));
    assertEquals(1, push.sent().size());
  }

  @Test
  void fallsBackToSmsAfterBoundedPushRetries() {
    assertEquals(Outcome.NOTIFIED, handler.handle(event("e-4", "fail-push-1", 1)));
    var records = store.deliveries("e-4");
    assertEquals(2, records.size());
    assertEquals(Channel.PUSH, records.get(0).channel());
    assertEquals("FAILED", records.get(0).status());
    assertEquals(3, records.get(0).attempts());
    assertEquals("SENT", records.get(1).status());
    assertEquals(1, sms.sent().size());
  }

  @Test
  void raisesWhenNoChannelDeliversAndDoesNotMarkProcessed() {
    assertThrows(DeliveryFailedException.class, () -> handler.handle(event("e-5", "fail-all-1", 1)));
    assertEquals(false, store.alreadyProcessed("e-5"));
  }

  @Test
  void recipientsAreMaskedInRecords() {
    handler.handle(event("e-6", "subj-6", 1));
    String masked = push.sent().get(0).maskedRecipient();
    assertEquals(false, masked.contains("subj-6"));
  }

  @Test
  void ignoresUnsupportedEventTypes() {
    var other =
        new EventEnvelope(
            "e-7", "SomethingElse", 1, "x", "CUSTOMER", "c", 1, Instant.now(), null,
            AuditCategory.A8, Map.of());
    assertEquals(Outcome.IGNORED, handler.handle(other));
  }
}
