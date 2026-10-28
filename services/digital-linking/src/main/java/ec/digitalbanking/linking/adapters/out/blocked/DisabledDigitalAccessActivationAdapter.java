package ec.digitalbanking.linking.adapters.out.blocked;

import ec.digitalbanking.linking.application.port.DigitalAccessActivationPort;
import ec.digitalbanking.linking.application.port.OperationBlockedException;
import ec.digitalbanking.observability.SensitiveDataMasker;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * DISABLED adapter for E5 (ARG-03 §7). It never calls the IdP: no digital access is activated in
 * this foundation. Reaching it is a defect, so it fails closed and raises an alert.
 */
@ApplicationScoped
public class DisabledDigitalAccessActivationAdapter implements DigitalAccessActivationPort {

  private static final Logger LOG = Logger.getLogger(DisabledDigitalAccessActivationAdapter.class);

  @Override
  public void activate(String requestId) {
    LOG.errorf(
        "ALERT blocked operation invoked: E5 digital access activation requestId=%s",
        SensitiveDataMasker.maskIdentifier(requestId));
    throw new OperationBlockedException("E5 digital access activation", "ARG-03");
  }
}
