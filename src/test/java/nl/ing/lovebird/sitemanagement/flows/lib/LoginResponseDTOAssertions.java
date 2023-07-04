package nl.ing.lovebird.sitemanagement.flows.lib;

import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.LoginResponseDTO;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginResponseDTOAssertions {

    /**
     * The "JustNow" in this method name is there because {@link UserSiteDTOAssertions#assertConnectedJustNow} asserts on a timestamp being 'close to' now.
     */
    public static void assertConnectedJustNow(LoginResponseDTO dto, Instant now) {
        assertInvariants(dto);

        assertThat(dto.getActivityId())
                .withFailMessage("An activity should have been started if the UserSite is expected to be connected.")
                .isNotNull();
        assertThat(dto.getStep())
                .withFailMessage("Step should be empty if the UserSite is expected to be connected.")
                .isNull();

        UserSiteDTOAssertions.assertConnectedJustNow(dto.getUserSite(), now);
    }

    /**
     * Check that the response contains a RedirectStep.
     */
    public static void assertRedirectStepResponse(LoginResponseDTO dto) {
        assertInvariants(dto);

        assertThat(dto.getActivityId())
                .withFailMessage("A LoginResponseDTO that contains a step must not have activityId non-null.")
                .isNull();
        assertThat(dto.getStep())
                .withFailMessage("Expecting a Step, got null instead.")
                .isNotNull();
        assertThat(dto.getStep().getForm())
                .withFailMessage("Expecting a RedirectStep, got a FormStep instead.")
                .isNull();
        assertThat(dto.getStep().getRedirect())
                .withFailMessage("Expecting a RedirectStep, got null instead.")
                .isNotNull();
        assertThat(dto.getStep().getRedirect().getUrl())
                .withFailMessage("Expecting the RedirectStep to contain a URL, got null.")
                .isNotNull();

        UserSiteDTOAssertions.assertStatusAndReason(dto.getUserSite(), ConnectionStatus.STEP_NEEDED, null);
    }

    /**
     * These assertions should always be true for any LoginResponseDTO returned by our service.
     */
    public static void assertInvariants(LoginResponseDTO dto) {
        assertThat(dto.getUserSite())
                .withFailMessage("User-site object should be present.")
                .isNotNull();
        // XXX We're sending the same data (userSiteId) twice in LoginResponseDTO, kind of strange.
        assertThat(dto.getUserSiteId())
                .withFailMessage("LoginResponseDTO.userSiteId != LoginResponseDTO.userSite.id, this should never happen.")
                .isEqualTo(dto.getUserSite().getId());
    }
}
