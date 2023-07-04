package nl.ing.lovebird.sitemanagement.flows.lib;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import lombok.SneakyThrows;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.providershared.ProviderServiceResponseDTO;
import nl.ing.lovebird.providershared.ProviderServiceResponseStatus;
import nl.ing.lovebird.providershared.form.Form;
import nl.ing.lovebird.providershared.form.FormComponent;
import nl.ing.lovebird.providershared.form.SelectField;
import nl.ing.lovebird.providershared.form.SelectOptionValue;
import nl.ing.lovebird.sitemanagement.providerclient.ApiCreateAccessMeansDTO;
import nl.ing.lovebird.sitemanagement.sites.ProvidersSites;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.time.Clock.systemUTC;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Map.of;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

/**
 * Fake behaviour of the providers service.
 */
public class FauxProvidersService {

    static private final ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().build();

    /**
     * Add a stub to a WireMock service that will respond to /providers/v2/{provider}/login-info
     * <p>
     * urlConstructor requires some explanation, when site-management asks providers to get login info, site-management will send 2 pieces of data to providers:
     * - baseClientRedirectUrl: a (partial) url that points to a website of a client, if the client is Yolt this url is "https://yolt.com/callback"
     * - state: a UUID that must be included in the URL that providers constructs, this piece of data is used for correlation
     * <p>
     * It is the job of providers to construct an URL of the form https://yolt.com/callback?redirect_uri={1st argument of urlConstructor}&state={2nd argument of urlConstructor},
     * of course the query parameters redirect_uri and state can differ between banks, this is the reason that we go to providers to construct this URL for us.
     *
     * @param wireMockServer the server to add the stub to
     * @param provider       the provider for which to prepare the stub
     * @param urlConstructor a function taking 2 arguments that returns a redirect URL, the arguments are the baseClientRedirectUrl and the state parameter.
     */
    @SneakyThrows
    public static void setupPostLoginInfoStubForRedirectStep(
            WireMockServer wireMockServer,
            String provider,
            BiFunction<String, String, String> urlConstructor
    ) {
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/v2/" + provider + "/login-info\\?(.*)"))
                .withMetadata(WiremockStubManager.flowStubMetaData)
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("content-type", "application/json")
                        .withBody(objectMapper.writeValueAsString(of(
                                "type", "REDIRECT_URL",
                                // Read about the {{ }} syntax here http://wiremock.org/docs/response-templating/
                                "redirectUrl", urlConstructor.apply("{{jsonPath request.body '$.baseClientRedirectUrl'}}", "{{jsonPath request.body '$.state'}}")
                        )))
                ));
    }

    @SneakyThrows
    public static void setupRefreshAccessMeansReconsentExpiredFailure(
            WireMockServer wireMockServer,
            String provider
    ) {
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/" + provider + "/access-means/refresh\\?(.*)"))
                .withMetadata(WiremockStubManager.flowStubMetaData)
                .willReturn(aResponse()
                        .withStatus(BAD_REQUEST.value())
                        .withHeader("content-type", "application/json")
                        .withBody(objectMapper.writeValueAsString(of(
                                "code", "PR034",
                                "message", "The provided access means are not valid."
                        )))
                ));
    }

    @SneakyThrows
    public static void setupRefreshAccessMeansReconsentExpiredSuccess(
            WireMockServer wireMockServer,
            String provider,
            Supplier<Instant> expireTimeSupplier
    ) {
        final String bogusAccessMeans = "secret token the bank gave us";
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/" + provider + "/access-means/refresh\\?(.*)"))
                .withMetadata(WiremockStubManager.flowStubMetaData)
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("content-type", "application/json")
                        .withBody(objectMapper.writeValueAsString(of(
                                // echo the userId
                                "userId", "{{jsonPath request.body '$.accessMeansDTO.userId'}}",
                                "accessMeansBlob", bogusAccessMeans,
                                "updated", Instant.now(systemUTC()).toString(),
                                "expireTime", expireTimeSupplier.get().toString()
                                )
                        ))
                ));
    }

    @SneakyThrows
    public static void setupPostLoginStubForFormStep(
            WireMockServer wireMockServer,
            String provider,
            Form form
    ) {
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/v2/" + provider + "/login-info\\?(.*)"))
                .withMetadata(WiremockStubManager.flowStubMetaData)
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("content-type", "application/json")
                        .withBody(objectMapper.writeValueAsString(of(
                                "type", "FORM",
                                "form", form,
                                "encryptionDetails", emptyMap(),
                                "timeoutTime", Instant.now(systemUTC()).plus(1, HOURS).toString()
                        )))
                ));
    }

    /**
     * Add a stub to a WireMock service that will respond to /providers/v2/{provider}/access-means/create
     * <p>
     * Note: this stub will return AccessMeans that are valid for 90 days.  The accessMeansBlob contains
     * bogus data since we never talk to real banks anyway.
     *
     * @param wireMockServer     the server to add the stub to
     * @param provider           the provider for which to prepare the stub
     * @param requestBodyMatcher optional request body matcher
     */
    @SneakyThrows
    public static void setupPostAccessMeansCreateStubForAccessMeans(WireMockServer wireMockServer, String provider, Function<ApiCreateAccessMeansDTO, Boolean> requestBodyMatcher, Supplier<Instant> expireTimeSupplier) {
        final String bogusAccessMeans = "secret token the bank gave us";
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/v2/" + provider + "/access-means/create\\?(.*)"))
                .withMetadata(WiremockStubManager.flowStubMetaData)
                .andMatching(value -> value.getMethod().isOneOf(RequestMethod.POST) && requestBodyMatcher.apply(objectMapper_readValue(value.getBodyAsString(), ApiCreateAccessMeansDTO.class))
                        ? MatchResult.exactMatch()
                        : MatchResult.noMatch()
                )
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("content-type", "application/json")
                        .withBody(objectMapper.writeValueAsString(of(
                                "accessMeans", of(
                                        // echo the userId
                                        "userId", "{{jsonPath request.body '$.userId'}}",
                                        "accessMeansBlob", bogusAccessMeans,
                                        "updated", Instant.now(systemUTC()).toString(),
                                        "expireTime", expireTimeSupplier.get().toString()
                                )
                        )))
                ));
    }

    /**
     * Add a stub to a WireMock service that will respond to /providers/v2/{provider}/access-means/create
     * <p>
     * Note: this stub will return a RedirectStep
     *
     * @param wireMockServer the server to add the stub to
     * @param provider       the provider for which to prepare the stub
     * @param urlConstructor a function taking 2 arguments that returns a redirect URL, the arguments are the baseClientRedirectUrl and the state parameter.
     */
    @SneakyThrows
    public static void setupPostAccessMeansCreateStubForRedirectStep(
            WireMockServer wireMockServer,
            String provider,
            BiFunction<String, String, String> urlConstructor,
            Function<ApiCreateAccessMeansDTO, Boolean> requestBodyMatcher
    ) {
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/v2/" + provider + "/access-means/create\\?(.*)"))
                .withMetadata(WiremockStubManager.flowStubMetaData)
                .andMatching(value -> requestBodyMatcher.apply(objectMapper_readValue(value.getBodyAsString(), ApiCreateAccessMeansDTO.class))
                        ? MatchResult.exactMatch()
                        : MatchResult.noMatch()
                )
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("content-type", "application/json")
                        .withBody(objectMapper.writeValueAsString(of(
                                "step", of(
                                        "type", "REDIRECT_URL",
                                        "redirectUrl", urlConstructor.apply("{{jsonPath request.body '$.baseClientRedirectUrl'}}", "{{jsonPath request.body '$.state'}}")
                                )
                        )))
                ));
    }

    /**
     * Add a stub to a WireMock service that will respond to /providers/{provider}/fetch-data
     * <p>
     * Since providers processes these requests asynchronously it just returns 200 OK.
     * <p>
     * See also {@link #extractProviderRequestIdFromServedRequests}
     *
     * @param wireMockServer the server to add the stub to
     * @param provider       the provider for which to prepare the stub
     */
    public static void setupPostFetchDataStub(WireMockServer wireMockServer, String provider) {
        wireMockServer.stubFor(WireMock.post(urlMatching("/providers/" + provider + "/fetch-data\\?(.*)"))
                .withMetadata(WiremockStubManager.flowStubMetaData)
                .willReturn(aResponse()
                        .withStatus(OK.value())
                ));
    }

    /**
     * Add a stub to a Wiremock service that will response to /providers/sites-details
     *
     * @param wiremockServer  the server to add the stub to
     * @param registeredSites the response that should be returned in response to calling stub
     * @deprecated load the "real" list using {@link #setupProviderSitesStub(WireMockServer)}
     */
    @Deprecated
    @SneakyThrows
    public static void setupProviderSitesStub(WireMockServer wiremockServer, ProvidersSites registeredSites) {
        wiremockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .withMetadata(WiremockStubManager.providerSitesStubMetaData)
                .willReturn(aResponse()
                        .withBody(objectMapper.writeValueAsString(registeredSites))
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));
    }

    /**
     * Make /providers/sites-details return a snapshot of the real sites list.  Update the snapshot as follows:
     * $ kubectl exec -it $PROVIDERS_POD_NAME sh
     * $ curl --insecure https://localhost:8443/providers/sites-details
     *
     * Copy the resulting json blob into src/test/resources/providers/sites-details.json
     */
    @SneakyThrows
    public static void setupProviderSitesStub(WireMockServer wiremockServer) {
        var sitesList = new String(FauxProvidersService.class.getResourceAsStream("/providers/sites-details.json").readAllBytes());
        wiremockServer.stubFor(WireMock.get(urlMatching("/providers/sites-details"))
                .withMetadata(WiremockStubManager.providerSitesStubMetaData)
                .willReturn(aResponse()
                        .withBody(sitesList)
                        .withHeader("content-type", "application/json")
                        .withStatus(OK.value())
                ));
    }

    /**
     * Find a providerRequestId sent to the providers service by site-management.  When site-management asks providers
     * to fetch data site-management sends along a providerRequestId to later correlate an incoming message over Kafka.
     * <p>
     * This function can extract the providerRequestId from the WiremockServer by looking through the requests served.
     * <p>
     * See also {@link #setupPostFetchDataStub}
     *
     * @param wireMockServer the server containing ServeEvents that will be searched
     * @param provider       the provider for which to find a providerRequestId
     */
    public static UUID extractProviderRequestIdFromServedRequests(WireMockServer wireMockServer, String provider) {
        AtomicReference<UUID> providerRequestIdRef = new AtomicReference<>();
        await().atMost(TEN_SECONDS).until(() -> {
            UUID id = wireMockServer.getServeEvents().getRequests().stream()
                    .filter(s -> s.getRequest().getUrl().matches("/providers/" + provider + "/fetch-data\\?(.*)"))
                    .map(s -> {
                        try {
                            return new ObjectMapper().readValue(s.getRequest().getBodyAsString(), Map.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(map -> UUID.fromString((String) map.get("providerRequestId")))
                    .findFirst()
                    .orElse(null);
            providerRequestIdRef.set(id);
            return id != null;
        });
        return providerRequestIdRef.get();
    }

    /**
     * When a data fetch completes, providers notifies site-management over Kafka.  This method sends this notification.
     *
     * @param kafkaTemplate     the kafkatemplate to use to send the message
     * @param providerRequestId the providerRequestId sent by site-management, can be found using {@link #extractProviderRequestIdFromServedRequests}
     * @param clientUserToken   the clientUserToken
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void sendKafkaFetchDataSuccessMessage(
            KafkaTemplate kafkaTemplate,
            UUID providerRequestId,
            ClientUserToken clientUserToken
    ) {
        kafkaTemplate.send(MessageBuilder
                .withPayload(new ProviderServiceResponseDTO(emptyList(), emptyList(), emptyList(), ProviderServiceResponseStatus.FINISHED, providerRequestId))
                .setHeader(KafkaHeaders.TOPIC, "providerAccounts")
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientUserToken.getUserIdClaim().toString())
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .setHeader("payload-type", "PROVIDER_SERVICE_RESPONSE")
                .build()
        ).completable().join();
    }

    /**
     * @return the form that Credit Agricole uses to ask a user to pick a region.
     */
    public static Form creditAgricoleRegionSelectionForm() {
        SelectField regionSelectionField = new SelectField("region", "Région", 0, 0, false, true);
        regionSelectionField.addSelectOptionValue(new SelectOptionValue("CAM_ALPES_PROVENCE", "Alpes Provence"));
        regionSelectionField.addSelectOptionValue(new SelectOptionValue("CAM_ALSACE_VOSGES", "Alsace-Vosges"));
        // XXX Omitted 40+ other regions.
        List<FormComponent> fields = new ArrayList<>();
        fields.add(regionSelectionField);
        return new Form(fields, null, emptyMap());
    }

    /**
     * Hack to work around:
     * unreported exception com.fasterxml.jackson.core.JsonProcessingException; must be caught or declared to be thrown
     */
    @SneakyThrows
    private static <T> T objectMapper_readValue(String value, @SuppressWarnings("SameParameterValue") Class<T> clazz) {
        return objectMapper.readValue(value, clazz);
    }

}
