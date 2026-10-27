package ec.digitalbanking.coresimulator;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Read-only simulator endpoints. Fault injection through {@code X-Sim-Fault}: {@code unavailable}
 * returns 503 and {@code slow} delays the answer, to exercise ACL failure paths.
 */
@Path("/core-sim/v1")
@Produces(MediaType.APPLICATION_JSON)
public class CoreSimulatorResource {

  private final SyntheticLedger ledger;

  public CoreSimulatorResource(SyntheticLedger ledger) {
    this.ledger = ledger;
  }

  @GET
  @Path("/customers/{customerId}/accounts")
  public Response accounts(
      @PathParam("customerId") String customerId, @HeaderParam("X-Sim-Fault") String fault) {
    Response faulted = fault(fault);
    if (faulted != null) {
      return faulted;
    }
    return ledger
        .accountsOf(customerId)
        .map(accounts -> Response.ok(accounts).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).entity(new LegacyError("CLI-404")).build());
  }

  @GET
  @Path("/accounts/{accountId}/movements")
  public Response movements(
      @PathParam("accountId") String accountId,
      @QueryParam("offset") @DefaultValue("0") int offset,
      @QueryParam("limit") @DefaultValue("20") int limit,
      @HeaderParam("X-Sim-Fault") String fault) {
    Response faulted = fault(fault);
    if (faulted != null) {
      return faulted;
    }
    if (offset < 0 || limit < 1 || limit > 100) {
      return Response.status(Response.Status.BAD_REQUEST).entity(new LegacyError("PAR-400")).build();
    }
    return ledger
        .movementsOf(accountId)
        .map(all -> Response.ok(page(all, offset, limit)).build())
        .orElse(Response.status(Response.Status.NOT_FOUND).entity(new LegacyError("CTA-404")).build());
  }

  private static LegacyMovementPage page(
      List<SyntheticLedger.LegacyMovement> all, int offset, int limit) {
    int from = Math.min(offset, all.size());
    int to = Math.min(from + limit, all.size());
    return new LegacyMovementPage(all.subList(from, to), all.size());
  }

  private static Response fault(String fault) {
    if ("unavailable".equals(fault)) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE)
          .entity(new LegacyError("SYS-503"))
          .build();
    }
    if ("slow".equals(fault)) {
      try {
        Thread.sleep(3_000);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    return null;
  }

  public record LegacyMovementPage(List<SyntheticLedger.LegacyMovement> registros, int total) {}

  public record LegacyError(String codError) {}
}
