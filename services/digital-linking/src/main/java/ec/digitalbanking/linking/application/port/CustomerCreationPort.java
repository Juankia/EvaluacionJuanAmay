package ec.digitalbanking.linking.application.port;

/**
 * E3 — customer creation in the Core. BLOCKED (B-05: execution and recovery design pending;
 * ARG03-M02). The only implementation is a disabled adapter, and no use case depends on this port.
 */
public interface CustomerCreationPort {

  void createCustomer(String requestId);
}
