package ec.digitalbanking.notifications.adapters.out.channels;

import ec.digitalbanking.notifications.application.RecipientResolver;
import ec.digitalbanking.notifications.domain.Channel;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * SYNTHETIC resolver: derives a placeholder address from the opaque subject id. No real contact
 * data exists in this foundation. Replaced by an adapter over the inquiries internal API (A4).
 */
@ApplicationScoped
public class SyntheticRecipientResolver implements RecipientResolver {

  @Override
  public Optional<String> resolve(String subjectId, Channel channel) {
    return Optional.of("synthetic-" + channel.name().toLowerCase() + "-" + subjectId);
  }
}
