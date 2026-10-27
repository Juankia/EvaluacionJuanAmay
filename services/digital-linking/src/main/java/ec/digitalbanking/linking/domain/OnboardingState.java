package ec.digitalbanking.linking.domain;

import java.util.Map;
import java.util.Set;

/**
 * Implemented subset of the onboarding state machine (docs/07 §6, stage E1 only). States after
 * {@link #IDENTITY_VERIFIED} (customer lookup, Core creation, link authorization, activation) are
 * intentionally absent: E3 is blocked (B-05) and activation of existing customers is out of scope.
 */
public enum OnboardingState {
  STARTED,
  IDENTITY_VERIFICATION_IN_PROGRESS,
  IDENTITY_VERIFICATION_UNAVAILABLE,
  IDENTITY_REJECTED,
  IDENTITY_VERIFIED;

  private static final Map<OnboardingState, Set<OnboardingState>> ALLOWED =
      Map.of(
          STARTED, Set.of(IDENTITY_VERIFICATION_IN_PROGRESS),
          IDENTITY_VERIFICATION_IN_PROGRESS,
              Set.of(IDENTITY_VERIFIED, IDENTITY_REJECTED, IDENTITY_VERIFICATION_UNAVAILABLE),
          IDENTITY_VERIFICATION_UNAVAILABLE, Set.of(IDENTITY_VERIFICATION_IN_PROGRESS),
          IDENTITY_REJECTED, Set.of(),
          // Pending: the next stage (E2) is not implemented in this foundation.
          IDENTITY_VERIFIED, Set.of());

  public boolean canTransitionTo(OnboardingState target) {
    return ALLOWED.get(this).contains(target);
  }

  public boolean terminal() {
    return this == IDENTITY_REJECTED;
  }

  /** Channel projection without revealing internal conditions (VD-07, docs/25 §2.3). */
  public ChannelStatus channelStatus() {
    return switch (this) {
      case STARTED, IDENTITY_VERIFICATION_IN_PROGRESS, IDENTITY_VERIFIED ->
          ChannelStatus.IN_PROGRESS;
      case IDENTITY_VERIFICATION_UNAVAILABLE -> ChannelStatus.ACTION_REQUIRED;
      case IDENTITY_REJECTED -> ChannelStatus.CANNOT_CONTINUE_DIGITALLY;
    };
  }
}
