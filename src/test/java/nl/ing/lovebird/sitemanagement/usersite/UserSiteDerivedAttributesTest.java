package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UserSiteDerivedAttributesTest {

    @Test
    void test_externalIdNull_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createScrapingUserSite(ConnectionStatus.DISCONNECTED, null, null);

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_CREDENTIALS);
    }

    @Test
    @Disabled("Should never happen with non-scraping providers") // TODO: Kill off this test
    void test_externalIdNull_results_in_TRIGGER_REFRESH_when_extIdIsNotRequired() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, null, null);

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.TRIGGER_REFRESH);
    }

    @Test
    @Disabled("No real use-case") // TODO: Kill off this test
    void test_externalIdNull_results_not_in_UPDATE_CREDENTIALS() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, null, null);

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertNull(userSiteNeededAction);
    }

    @Test
    @Disabled("No representation of INITIAL_PROCESSING") // TODO: Kill off this test
    void test_INITIAL_PROCESSING_reason_null_results_in_no_action() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, null, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertNull(userSiteNeededAction);
    }

    @Test
    void test_STEP_NEEDED_reason_null_results_in_UPDATE_QUESTIONS() {
        final var userSite = createUserSite(ConnectionStatus.STEP_NEEDED, null, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_WITH_NEXT_STEP);
    }

    @Test
    void test_LOGIN_SUCCEEDED_reason_null_results_in_no_action() {
        final var userSite = createUserSite(ConnectionStatus.CONNECTED, null, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertNull(userSiteNeededAction);
    }

    @Test
    @Disabled("No real use-case") // TODO: Kill off this test
    void test_LOGIN_FAILED_reason_null_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createUserSite(ConnectionStatus.CONNECTED, null, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_CREDENTIALS);
    }

    @Test
    void test_REFRESH_FINISHED_reason_null_results_in_no_action() {
        final var userSite = createUserSite(ConnectionStatus.CONNECTED, null, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertNull(userSiteNeededAction);
    }


    @Test
    void test_REFRESH_FAILED_reason_GENERIC_ERROR_results_in_TRIGGER_REFRESH() {
        final var userSite = createUserSite(ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.TRIGGER_REFRESH);
    }

    @Test
    @Disabled("UNKNOWN is no longer used") // TODO: Kill off this test
    void test_UNKNOWN_reason_null_results_in_TRIGGER_REFRESH() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, null, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.TRIGGER_REFRESH);
    }

    @Test
    @Disabled("No representation of INITIAL_PROCESSING") // TODO: Kill off this test
    void test_INITIAL_PROCESSING_with_unexpected_reason_results_in_no_action() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertNull(userSiteNeededAction);
    }

    @Test
    void test_STEP_NEEDED_with_unexpected_reason_results_in_UPDATE_WITH_NEXT_STEP() {
        final var userSite = createUserSite(ConnectionStatus.STEP_NEEDED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_WITH_NEXT_STEP);
    }

    @Test
    @Disabled("No real use-case") // TODO: Kill off this test
    void test_LOGIN_SUCCEEDED_with_unexpected_reason_results_in_no_action() {
        final var userSite = createUserSite(ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertNull(userSiteNeededAction);
    }

    @Test
    void test_LOGIN_FAILED_reason_INCORRECT_CREDENTIALS_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createScrapingUserSite(ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_CREDENTIALS);
    }

    @Test
    void test_LOGIN_FAILED_reason_EXPIRED_CREDENTIALS_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createScrapingUserSite(ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_CREDENTIALS);
    }

    @Test
    void test_LOGIN_FAILED_reason_INCORRECT_ANSWER_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createScrapingUserSite(ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_CREDENTIALS);
    }

    @Test
    @Disabled("No real use-case") // TODO: Kill off this test
    void test_LOGIN_FAILED_reason_CONSENT_EXPIRED_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, FailureReason.CONSENT_EXPIRED, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_CREDENTIALS);
    }

    @Test
    @Disabled("No real use-case") // TODO: Kill off this test
    void test_LOGIN_FAILED_reason_ACCOUNT_LOCKED_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_CREDENTIALS);
    }

    @Test
    void test_LOGIN_FAILED_reason_SITE_ERROR_results_in_TRIGGER_REFRESH() {
        final var userSite = createUserSite(ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.TRIGGER_REFRESH);
    }

    @Test
    void test_LOGIN_FAILED_reason_GENERIC_ERROR_results_in_TRIGGER_REFRESH() {
        final var userSite = createUserSite(ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.TRIGGER_REFRESH);
    }

    @Test
    void test_LOGIN_FAILED_reason_MULTIPLE_LOGINS_results_in_TRIGGER_REFRESH() {
        final var userSite = createUserSite(ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.TRIGGER_REFRESH);
    }

    @Test
    void test_LOGIN_FAILED_reason_SITE_ACTION_NEEDED_results_in_TRIGGER_REFRESH() {
        final var userSite = createUserSite(ConnectionStatus.CONNECTED, FailureReason.ACTION_NEEDED_AT_SITE, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.TRIGGER_REFRESH);
    }

    @Test
    void test_LOGIN_FAILED_reason_UNSUPPORTED_LANGUAGE_results_in_TRIGGER_REFRESH() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.TRIGGER_REFRESH);
    }

    @Test
    void test_LOGIN_FAILED_reason_NEW_LOGIN_INFO_NEEDED_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createScrapingUserSite(ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_CREDENTIALS);
    }

    @Test
    void test_LOGIN_FAILED_reason_MFA_TIMED_OUT_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createScrapingUserSite(ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_CREDENTIALS);
    }


    @Test
    @Disabled("No real use-case") // TODO: Kill off this test
    void test_REFRESH_FINISHED_with_unexpected_reason_results_in_no_action() {
        final var userSite = createUserSite(ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertNull(userSiteNeededAction);
    }

    @Test
    void test_REFRESH_FAILED_with_any_NEW_LOGIN_INFO_NEEDED_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createScrapingUserSite(ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.UPDATE_CREDENTIALS);
    }

    @Test
    void test_UNKNOWN_with_unexpected_reason_results_in_TRIGGER_REFRESH() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.TRIGGER_REFRESH);
    }

    @Test
    void test_LOGIN_FAILED_with_TOKEN_EXPIRED_reason_results_in_LOGIN_AGAIN() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, FailureReason.CONSENT_EXPIRED, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.LOGIN_AGAIN);
    }

    @Test
    void test_LOGIN_FAILED_with_WRONG_CREDENTIALS_reason_results_in_UPDATE_CREDENTIALS() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.LOGIN_AGAIN);
    }

    @Test
    void test_LOGIN_FAILED_with_MFA_NOT_SUPPORTED_reason_results_in_TRIGGER_REFRESH() {
        final var userSite = createUserSite(ConnectionStatus.DISCONNECTED, FailureReason.TECHNICAL_ERROR, "fake");

        final UserSiteNeededAction userSiteNeededAction = UserSiteDerivedAttributes.determineNeededAction(userSite);

        assertThat(userSiteNeededAction).isEqualTo(UserSiteNeededAction.TRIGGER_REFRESH);
    }

    private PostgresUserSite createUserSite(ConnectionStatus connectionStatus, FailureReason failureReason, String externalId) {
        return createUserSite("YOLT_PROVIDER", connectionStatus, failureReason, externalId);
    }

    private PostgresUserSite createScrapingUserSite(ConnectionStatus connectionStatus, FailureReason failureReason, String externalId) {
        return createUserSite("BUDGET_INSIGHT", connectionStatus, failureReason, externalId);
    }

    private PostgresUserSite createUserSite(String provider, ConnectionStatus connectionStatus, FailureReason failureReason, String externalId) {
        return new PostgresUserSite(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), externalId, connectionStatus, failureReason, null, new Date().toInstant(), null, null, ClientId.random(), provider, null, null, null, false, null);
    }
}
