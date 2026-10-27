package ec.digitalbanking.observability;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Centralised exception mapping to {@link ErrorResponse}. Unexpected exceptions are logged with
 * their type only (no message, no payload) and answered with INTERNAL_ERROR.
 */
public final class ApiExceptionMappers {

  private static final Logger LOG = Logger.getLogger(ApiExceptionMappers.class);

  private ApiExceptionMappers() {}

  static Response render(int status, ErrorResponse body) {
    Response.ResponseBuilder builder =
        Response.status(status).type(MediaType.APPLICATION_JSON_TYPE).entity(body);
    if (body.retryAfterSeconds() != null) {
      builder.header("Retry-After", body.retryAfterSeconds());
    }
    return builder.build();
  }

  @Provider
  public static class ApiExceptionMapper implements ExceptionMapper<ApiException> {
    @Override
    public Response toResponse(ApiException e) {
      ErrorResponse body = ErrorResponse.of(e.code(), e.retryable());
      if (e.retryAfterSeconds() != null) {
        body = body.withRetryAfter(e.retryAfterSeconds());
      }
      if (e.status() >= 500) {
        LOG.warnf("request failed code=%s", e.code());
      }
      return render(e.status(), body);
    }
  }

  @Provider
  public static class ValidationMapper implements ExceptionMapper<ConstraintViolationException> {
    @Override
    public Response toResponse(ConstraintViolationException e) {
      return render(400, ValidationErrors.from(e));
    }
  }

  @Provider
  public static class WebApplicationMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException e) {
      if (e instanceof NotFoundException || e instanceof NotAllowedException) {
        // Unknown routes (including blocked financial routes) are indistinguishable from missing
        // resources.
        return render(404, ErrorResponse.of("RESOURCE_NOT_FOUND", false));
      }
      if (e instanceof NotAuthorizedException) {
        return render(401, ErrorResponse.of("UNAUTHENTICATED", false));
      }
      int status = e.getResponse() == null ? 500 : e.getResponse().getStatus();
      return switch (status) {
        case 400, 415, 406 -> render(400, ErrorResponse.of("VALIDATION_ERROR", false));
        case 401 -> render(401, ErrorResponse.of("UNAUTHENTICATED", false));
        case 403 -> render(403, ErrorResponse.of("FORBIDDEN", false));
        default -> {
          LOG.warnf("web application exception type=%s status=%d", e.getClass().getName(), status);
          yield render(500, ErrorResponse.of("INTERNAL_ERROR", false));
        }
      };
    }
  }

  @Provider
  public static class FallbackMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception e) {
      LOG.errorf("unexpected failure type=%s", e.getClass().getName());
      // Full detail only at DEBUG: messages may carry input values.
      LOG.debug("unexpected failure detail", e);
      return render(500, ErrorResponse.of("INTERNAL_ERROR", false));
    }
  }
}
