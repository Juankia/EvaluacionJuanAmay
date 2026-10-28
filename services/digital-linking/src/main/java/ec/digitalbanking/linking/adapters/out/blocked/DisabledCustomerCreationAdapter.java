package ec.digitalbanking.linking.adapters.out.blocked;

import ec.digitalbanking.linking.application.port.CustomerCreationPort;
import ec.digitalbanking.linking.application.port.OperationBlockedException;
import ec.digitalbanking.observability.SensitiveDataMasker;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * DISABLED adapter for E3 (ARG03-M02). It never contacts the Core: customer creation stays blocked
 * until B-05 is lifted. Reaching it is a defect, so it fails closed and raises an alert.
 */
@ApplicationScoped
public class DisabledCustomerCreationAdapter implements CustomerCreationPort {

  private static final Logger LOG = Logger.getLogger(DisabledCustomerCreationAdapter.class);

  @Override
  public void createCustomer(String requestId) {
    LOG.errorf(
        "ALERT blocked operation invoked: E3 customer creation requestId=%s",
        SensitiveDataMasker.maskIdentifier(requestId));
    throw new OperationBlockedException("E3 customer creation", "B-05");
  }
}
