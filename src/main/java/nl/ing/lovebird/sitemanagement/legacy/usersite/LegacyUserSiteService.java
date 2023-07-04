package nl.ing.lovebird.sitemanagement.legacy.usersite;

import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.sitemanagement.externalconsent.ExternalConsentService;

import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
@Deprecated
@RequiredArgsConstructor
public class LegacyUserSiteService {

    private final Clock clock;
    private final ExternalConsentService externalConsentService;
    private final LegacyUserSiteStatusMapper statusMapper;

    @Deprecated
    public LegacyUserSiteDTO createUserSiteDTO(final PostgresUserSite userSite) {

        final Optional<Instant> consentExpiryBy = externalConsentService.findConsentExpiryBy(userSite.getUserId(), userSite.getSiteId(), userSite.getUserSiteId());

        return LegacyUserSiteDTO.builder()
                .id(userSite.getUserSiteId())
                .siteId(userSite.getSiteId())
                .status(statusMapper.determineReasonableStatusCode(userSite))
                .reason(statusMapper.determineReasonableStatusReason(userSite))
                .lastDataFetch(userSite.getLastDataFetch() != null ? Date.from(userSite.getLastDataFetch()) : null)
                .externalConsentExpiresAt(consentExpiryBy.orElse(null))
                .noLongerSupported(false)
                .statusTimeoutSeconds(determineTimeoutInSeconds(userSite, clock))
                .migrationStatus(userSite.getMigrationStatus())
                .action(userSite.determineUserSiteNeededAction())
                .build();

    }

    private static Long determineTimeoutInSeconds(final PostgresUserSite userSite, Clock clock) {
        if (userSite.getStatusTimeoutTime() == null) {
            return null;
        }
        if (Instant.now(clock).isBefore(userSite.getStatusTimeoutTime())) {
            return Duration.between(Instant.now(clock), userSite.getStatusTimeoutTime()).getSeconds();
        } else {
            return 0L;
        }
    }

}
