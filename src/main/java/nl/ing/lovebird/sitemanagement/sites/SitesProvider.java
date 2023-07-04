package nl.ing.lovebird.sitemanagement.sites;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.exception.SiteNotFoundException;
import nl.ing.lovebird.sitemanagement.providerclient.ProviderRestClient;
import nl.ing.lovebird.sitemanagement.providerresponse.ProviderServiceResponseConsumer;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SitesProvider implements SmartLifecycle {

    private final SitesMapper sitesMapper = new SitesMapper();
    private final ProviderRestClient providerRestClient;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    private List<Site> sites = List.of();
    private Map<UUID, Site> sitesById = Map.of();

    volatile boolean loaded = false;
    private Instant lastLoaded = Instant.EPOCH;

    /**
     * By setting the lifecycle phase of this bean to a negative integer, we ensure that this service will be initialized
     * before all other services.
     * Especially the {@link ProviderServiceResponseConsumer} can cause issues if this service is not initialized before
     * we attempt to consume messages from kafka (this is part of the readiness check of the site-management service).
     */
    @Override
    public int getPhase() {
        return -1;
    }

    @Override
    @SneakyThrows
    public void start() {
        registerMetric();
        while (!loaded) {
            update();
            Thread.sleep(1_000);
        }
    }

    @Override
    public boolean isRunning() {
        return loaded;
    }

    @Override
    public void stop() {
        loaded = false;
    }

    /**
     * Retrieve details about sites every seven minutes
     */
    // second, minute, hour, day of month, month, day(s) of week
    @Scheduled(cron = "0 0/7 * * * *")
    public void retrieveSitesPeriodically() {
        update();
    }

    /**
     * TODO make this non-public
     */
    public void update() {
        try {
            var providersSites = providerRestClient.getProvidersSites();

            Map<UUID, Site> siteAisDetails = providersSites
                    .getRegisteredSites()
                    .stream()
                    .collect(Collectors.toMap(RegisteredSite::getId, sitesMapper::mapToSite));

            this.sitesById = siteAisDetails;
            this.sites = List.copyOf(siteAisDetails.values());
            this.loaded = true;
            this.lastLoaded = Instant.now(clock);
        } catch (HttpException e) {
            log.warn("Failed to call providers/sites-details failed with http_status={} and error_code={}", e.getHttpStatusCode(), e.getFunctionalErrorCode());
        } catch (RuntimeException e) {
            log.warn("Failed to call providers/sites-details", e);
        }
    }

    public List<Site> allSites() {
        return sites;
    }

    public Site findByIdOrThrow(@NonNull UUID id) {
        return Optional.ofNullable(sitesById.get(id)).orElseThrow(() -> new SiteNotFoundException("Site with id " + id + " does not exist."));
    }

    /**
     * Site details are retrieved every seven minutes.  During normal operation this gauge value should always be at
     * most 7 minutes.
     */
    private void registerMetric() {
        Gauge.builder("site_details_age_minutes", () -> {
            if (lastLoaded == Instant.EPOCH) {
                return 0;
            }
            return Math.abs((int) ChronoUnit.MINUTES.between(Instant.now(clock), lastLoaded));
        })
                .description("age of the sites list in minutes")
                .register(meterRegistry);
    }

}
