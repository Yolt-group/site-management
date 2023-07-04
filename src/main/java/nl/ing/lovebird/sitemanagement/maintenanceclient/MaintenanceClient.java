package nl.ing.lovebird.sitemanagement.maintenanceclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.UUID;


@Service
public class MaintenanceClient {

    private final RestTemplate restTemplate;


    public MaintenanceClient(RestTemplateBuilder restTemplateBuilder,
                             @Value("${service.maintenance.timeout-in-seconds:5}") Integer maintenanceServiceTimeout,
                             @Value("${service.maintenance.url:https://maintenance}") String maintenanceServiceUrl
    ) {
        restTemplate = restTemplateBuilder.rootUri(maintenanceServiceUrl)
                .setConnectTimeout(Duration.ofSeconds(maintenanceServiceTimeout))
                .setReadTimeout(Duration.ofSeconds(maintenanceServiceTimeout))
                .build();
    }

    /**
     * Schedules given user-site ID for deletion from the Yolt system. The delete will be executed after at least 24h.
     *
     * @param userId     user ID to which the user-site belongs
     * @param userSiteId userSite ID to delete
     */
    public void scheduleUserSiteDelete(final UUID userId, final UUID userSiteId) {
        restTemplate.delete("/maintenance/user-site/{userId}/{userSiteId}", userId, userSiteId);
    }
}
