package nl.ing.lovebird.sitemanagement.accessmeans;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@AllArgsConstructor
@Slf4j
public class CustomExpiredConsentFlowService {

    private final Clock clock;
    private final CustomExpiredConsentFlowConfiguration customExpiredConsentFlowConfiguration;
    private final UserSiteAccessMeansRepository userSiteAccessMeansRepository;

    public boolean shouldDisconnectOnConsentExpired(PostgresUserSite userSite) {
        if (!customExpiredConsentFlowConfiguration.appliesToClientAndSite(userSite.getClientId(), userSite.getSiteId())) {
            return true;
        }

        var optionalAccessMeans = userSiteAccessMeansRepository.get(userSite.getUserId(), userSite.getUserSiteId(), userSite.getProvider());
        if (optionalAccessMeans.isEmpty()) {
            log.warn("Could not find access means for user-site '{}' to determine if user-site should be disconnected; will disconnect", userSite.getUserSiteId());
            return true;
        }

        var accessMeans = optionalAccessMeans.get();
        var shouldDisconnect = accessMeans.getCreated().isBefore(Instant.now(clock).minus(customExpiredConsentFlowConfiguration.getMinimumConsentAgeBeforeDisconnect()));

        if (!shouldDisconnect) {
            log.info("Not disconnecting user-site '{}' despite a \"consent-expired\" response from the bank", userSite.getUserSiteId());
        }

        return shouldDisconnect;
    }
}
