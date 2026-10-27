package ec.digitalbanking.notifications.application;

import ec.digitalbanking.notifications.domain.Channel;
import java.util.Optional;

/**
 * Resolves the contact point at send time (docs/26 scenario 7). The target design reads it from the
 * inquiries service internal API (A4); this foundation only has a synthetic resolver.
 */
public interface RecipientResolver {

  Optional<String> resolve(String subjectId, Channel channel);
}
