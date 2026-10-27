package ec.digitalbanking.notifications;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

/** Test-only publisher and dead-letter queue reader. */
@ApplicationScoped
public class TestChannels {

  @Inject
  @Channel("test-publisher")
  Emitter<String> publisher;

  @ConfigProperty(name = "rabbitmq-host")
  String host;

  @ConfigProperty(name = "rabbitmq-port")
  int port;

  @ConfigProperty(name = "rabbitmq-username")
  String username;

  @ConfigProperty(name = "rabbitmq-password")
  String password;

  public void publish(String json) {
    publisher.send(json);
  }

  /** Drains the dead-letter queue and returns the message bodies. */
  public List<String> drainDeadLetters(String queue) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(port);
    factory.setUsername(username);
    factory.setPassword(password);
    List<String> bodies = new ArrayList<>();
    try (var connection = factory.newConnection();
        var channel = connection.createChannel()) {
      GetResponse response;
      while ((response = channel.basicGet(queue, true)) != null) {
        bodies.add(new String(response.getBody(), StandardCharsets.UTF_8));
      }
    }
    return bodies;
  }
}
