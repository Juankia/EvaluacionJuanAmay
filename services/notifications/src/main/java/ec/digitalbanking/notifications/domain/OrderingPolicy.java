package ec.digitalbanking.notifications.domain;

import java.util.Optional;

/**
 * NOT-2: a state event older than (or equal to) the last applied one is not notified. Movement
 * events (NOT-1, never discarded, delivered in order with ESPERA_SECUENCIA) are not produced by any
 * implemented service yet; see docs/implementacion/05.
 */
public final class OrderingPolicy {

  private OrderingPolicy() {}

  public static boolean isStale(long sequence, Optional<Long> lastApplied) {
    return lastApplied.map(last -> sequence <= last).orElse(false);
  }
}
