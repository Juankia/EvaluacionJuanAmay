package ec.digitalbanking.linking.application.port;

/**
 * E5 — digital access activation through the IdP linking operation (ADR-0013). NOT AUTHORISED in
 * this foundation: no digital access is activated for any customer. The only implementation is a
 * disabled adapter, and no use case depends on this port.
 */
public interface DigitalAccessActivationPort {

  void activate(String requestId);
}
