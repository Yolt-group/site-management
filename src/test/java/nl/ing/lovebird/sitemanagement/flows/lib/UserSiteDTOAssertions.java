package nl.ing.lovebird.sitemanagement.flows.lib;

import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.FailureReason;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteDTO;

import java.time.Instant;

import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;

public class UserSiteDTOAssertions {

    /**
     * The "WasJust" in this method name is there because we assert on a timestamp being 'close to' now.
     */
    public static void assertConnectedJustNow(UserSiteDTO dto, Instant now) {
        assertStatusAndReason(dto, CONNECTED, null);

        assertThat(dto.getConsentValidFrom())
                .withFailMessage("The field consentValidFrom should have been updated to 'just now'.")
                .isCloseTo(now, byLessThan(1, MINUTES));
        assertThat(dto.getLastDataFetchTime())
                .withFailMessage("A data fetch cannot have completed already.")
                .isNull();
    }

    public static void assertDataFetchCompletedJustNow(UserSiteDTO dto) {
        assertStatusAndReason(dto, CONNECTED, null);

        assertThat(dto.getLastDataFetchTime())
                .withFailMessage("A data fetch should have just completed, so lastDataFetchTime should be 'just now'.")
                .isCloseTo(Instant.now(systemUTC()), byLessThan(1, MINUTES));
    }

    public static void assertStatusAndReason(UserSiteDTO dto, ConnectionStatus status, FailureReason reason) {
        assertInvariants(dto);

        assertThat(dto.getConnectionStatus()).isEqualTo(status);
        if (reason == null) {
            assertThat(dto.getLastDataFetchFailureReason())
                    .withFailMessage("User-site should have an empty dataFetchFailureReason.")
                    .isNull();
        } else {
            assertThat(dto.getLastDataFetchFailureReason())
                    .isEqualTo(reason);
        }
    }

    /**
     * Sometimes a site can be connected but the datafetch failed (e.g. providers cannot be reached or health cannot be reached)
     *
     * @param dto    the dto we want to validate
     * @param reason the failure reason
     */
    public static void assertDataFetchNotCompletedWithReason(UserSiteDTO dto, FailureReason reason) {
        assertThat(dto)
                .returns(null, UserSiteDTO::getLastDataFetchTime)
                .returns(CONNECTED, UserSiteDTO::getConnectionStatus);

        if (reason == null) {
            assertThat(dto.getLastDataFetchFailureReason())
                    .withFailMessage("User-site should have an empty dataFetchFailureReason.")
                    .isNull();
        } else {
            assertThat(dto.getLastDataFetchFailureReason())
                    .isEqualTo(reason);
        }
        assertSiteInvariants(dto);
    }

    /**
     * These assertions should always be true for any UserSiteDTO returned by our service.
     */
    private static void assertInvariants(UserSiteDTO dto) {
        assertThat(dto.getId())
                .withFailMessage("A UserSite should always have an identifier.")
                .isNotNull();
        assertThat(dto.getConnectionStatus())
                .withFailMessage("A UserSite should always have a connectionStatus.")
                .isNotNull();
        if (dto.getConnectionStatus() == CONNECTED) {
            assertThat(dto.getLastDataFetchFailureReason())
                    .withFailMessage("A UserSite that is connected should have lastDataFetchFailureReason == null.")
                    .isNull();
            assertThat(dto.getConsentValidFrom())
                    .withFailMessage("A UserSite that is connected should have consentValidFrom != null.")
                    .isNotNull();
        } else if (dto.getConnectionStatus() == STEP_NEEDED) {
            // XXX Unsure if this is always true; do we clear this field when a user must renew access?
            assertThat(dto.getLastDataFetchFailureReason())
                    .withFailMessage("A UserSite that is in STEP_NEEDED should have lastDataFetchFailureReason == null.")
                    .isNull();
            // XXX Unsure if this is currently valid, if not: we should probably set this to null if we detect that consent has expired.
            assertThat(dto.getConsentValidFrom())
                    .withFailMessage("A UserSite that is not connected should have consentValidFrom == Instant.EPOCH.")
                    .isEqualTo(Instant.EPOCH);
        } else if (dto.getConnectionStatus() == DISCONNECTED) {
            assertThat(dto.getLastDataFetchFailureReason())
                    .withFailMessage("A UserSite that is not connected should have lastDataFetchFailureReason != null.")
                    .isNotNull();
            // XXX Unsure if this is currently valid, if not: we should probably set this to null if we detect that consent has expired.
            assertThat(dto.getConsentValidFrom())
                    .withFailMessage("A UserSite that is not connected should have consentValidFrom == Instant.EPOCH.")
                    .isEqualTo(Instant.EPOCH);
        } else {
            throw new IllegalStateException();
        }
        assertSiteInvariants(dto);
    }

    private static void assertSiteInvariants(UserSiteDTO dto) {
        assertThat(dto.getSite())
                .withFailMessage("UserSite.site should not be null.")
                .isNotNull();
        assertThat(dto.getSite().getId())
                .withFailMessage("UserSite.site.id should not be null.")
                .isNotNull();
        assertThat(dto.getSite().getName())
                .withFailMessage("UserSite.site.name should not be null.")
                .isNotNull();
        assertThat(dto.getSite().getSupportedAccountTypes())
                .withFailMessage("UserSite.site.supportedAccountTypes should not be null.")
                .isNotNull();
    }

}
