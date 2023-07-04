package nl.ing.lovebird.sitemanagement.flywheel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.sitemanagement.configuration.ApplicationConfiguration;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationConstants;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteNeededAction;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteRefreshService;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType.FLYWHEEL_REFRESH;

@Slf4j
@Component
@RequiredArgsConstructor
class InternalFlywheelRefreshService {
    private final Clock clock;
    private final UserSiteService userSiteService;
    private final InternalFlywheelProperties properties;
    private final ClientTokenRequesterService clientTokenRequesterService;
    private final UserSiteRefreshService userSiteRefreshService;

    @Async(ApplicationConfiguration.INTERNAL_FLYWHEEL_PER_USER_EXECUTOR)
    public void refreshForUser(UUID userId, boolean oneOffAisUser, boolean forceRefresh) {

        List<PostgresUserSite> userSites = userSiteService.getNonDeletedUserSites(userId).stream()
                .filter(userSite -> shouldRefresh(userSite, oneOffAisUser, forceRefresh))
                .toList();

        if (userSites.isEmpty()) {
            return;
        }
        ClientId clientId = userSites.get(0).getClientId();
        final ClientUserToken clientUserToken = clientTokenRequesterService.getClientUserToken(clientId.unwrap(), userId);

        userSiteRefreshService.refreshUserSitesBlocking(
                userSites,
                oneOffAisUser,
                clientUserToken,
                FLYWHEEL_REFRESH,
                null,
                null
        );
    }

    private boolean shouldRefresh(final PostgresUserSite userSite, final boolean oneOffAisUser, final boolean forceRefresh) {
        final UUID userSiteId = userSite.getUserSiteId();
        final String providerName = userSite.getProvider();
        final UserSiteNeededAction neededAction = userSite.determineUserSiteNeededAction();

        if (properties.getBlacklistedProviders().contains(providerName)) {
            log.info("Skipping user-site with id {} because it has provider {} which is blacklisted", userSiteId, providerName);
            return false;
        } else if (oneOffAisUser && userSite.getLastDataFetch() != null) {
            // We're only allowed to fetch a one-off AIS user's data once from a compliance perspective. Hence we also
            // stop forced refreshes.
            log.info("Skipping user-site with id {} because its data has already been fetched and its user is a one-off AIS user", userSiteId);
            return false;
        }

        if (forceRefresh) {
            return true;
        }

        if (neededAction != null && neededAction != UserSiteNeededAction.TRIGGER_REFRESH) {
            log.info("Skipping user-site with id {} because it is in an unrecoverable state. User action needed first.", userSiteId);
            return false;
        }
        final int minimumSecondsSinceLastRefresh = properties.getMinimumSecondsSinceLastRefresh();
        final Instant lastDataFetch = userSite.getLastDataFetch();
        if (minimumSecondsSinceLastRefresh >= 1
                && lastDataFetch != null
                && lastDataFetch.isAfter(Instant.now(clock).minus(minimumSecondsSinceLastRefresh, ChronoUnit.SECONDS))) {
            log.info("Skipping user-site with id {} because the user had data refreshed recently.", userSiteId);
            return false;
        }
        if (MigrationConstants.IN_MIGRATION_STATUSES.contains(userSite.getMigrationStatus())) {
            log.info("Skipping user-site with id {} because it is in migration.", userSiteId);
            return false;
        }

        return true;
    }

}
