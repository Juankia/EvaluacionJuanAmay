package ec.digitalbanking.observability;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/** Propagates the current correlation identifier to downstream HTTP calls. */
public class CorrelationIdClientFilter implements ClientRequestFilter {

  @Override
  public void filter(ClientRequestContext request) {
    CorrelationId.current()
        .ifPresent(id -> request.getHeaders().putSingle(CorrelationId.HEADER, id));
  }
}
