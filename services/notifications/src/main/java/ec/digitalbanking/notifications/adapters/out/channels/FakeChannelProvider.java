package ec.digitalbanking.notifications.adapters.out.channels;

import ec.digitalbanking.notifications.application.ChannelProvider;
import ec.digitalbanking.notifications.domain.Channel;
import ec.digitalbanking.notifications.domain.Notification;
import ec.digitalbanking.observability.SensitiveDataMasker;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jboss.logging.Logger;

/**
 * FAKE provider: records the message in memory and logs a masked line. It never contacts a real
 * push or SMS gateway. Subjects whose id starts with a configured prefix fail, to exercise retry,
 * fallback and dead-lettering locally.
 */
public abstract class FakeChannelProvider implements ChannelProvider {

  public record SentMessage(String eventId, String templateKey, String maskedRecipient) {}

  private static final Logger LOG = Logger.getLogger(FakeChannelProvider.class);

  private final List<SentMessage> sent = new CopyOnWriteArrayList<>();
  private final List<String> failingPrefixes;

  protected FakeChannelProvider(List<String> failingPrefixes) {
    this.failingPrefixes = List.copyOf(failingPrefixes);
  }

  @Override
  public void send(Notification notification, String recipient) {
    if (failingPrefixes.stream().anyMatch(notification.subjectId()::startsWith)) {
      throw new ChannelUnavailableException("fake " + channel() + " failure");
    }
    String masked = SensitiveDataMasker.maskIdentifier(recipient);
    sent.add(new SentMessage(notification.eventId(), notification.templateKey(), masked));
    LOG.infof(
        "FAKE %s delivery (no real message sent) eventId=%s template=%s recipient=%s",
        channel(), notification.eventId(), notification.templateKey(), masked);
  }

  public List<SentMessage> sent() {
    return List.copyOf(sent);
  }

  public abstract Channel channel();
}
