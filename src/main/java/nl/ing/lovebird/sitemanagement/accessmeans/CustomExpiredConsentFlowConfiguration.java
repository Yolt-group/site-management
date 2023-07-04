package nl.ing.lovebird.sitemanagement.accessmeans;

import lombok.Data;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Some Spanish and Italian banks return consent expired responses when they're having technical issues, even when a
 * consent is not expired. Because `site-management` disconnects user sites on such events, it forces Clients to ask their
 * customers to reconnect their user sites. A lot of their users don't do this, especially since this happens frequently,
 * which hurts Client's business.
 * <p>
 * In order to minimize this problem with the least amount of effort (and some downsides, see story YD-106),
 * we've decided to keep user sites connected for certain banks on consent expired, until a consent reached a configured
 * age.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "yolt.site-management.custom-expired-consent-flow")
class CustomExpiredConsentFlowConfiguration {
    private List<UUID> applicableClientIds = List.of();
    private List<UUID> applicableSiteIds = new ArrayList<>();
    private Duration minimumConsentAgeBeforeDisconnect = Duration.of(90, ChronoUnit.DAYS);

    boolean appliesToClientAndSite(ClientId clientId, UUID siteId) {
        return applicableClientIds.contains(clientId.unwrap()) && applicableSiteIds.contains(siteId);
    }
}
