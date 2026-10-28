package ec.digitalbanking.linking;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/** Test-only consumer bound to the domain-events exchange: records what the relay published. */
@ApplicationScoped
public class TestEventObserver {

  private final List<String> received = new CopyOnWriteArrayList<>();

  @Incoming("test-observer")
  public void observe(Object payload) {
    received.add(
        switch (payload) {
          case String s -> s;
          case byte[] bytes -> new String(bytes, StandardCharsets.UTF_8);
          case Buffer buffer -> buffer.toString(StandardCharsets.UTF_8);
          case JsonObject json -> json.encode();
          default -> payload.toString();
        });
  }

  public List<String> received() {
    return List.copyOf(received);
  }
}
