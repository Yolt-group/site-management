package nl.ing.lovebird.sitemanagement.usersite;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
@UtilityClass
public class UserSiteDerivedAttributes {

    public UserSiteNeededAction determineNeededAction(final PostgresUserSite userSite) {
        final var connectionStatus = userSite.getConnectionStatus();

        if (!StringUtils.hasLength(userSite.getExternalId()) && isScrapingSite(userSite.getProvider()) && connectionStatus != ConnectionStatus.CONNECTED && userSite.getFailureReason() != null) {
            log.info("No external id found for scraping userSiteId={}. User should update credentials.", userSite.getUserSiteId());
            return UserSiteNeededAction.UPDATE_CREDENTIALS;
        }

        return switch (connectionStatus) {
            case CONNECTED -> determineNeededActionWhenConnected(userSite);
            case DISCONNECTED -> determineNeededActionWhenDisconnected(userSite);
            case STEP_NEEDED -> UserSiteNeededAction.UPDATE_WITH_NEXT_STEP;
        };
    }

    private UserSiteNeededAction determineNeededActionWhenConnected(PostgresUserSite userSite) {
        if (userSite.getFailureReason() == null) {
            return null;
        }

        return UserSiteNeededAction.TRIGGER_REFRESH;
    }

    private UserSiteNeededAction determineNeededActionWhenDisconnected(PostgresUserSite userSite) {
        if (userSite.getFailureReason() == null) {
            return determineNeededActionForAuth(userSite.getProvider());
        }

        return switch (userSite.getFailureReason()) {
            case TECHNICAL_ERROR, ACTION_NEEDED_AT_SITE -> UserSiteNeededAction.TRIGGER_REFRESH;
            case AUTHENTICATION_FAILED, CONSENT_EXPIRED -> determineNeededActionForAuth(userSite.getProvider());
        };
    }

    private UserSiteNeededAction determineNeededActionForAuth(@NonNull final String provider) {
        // An attempt to return the most suitable action (description) based on the type of site.
        if (isScrapingSite(provider)) {
            return UserSiteNeededAction.UPDATE_CREDENTIALS;
        } else {
            return UserSiteNeededAction.LOGIN_AGAIN;
        }
    }

    public static boolean isScrapingSite(@NonNull String provider) {
        return provider.equals("BUDGET_INSIGHT") || provider.equals("YODLEE") || provider.equals("SALTEDGE");
    }

}
