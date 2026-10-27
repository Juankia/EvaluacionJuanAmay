package ec.digitalbanking.notifications.application;

import ec.digitalbanking.notifications.domain.Channel;
import ec.digitalbanking.notifications.domain.Notification;

/** Output port for a delivery channel. Implementations must not log recipient data in clear. */
public interface ChannelProvider {

  Channel channel();

  /**
   * Sends one attempt.
   *
   * @throws ChannelUnavailableException when the attempt failed and may be retried
   */
  void send(Notification notification, String recipient);

  class ChannelUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ChannelUnavailableException(String message) {
      super(message);
    }
  }
}
