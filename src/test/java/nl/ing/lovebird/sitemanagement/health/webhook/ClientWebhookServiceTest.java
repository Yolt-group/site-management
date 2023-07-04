package nl.ing.lovebird.sitemanagement.health.webhook;

import nl.ing.lovebird.activityevents.events.AbstractEvent;
import nl.ing.lovebird.activityevents.events.IngestionFinishedEvent;
import nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent;
import nl.ing.lovebird.activityevents.events.TransactionsEnrichmentFinishedEvent;
import nl.ing.lovebird.activityevents.events.TransactionsEnrichmentFinishedEvent.UserSiteInfo;
import nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;
import static nl.ing.lovebird.activityevents.events.ConnectionStatus.CONNECTED;
import static nl.ing.lovebird.activityevents.events.ConnectionStatus.DISCONNECTED;
import static nl.ing.lovebird.activityevents.events.FailureReason.TECHNICAL_ERROR;
import static nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent.Status.FAILED;
import static nl.ing.lovebird.activityevents.events.TransactionsEnrichmentFinishedEvent.Status.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ClientWebhookServiceTest {
    private ClientWebhookService clientWebhookService;

    @Mock
    private KafkaTemplate kafkaTemplate;

    private Clock clock;

    @BeforeEach
    public void init() {
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        clientWebhookService = new ClientWebhookService(clock, kafkaTemplate, "IAmACageInSearchOfABird");
    }

    @Test
    public void testUserSiteResultsWithFailureReason() {
        var userId = randomUUID();
        var activityId = randomUUID();
        var userSiteId = randomUUID();

        List<AbstractEvent> activityEvents = List.of(
                new RefreshedUserSiteEvent(userId, activityId, now(clock), userSiteId, DISCONNECTED, TECHNICAL_ERROR, FAILED));
        var userSiteResults = clientWebhookService.createUserSiteResults(activityEvents, false);

        assertThat(userSiteResults.size()).isEqualTo(1);
        var userSiteResult = userSiteResults.stream().findFirst().orElseThrow();
        assertThat(userSiteResult.userSiteId).isEqualTo(userSiteId);
        assertThat(userSiteResult.lastDataFetch).isPresent();
        assertThat(userSiteResult.failureReason).isPresent();
        assertThat(userSiteResult.failureReason.get()).isEqualTo(TECHNICAL_ERROR);
        assertThat(userSiteResult.connectionStatus).isEqualTo(DISCONNECTED);
        assertThat(userSiteResult.accounts).isEmpty();
    }

    @Test
    public void testUserSiteResultsWithFailureReasonAndEnrichmentResults() {
        var userId = randomUUID();
        var activityId = randomUUID();
        var userSiteId = randomUUID();
        var accountOne = randomUUID();
        var accountTwo = randomUUID();
        var dateOne = LocalDate.of(2020, 3, 14);
        var dateTwo = LocalDate.of(2019, 3, 14);

        List<AbstractEvent> activityEvents = List.of(
                new TransactionsEnrichmentFinishedEvent(userId, activityId, List.of(
                        UserSiteInfo.builder()
                                .userSiteId(userSiteId)
                                .accountId(accountOne)
                                .oldestChangedTransaction(dateOne)
                                .build(),
                        UserSiteInfo.builder()
                                .userSiteId(userSiteId)
                                .accountId(accountTwo)
                                .oldestChangedTransaction(dateTwo)
                                .build()),
                        now(clock), SUCCESS),
                new RefreshedUserSiteEvent(userId, activityId, now(clock), userSiteId, DISCONNECTED, TECHNICAL_ERROR, FAILED));
        var userSiteResults = clientWebhookService.createUserSiteResults(activityEvents, false);

        assertThat(userSiteResults.size()).isEqualTo(1);
        var userSiteResult = userSiteResults.stream().findFirst().orElseThrow();
        assertThat(userSiteResult.userSiteId).isEqualTo(userSiteId);
        assertThat(userSiteResult.lastDataFetch).isPresent();
        assertThat(userSiteResult.failureReason).isPresent();
        assertThat(userSiteResult.failureReason.get()).isEqualTo(TECHNICAL_ERROR);
        assertThat(userSiteResult.connectionStatus).isEqualTo(DISCONNECTED);
        assertThat(userSiteResult.accounts.size()).isEqualTo(2);
        assertThat(userSiteResult.accounts.stream().map(AISWebhookEventPayload.AffectedAccount::getAccountId).collect(toSet())).containsExactlyInAnyOrder(accountOne, accountTwo);
        assertThat(userSiteResult.accounts.stream().map(AISWebhookEventPayload.AffectedAccount::getOldestChangedTransaction).collect(toSet())).containsExactlyInAnyOrder(dateOne, dateTwo);
    }

    @Test
    public void testOldestTransactionChangeDateIsTakenFromIngestionAndEnrichmentEvent() {

        var userId = randomUUID();
        var activityId = randomUUID();
        var userSiteIdSuccess = randomUUID();
        var userSiteIdFailed = randomUUID();
        var accountOne = randomUUID();
        var accountTwo = randomUUID();

        var ingestionDateOne = LocalDate.of(2021, 3, 1);
        var ingestionDateTwo = LocalDate.of(2020, 2, 2);

        // enrichment oldest changes transaction dates are earlier then ingestion dates
        var enrichmentDateOne = LocalDate.of(2019, 3, 3);
        var enrichmentDateTwo = LocalDate.of(2018, 3, 4);


        List<AbstractEvent> activityEvents = List.of(
                new IngestionFinishedEvent(userId, activityId, now(clock), userSiteIdSuccess, "2021-01", "2021-02", emptyMap(), Map.of(
                        accountOne, ingestionDateOne,
                        accountTwo, ingestionDateTwo
                )),
                new TransactionsEnrichmentFinishedEvent(userId, activityId, List.of(
                        UserSiteInfo.builder()
                                .userSiteId(userSiteIdSuccess)
                                .accountId(accountOne)
                                .oldestChangedTransaction(enrichmentDateOne)
                                .build(),
                        UserSiteInfo.builder()
                                .userSiteId(userSiteIdSuccess)
                                .accountId(accountTwo)
                                .oldestChangedTransaction(enrichmentDateTwo)
                                .build()),
                        now(clock), SUCCESS),
                new RefreshedUserSiteEvent(userId, activityId, now(clock), userSiteIdFailed, DISCONNECTED, TECHNICAL_ERROR, FAILED));

        var userSiteResults = clientWebhookService.createUserSiteResults(activityEvents, false);

        assertThat(userSiteResults)
                .containsExactlyInAnyOrder(
                        AISWebhookEventPayload.UserSiteResult.builder()
                                .userSiteId(userSiteIdFailed)
                                .connectionStatus(DISCONNECTED)
                                .lastDataFetch(Optional.of(ZonedDateTime.now(clock)))
                                .failureReason(Optional.of(TECHNICAL_ERROR))
                                .accounts(emptyList())
                                .build(),

                        AISWebhookEventPayload.UserSiteResult.builder()
                                .userSiteId(userSiteIdSuccess)
                                .connectionStatus(CONNECTED)
                                .lastDataFetch(Optional.of(ZonedDateTime.now(clock)))
                                .failureReason(Optional.empty())
                                .accounts(List.of(
                                        AISWebhookEventPayload.AffectedAccount.builder()
                                                .accountId(accountOne)
                                                .oldestChangedTransaction(enrichmentDateOne)
                                                .build(),
                                        AISWebhookEventPayload.AffectedAccount.builder()
                                                .accountId(accountTwo)
                                                .oldestChangedTransaction(enrichmentDateTwo)
                                                .build())
                                        .stream()
                                        .sorted() // required for assertj
                                        .collect(Collectors.toList()))
                                .build()
                );

    }

}
