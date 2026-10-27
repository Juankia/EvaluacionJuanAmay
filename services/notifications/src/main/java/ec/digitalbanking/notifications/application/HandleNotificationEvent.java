package ec.digitalbanking.notifications.application;

import ec.digitalbanking.integration.events.EventEnvelope;
import ec.digitalbanking.notifications.application.ChannelProvider.ChannelUnavailableException;
import ec.digitalbanking.notifications.application.NotificationStore.DeliveryRecord;
import ec.digitalbanking.notifications.domain.Channel;
import ec.digitalbanking.notifications.domain.Notification;
import ec.digitalbanking.notifications.domain.OrderingPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.arc.All;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Turns a domain event into at most one delivered notification: deduplication by eventId, NOT-2
 * stale-state discard, bounded retry per channel and fallback from push to SMS. When no channel
 * succeeds a {@link DeliveryFailedException} is raised so the message is dead-lettered (never
 * dropped silently). Delivery is at-least-once: a crash between sending and recording may repeat
 * a notification.
 */
@ApplicationScoped
public class HandleNotificationEvent {

  public enum Outcome {
    NOTIFIED,
    DUPLICATE,
    STALE_DISCARDED,
    IGNORED
  }

  public static class DeliveryFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DeliveryFailedException(String message) {
      super(message);
    }
  }

  static final List<Channel> CHANNEL_ORDER = List.of(Channel.PUSH, Channel.SMS);

  private final NotificationStore store;
  private final RecipientResolver recipients;
  private final Map<Channel, ChannelProvider> providers = new EnumMap<>(Channel.class);
  private final int maxAttemptsPerChannel;
  private final long backoffMillis;

  public HandleNotificationEvent(
      NotificationStore store,
      RecipientResolver recipients,
      @All List<ChannelProvider> providers,
      @ConfigProperty(name = "notifications.channel.max-attempts", defaultValue = "3")
          int maxAttemptsPerChannel,
      @ConfigProperty(name = "notifications.channel.backoff-millis", defaultValue = "200")
          long backoffMillis) {
    this.store = store;
    this.recipients = recipients;
    providers.forEach(p -> this.providers.put(p.channel(), p));
    this.maxAttemptsPerChannel = maxAttemptsPerChannel;
    this.backoffMillis = backoffMillis;
  }

  public Outcome handle(EventEnvelope event) {
    if (store.alreadyProcessed(event.eventId())) {
      return Outcome.DUPLICATE;
    }
    Optional<String> template = templateFor(event);
    if (template.isEmpty()) {
      complete(event, Outcome.IGNORED, List.of());
      return Outcome.IGNORED;
    }
    if (OrderingPolicy.isStale(
        event.sequence(), store.lastSequence(event.source(), event.subjectId()))) {
      complete(event, Outcome.STALE_DISCARDED, List.of());
      return Outcome.STALE_DISCARDED;
    }
    Notification notification =
        new Notification(event.eventId(), event.subjectId(), template.get());
    List<DeliveryRecord> deliveries = new ArrayList<>();
    for (Channel channel : CHANNEL_ORDER) {
      ChannelProvider provider = providers.get(channel);
      Optional<String> recipient = recipients.resolve(event.subjectId(), channel);
      if (provider == null || recipient.isEmpty()) {
        continue;
      }
      int attempts = deliver(provider, notification, recipient.get());
      boolean sent = attempts > 0;
      deliveries.add(
          new DeliveryRecord(
              event.eventId(),
              channel,
              sent ? "SENT" : "FAILED",
              sent ? attempts : maxAttemptsPerChannel));
      if (sent) {
        complete(event, Outcome.NOTIFIED, deliveries);
        return Outcome.NOTIFIED;
      }
    }
    throw new DeliveryFailedException("no channel delivered event " + event.eventId());
  }

  /** Returns the attempt number that succeeded, or 0 when every attempt failed. */
  private int deliver(ChannelProvider provider, Notification notification, String recipient) {
    for (int attempt = 1; attempt <= maxAttemptsPerChannel; attempt++) {
      try {
        provider.send(notification, recipient);
        return attempt;
      } catch (ChannelUnavailableException e) {
        if (attempt < maxAttemptsPerChannel) {
          sleep(backoffMillis * attempt);
        }
      }
    }
    return 0;
  }

  private void complete(EventEnvelope event, Outcome outcome, List<DeliveryRecord> deliveries) {
    store.complete(
        event.eventId(),
        outcome.name(),
        event.source(),
        event.subjectId(),
        event.sequence(),
        deliveries);
  }

  static Optional<String> templateFor(EventEnvelope event) {
    if (!"OnboardingStatusChanged".equals(event.eventType())) {
      return Optional.empty();
    }
    Object status = event.payload().get("channelStatus");
    return status == null
        ? Optional.empty()
        : Optional.of("onboarding.status." + status.toString().toLowerCase(Locale.ROOT));
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
