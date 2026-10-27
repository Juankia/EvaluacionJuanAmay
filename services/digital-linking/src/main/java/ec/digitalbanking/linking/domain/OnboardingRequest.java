package ec.digitalbanking.linking.domain;

import java.time.Instant;
import java.util.UUID;

/** Aggregate snapshot. {@code version} is the CAS token and the event sequence of the subject. */
public record OnboardingRequest(
    UUID requestId, String identificationDigest, OnboardingState state, long version, Instant updatedAt) {

  public OnboardingRequest transitionTo(OnboardingState target, Instant now) {
    if (!state.canTransitionTo(target)) {
      throw new IllegalTransitionException(state, target);
    }
    return new OnboardingRequest(requestId, identificationDigest, target, version + 1, now);
  }

  public static final class IllegalTransitionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IllegalTransitionException(OnboardingState from, OnboardingState to) {
      super("transition not allowed: " + from + " -> " + to);
    }
  }
}
