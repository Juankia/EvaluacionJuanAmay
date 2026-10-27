package ec.digitalbanking.observability;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;

/** Reads or creates the correlation identifier and echoes it on every response. */
@Provider
@PreMatching
public class CorrelationIdServerFilter implements ContainerRequestFilter, ContainerResponseFilter {

  static final String PROPERTY = CorrelationIdServerFilter.class.getName();

  @Override
  public void filter(ContainerRequestContext request) {
    String correlationId =
        CorrelationId.sanitizeOrGenerate(request.getHeaderString(CorrelationId.HEADER));
    request.setProperty(PROPERTY, correlationId);
    MDC.put(CorrelationId.MDC_KEY, correlationId);
  }

  @Override
  public void filter(ContainerRequestContext request, ContainerResponseContext response) {
    Object correlationId = request.getProperty(PROPERTY);
    if (correlationId != null) {
      response.getHeaders().putSingle(CorrelationId.HEADER, correlationId.toString());
    }
    MDC.remove(CorrelationId.MDC_KEY);
  }
}
