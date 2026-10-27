package ec.digitalbanking.audit.adapters;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;

@ApplicationScoped
public class ClockProducer {

  @Produces
  @Singleton
  Clock clock() {
    return Clock.systemUTC();
  }
}
