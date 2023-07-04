package nl.ing.lovebird.sitemanagement.flows.lib;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import nl.ing.lovebird.activityevents.events.ActivityEventKey;
import nl.ing.lovebird.activityevents.events.IngestionFinishedEvent;
import nl.ing.lovebird.activityevents.events.serializer.ActivityEventSerializer;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.time.Clock.systemUTC;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class FauxAccountsAndTransactionsService {

    /**
     * After providers fetches data it sends the data to the accounts-and-transactions (A&T) service, A&T sends out
     * a notification message whenever it has saved the data to the database.  site-management listens to this message
     * so it can update the lastDataFetch timestamp of a user-site and keep some additional administration that is
     * mostly useful for scraping sites (last saved transaction id).
     *
     * @param kafkaTemplate the kafka template to use
     * @param activityId    the activityId for which data was saved to the db by A&T
     * @param provider      the provider of the usersite
     * @param userSiteId    identifier of the usersite for which data was saved
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void sendKafkaIngestionFinishedEvent(
            KafkaTemplate kafkaTemplate,
            UUID activityId,
            String provider,
            UUID userSiteId,
            ClientUserToken clientUserToken
    ) {
        var now = ZonedDateTime.now(systemUTC());
        var accountId = UUID.randomUUID();
        kafkaTemplate.send(MessageBuilder
                .withPayload(new IngestionFinishedEvent(
                        clientUserToken.getUserIdClaim(),
                        activityId,
                        now,
                        userSiteId,
                        String.format("%04d%02d", now.getYear(), now.getMonthValue()), // YYYYMM
                        String.format("%04d%02d", now.getYear() + 1, now.getMonthValue()), // YYYYMM (next year)
                        Map.of(accountId, new IngestionFinishedEvent.AccountInformationDTO("iban", provider, "123")),
                        Map.of(accountId, now.toLocalDate().minusDays(7))
                ))
                .setHeader(KafkaHeaders.TOPIC, "activityEvents")
                .setHeader(KafkaHeaders.MESSAGE_KEY, ActivityEventSerializer.serialize(new ActivityEventKey(clientUserToken.getUserIdClaim(), activityId, new UUID(0, 0))))
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .build()
        ).completable().join();
    }

    /**
     * Fake a 200 response from accounts-and-transactions when retrieving the user site transaction summary.
     * <p>
     * The returned summary is (currently) always empty.
     *
     * @param wireMockServer the server to add the stub to
     */
    @SneakyThrows
    public static void setupUserSiteTransactionStatusSummary(WireMockServer wireMockServer) {
        wireMockServer.stubFor(WireMock.get(urlMatching("/accounts-and-transactions/internal/[^/]+/user-site-transaction-status-summary"))
                .withMetadata(WiremockStubManager.flowStubMetaData)
                .willReturn(aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("content-type", APPLICATION_JSON_VALUE)
                        .withBody("[]")
                ));
    }

    /**
     * Fake a 200 response from accounts-and-transactions when retrieving a user's accounts.
     *
     * @param wireMockServer the server to add the stub to
     * @param userId         the user id to fake an accounts response for
     */
    public static void setupUserAccounts(WireMockServer wireMockServer, UUID userId) {
        wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/accounts-and-transactions/v1/users/" + userId + "/accounts"))
                .willReturn(aResponse()
                        .withStatus(OK.value())
                        .withHeader("content-type", APPLICATION_JSON_VALUE)
                        .withBody("[]")
                ));
    }

}
