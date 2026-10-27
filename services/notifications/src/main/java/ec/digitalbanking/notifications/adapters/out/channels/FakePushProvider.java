package ec.digitalbanking.notifications.adapters.out.channels;

import ec.digitalbanking.notifications.domain.Channel;
import jakarta.inject.Singleton;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class FakePushProvider extends FakeChannelProvider {

  public FakePushProvider(
      @ConfigProperty(name = "notifications.fake.push.failing-subject-prefixes")
          List<String> failingPrefixes) {
    super(failingPrefixes);
  }

  @Override
  public Channel channel() {
    return Channel.PUSH;
  }
}
