package ec.digitalbanking.linking.application.port;

/** Raised by the disabled adapters of blocked operations. */
public class OperationBlockedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public OperationBlockedException(String operation, String blocker) {
    super(operation + " is blocked (" + blocker + ")");
  }
}
