package nl.ing.lovebird.sitemanagement.legacy.usersite;

import nl.ing.lovebird.sitemanagement.usersite.FailureReason;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import org.springframework.stereotype.Component;

@Deprecated
@Component
public class LegacyUserSiteStatusMapper {
    public LegacyUserSiteStatusCode determineReasonableStatusCode(PostgresUserSite userSite) {
        return switch (userSite.getConnectionStatus()) {
            case CONNECTED -> {
                if (userSite.getLastDataFetch() == null) {
                    yield LegacyUserSiteStatusCode.LOGIN_SUCCEEDED;
                } else {
                    yield LegacyUserSiteStatusCode.REFRESH_FINISHED;
                }
            }
            case DISCONNECTED -> {
                if (userSite.getLastDataFetch() == null && userSite.getFailureReason() == null) {
                    yield LegacyUserSiteStatusCode.INITIAL_PROCESSING;
                } else if (userSite.getFailureReason() == null || userSite.getFailureReason() == FailureReason.TECHNICAL_ERROR) {
                    yield LegacyUserSiteStatusCode.REFRESH_FAILED;
                } else {
                    yield LegacyUserSiteStatusCode.LOGIN_FAILED;
                }
            }
            case STEP_NEEDED -> LegacyUserSiteStatusCode.STEP_NEEDED;
        };
    }

    public LegacyUserSiteStatusReason determineReasonableStatusReason(PostgresUserSite userSite) {
        if (userSite.getFailureReason() == null) {
            return null;
        }

        return switch (userSite.getFailureReason()) {
            case TECHNICAL_ERROR -> LegacyUserSiteStatusReason.GENERIC_ERROR;
            case ACTION_NEEDED_AT_SITE -> LegacyUserSiteStatusReason.SITE_ACTION_NEEDED;
            case AUTHENTICATION_FAILED -> LegacyUserSiteStatusReason.WRONG_CREDENTIALS;
            case CONSENT_EXPIRED -> LegacyUserSiteStatusReason.TOKEN_EXPIRED;
        };
    }
}
