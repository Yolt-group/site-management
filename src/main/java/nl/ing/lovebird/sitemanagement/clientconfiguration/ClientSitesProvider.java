package nl.ing.lovebird.sitemanagement.clientconfiguration;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.lib.types.SiteId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ClientSitesProvider implements SmartLifecycle {

    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final RestTemplate clientsRestTemplate;

    private Map<UUID, Map<UUID, ClientSiteDTO>> clientSiteDTOPerClient = Map.of();

    volatile boolean loaded = false;
    private Instant lastLoaded = Instant.EPOCH;

    public ClientSitesProvider(Clock clock, MeterRegistry meterRegistry, RestTemplateBuilder builder,
                               @Value("${service.clients.url}") String endpointBaseUrl) {
        this.clock = clock;
        this.meterRegistry = meterRegistry;
        this.clientsRestTemplate = builder
                .rootUri(endpointBaseUrl)
                .setReadTimeout(Duration.ofSeconds(81)) // Match the read-timeout of clients plus some margin
                .build();
    }

    public Map<UUID, ClientSiteDTO> getClientSites(ClientId clientId) {
        return clientSiteDTOPerClient.getOrDefault(clientId.unwrap(), Collections.emptyMap());
    }

    public Optional<ClientSiteDTO> getClientSite(ClientId clientId, SiteId siteId) {
        return Optional.ofNullable(clientSiteDTOPerClient.getOrDefault(clientId.unwrap(), Collections.emptyMap()).get(siteId.unwrap()));
    }

    /**
     * By setting the lifecycle phase of this bean to a negative integer, we ensure that this service will be initialized
     * before all other services.
     */
    @Override
    public int getPhase() {
        return -1;
    }

    @SneakyThrows
    @Override
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

    void update() {
        try {
            var clientSites = clientsRestTemplate.exchange("/internal/v2/sites-per-client", HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<Map<UUID, List<ClientSiteDTO>>>() {
            }).getBody();

            clientSiteDTOPerClient = clientSites.entrySet()
                    .stream().collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> entry.getValue().stream().collect(Collectors.toMap(ClientSiteDTO::getId, it -> it))));
            this.loaded = true;
            this.lastLoaded = Instant.now(clock);
            log.info("loaded client sites for {} clients", clientSiteDTOPerClient.size());
        } catch (RuntimeException e) {
            log.warn("Failed to call clients/internal/v2/sites-per-client", e);
        }
    }


    /**
     * Site details are retrieved every seven minutes.  During normal operation this gauge value should always be at
     * most 7 minutes.
     */
    private void registerMetric() {
        Gauge.builder("client_site_details_age_minutes", () -> {
                    if (lastLoaded == Instant.EPOCH) {
                        return 0;
                    }
                    return Math.abs((int) ChronoUnit.MINUTES.between(Instant.now(clock), lastLoaded));
                })
                .description("age of the client sites list in minutes")
                .register(meterRegistry);
    }

}
