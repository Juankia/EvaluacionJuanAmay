package ec.digitalbanking.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ec.digitalbanking.linking.domain.ChannelStatus;
import ec.digitalbanking.linking.domain.OnboardingRequest;
import ec.digitalbanking.linking.domain.OnboardingState;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OnboardingStateTest {

  @Test
  void identityVerificationPathIsAllowed() {
    assertTrue(OnboardingState.STARTED.canTransitionTo(OnboardingState.IDENTITY_VERIFICATION_IN_PROGRESS));
    assertTrue(
        OnboardingState.IDENTITY_VERIFICATION_IN_PROGRESS.canTransitionTo(
            OnboardingState.IDENTITY_VERIFIED));
    assertTrue(
        OnboardingState.IDENTITY_VERIFICATION_UNAVAILABLE.canTransitionTo(
            OnboardingState.IDENTITY_VERIFICATION_IN_PROGRESS));
  }

  @Test
  void noStateLeadsBeyondIdentityVerification() {
    // E3 and E5 are unreachable: verified and rejected requests have no outgoing transitions.
    for (OnboardingState target : OnboardingState.values()) {
      assertFalse(OnboardingState.IDENTITY_VERIFIED.canTransitionTo(target), target.name());
      assertFalse(OnboardingState.IDENTITY_REJECTED.canTransitionTo(target), target.name());
    }
  }

  @Test
  void providerUnavailabilityIsNeverARejection() {
    assertEquals(
        ChannelStatus.ACTION_REQUIRED,
        OnboardingState.IDENTITY_VERIFICATION_UNAVAILABLE.channelStatus());
    assertFalse(OnboardingState.IDENTITY_VERIFICATION_UNAVAILABLE.terminal());
  }

  @Test
  void illegalTransitionIsRejectedAndVersionAdvancesOnLegalOnes() {
    var request =
        new OnboardingRequest(UUID.randomUUID(), "d", OnboardingState.STARTED, 1, Instant.EPOCH);
    assertThrows(
        OnboardingRequest.IllegalTransitionException.class,
        () -> request.transitionTo(OnboardingState.IDENTITY_VERIFIED, Instant.EPOCH));
    assertEquals(
        2,
        request
            .transitionTo(OnboardingState.IDENTITY_VERIFICATION_IN_PROGRESS, Instant.EPOCH)
            .version());
  }
}
