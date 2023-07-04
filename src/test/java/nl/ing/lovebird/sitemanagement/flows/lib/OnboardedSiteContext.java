package nl.ing.lovebird.sitemanagement.flows.lib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.NonNull;
import lombok.SneakyThrows;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlDTO;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSiteDTO;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSitesProvider;
import nl.ing.lovebird.sitemanagement.exception.SiteNotFoundException;
import nl.ing.lovebird.sitemanagement.lib.types.SiteId;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.users.StatusType;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserMessage;
import org.awaitility.Durations;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.time.Clock.systemUTC;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static nl.ing.lovebird.sitemanagement.users.UserMessage.Payload.UserMessageType.USER_CREATED;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpStatus.OK;

/**
 * This class contains functionality to 'mock a real client', e.g. it sets up the prerequisites to perform
 * api calls as a registered client so we don't have to think about it in our tests.
 */
public class OnboardedSiteContext {

    private boolean initialized = false;

    private ConfiguredClient configuredClient;
    static private final ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().build();
    private ConfiguredClientSite configuredClientSite;
    private ConfiguredUser configuredUser;
    private SitesProvider sitesProvider;
    private ClientSitesProvider clientSitesProvider;
    private WireMockServer wireMockServer;
    private ClientRedirectUrlService clientRedirectUrlService;


    /**
     * Configured the client if this has not already been done.
     */
    @SneakyThrows
    public synchronized void initialize(SitesProvider sitesProvider,
                                        KafkaTemplate<?,?> kafkaTemplate,
                                        String provider,
                                        ClientSitesProvider clientSitesProvider,
                                        WireMockServer wireMockServer,
                                        ClientRedirectUrlService clientRedirectUrlService,
                                        TestClientTokens testClientTokens
    ) {
        if (initialized) {
            return;
        }
        this.sitesProvider = sitesProvider;
        this.clientSitesProvider = clientSitesProvider;
        this.wireMockServer = wireMockServer;
        this.clientRedirectUrlService = clientRedirectUrlService;
        this.configuredClient = new ConfiguredClient(testClientTokens);
        configuredUser = createUser(kafkaTemplate);

        // Select ABN Amro as a default.
        var site = findByNonScrapingProviderOrThrow(provider);
        configureSite(kafkaTemplate, site);

        // All done.
        initialized = true;
    }

    /**
     * Return a {@link Consumer} that can be used to augment the headers on a {@link RequestEntity} with a
     * {@link ClientToken}
     * <p>
     * Use like this:
     * <pre>
     * RequestEntity.post(...)
     *   .headers(testClient.httpHeadersForClientAndUser())
     *   ...
     * </pre>
     */
    public Consumer<HttpHeaders> httpHeadersForClientAndUser() {
        if (!initialized) {
            throw new IllegalStateException();
        }
        return headers -> configuredClient.clientUserTokenHttpHeaders(configuredUser.getId())
                .accept(headers);
    }

    public ConfiguredClient client() {
        if (!initialized) {
            throw new IllegalStateException();
        }
        return configuredClient;
    }

    public ConfiguredClientSite clientSite() {
        if (!initialized) {
            throw new IllegalStateException();
        }
        return configuredClientSite;
    }

    public ConfiguredUser user() {
        if (!initialized) {
            throw new IllegalStateException();
        }
        return configuredUser;
    }

    /**
     * There is a 1:1 mapping between api connections for sites and providers.  This is tested by ProductionSitesTest#assertOneToOneCorrespondenceBetweenSiteAndNonScrapingProvider
     * <p>
     * This mapping does not exist for scraping providers, in which case we throw an {@link IllegalArgumentException}.
     * <p>
     * Note: O(n)
     */
    private nl.ing.lovebird.sitemanagement.sites.Site findByNonScrapingProviderOrThrow(@NonNull String provider) {
        if (isScrapingSite(provider)) {
            throw new IllegalArgumentException("can't use this method for scraping providers");
        }
        return sitesProvider.allSites().stream()
                .filter(s -> provider.equals(s.getProvider()))
                .findFirst()
                .orElseThrow(() -> new SiteNotFoundException("No site with provider " + provider + "."));
    }

