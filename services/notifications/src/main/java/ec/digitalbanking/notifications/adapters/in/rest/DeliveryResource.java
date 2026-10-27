package ec.digitalbanking.notifications.adapters.in.rest;

import ec.digitalbanking.notifications.application.NotificationStore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Operational read-only view of delivery records, for local verification. In a deployed
 * environment it is reachable only on the internal network (not routed by the gateway).
 */
@Path("/ops/v1/deliveries")
@Produces(MediaType.APPLICATION_JSON)
public class DeliveryResource {

  public record DeliveryResponse(String eventId, String channel, String status, int attempts) {}

  public record DeliveryListResponse(List<DeliveryResponse> items) {}

  private final NotificationStore store;

  public DeliveryResource(NotificationStore store) {
    this.store = store;
  }

  @GET
  public DeliveryListResponse list(@QueryParam("eventId") @NotBlank @Size(max = 64) String eventId) {
    return new DeliveryListResponse(
        store.deliveries(eventId).stream()
            .map(d -> new DeliveryResponse(d.eventId(), d.channel().name(), d.status(), d.attempts()))
            .toList());
  }
}
