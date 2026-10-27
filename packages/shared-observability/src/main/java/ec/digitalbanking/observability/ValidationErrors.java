package ec.digitalbanking.observability;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;

/** Converts Bean Validation failures into the uniform error contract without echoing values. */
public final class ValidationErrors {

  private ValidationErrors() {}

  public static ErrorResponse from(ConstraintViolationException exception) {
    List<ErrorResponse.FieldViolation> violations =
        exception.getConstraintViolations().stream()
            .map(ValidationErrors::toViolation)
            .sorted(Comparator.comparing(ErrorResponse.FieldViolation::field))
            .toList();
    return ErrorResponse.of("VALIDATION_ERROR", false).withDetails(violations);
  }

  private static ErrorResponse.FieldViolation toViolation(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    int lastDot = path.lastIndexOf('.');
    String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
    return new ErrorResponse.FieldViolation(field, violation.getMessage());
  }
}