    /**
     * Configures a {@link Site} for the client.
     * <p>
     * After this method completes without throwing the site is ready for use.
     */
    private void configureSite(KafkaTemplate<?,?> kafkaTemplate, @NonNull Site site) throws JsonProcessingException {
        var redirectUrlIdAIS = UUID.randomUUID();
        var redirectUrlIdPIS = UUID.randomUUID();

        ClientSiteDTO clientSiteDTO = new ClientSiteDTO(
                site.getId(),
                site.getName(),
                new ClientSiteDTO.Services(
                        new ClientSiteDTO.Services.AIS(new ClientSiteDTO.Services.Onboarded(List.of(redirectUrlIdAIS)))
                ),
                false);

        configuredClientSite = new ConfiguredClientSite(clientSiteDTO, site);

        // Create the redirectUrls
        kafkaTemplate.send(MessageBuilder
                .withPayload(new ClientRedirectUrlDTO(
                        configuredClient.getClientId(),
                        redirectUrlIdAIS,
                        configuredClientSite.getRedirectBaseUrlAIS()
                ))
                .setHeader(KafkaHeaders.TOPIC, "clientRedirectUrls")
                .setHeader(KafkaHeaders.MESSAGE_KEY, configuredClient.getClientId().toString())
                .setHeader(CLIENT_TOKEN_HEADER_NAME, configuredClient.getClientToken().getSerialized())
                .setHeader("message_type", "CLIENT_REDIRECT_URL_CREATED")
                .build()
        ).completable().join();

        kafkaTemplate.send(MessageBuilder
                .withPayload(new ClientRedirectUrlDTO(
                        configuredClient.getClientId(),
                        redirectUrlIdPIS,
                        configuredClientSite.getRedirectBaseUrlPIS()
                ))
                .setHeader(KafkaHeaders.TOPIC, "clientRedirectUrls")
                .setHeader(KafkaHeaders.MESSAGE_KEY, configuredClient.getClientId().toString())
                .setHeader(CLIENT_TOKEN_HEADER_NAME, configuredClient.getClientToken().getSerialized())
                .setHeader("message_type", "CLIENT_REDIRECT_URL_CREATED")
                .build()
        ).completable().join();

        // Validate that the redirect urls have been stored
        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> Assertions.assertDoesNotThrow(() -> clientRedirectUrlService.getRedirectUrlOrThrow(configuredClient.getClientId(), redirectUrlIdAIS)));
        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> Assertions.assertDoesNotThrow(() -> clientRedirectUrlService.getRedirectUrlOrThrow(configuredClient.getClientId(), redirectUrlIdPIS)));

        Map<UUID, List<ClientSiteDTO>> sitesPerClient = new HashMap<>();
        sitesPerClient.put(configuredClient.getClientId().unwrap(), List.of(clientSiteDTO));

        wireMockServer.stubFor(WireMock.get(urlMatching("/clients/internal/v2/sites-per-client"))
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(sitesPerClient))
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));
        // Signal the client sites provider to update its internal state
        kafkaTemplate.send(MessageBuilder
                .withPayload("please update client sites list")
                .setHeader(KafkaHeaders.TOPIC, "ycs_clientSitesUpdates")
                .setHeader(KafkaHeaders.MESSAGE_KEY, configuredClient.getClientId().toString())
                .build()
        ).completable().join();

        // Validate that the site has been enabled.
        await().atMost(Durations.TEN_SECONDS).until(() -> clientSitesProvider.getClientSite(configuredClient.getClientId(), new SiteId(site.getId())).isPresent());

    }

    private ConfiguredUser createUser(KafkaTemplate<?,?> kafkaTemplate) {
        var user = new User(
                UUID.randomUUID(),
                Instant.now(systemUTC()),
                configuredClient.getClientId(),
                StatusType.ACTIVE,
                false
        );
        var configuredUser = new ConfiguredUser(user);

        kafkaTemplate.send(MessageBuilder
                .withPayload(new UserMessage(
                        new UserMessage.Headers(USER_CREATED.name()),
                        new UserMessage.Payload(user.getUserId(), user.getClientId(), USER_CREATED, user.isOneOffAis(), user.getLastLogin().atZone(ZoneOffset.UTC))
                ))
                .setHeader(KafkaHeaders.TOPIC, "users")
                .setHeader(KafkaHeaders.MESSAGE_KEY, user.getUserId().toString())
                .setHeader(CLIENT_TOKEN_HEADER_NAME, configuredClient.getClientToken().getSerialized())
                .build()
        ).completable().join();

        return configuredUser;
    }

    public ClientUserToken getClientUserToken() {
        return configuredClient.getClientUserToken(configuredUser.getId());
    }
}
