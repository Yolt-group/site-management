package nl.ing.lovebird.sitemanagement.health.webhook;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.*;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.health.webhook.consumer.TestClientWebhookEventEnvelopeConsumer;
import nl.ing.lovebird.sitemanagement.health.webhook.consumer.TestClientWebhookEventEnvelopeConsumer.Consumed;
import nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload;
import nl.ing.lovebird.sitemanagement.health.webhook.dto.WebhookEventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.time.Clock.fixed;
import static java.time.Clock.systemUTC;
import static java.util.Collections.*;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.activityevents.events.ConnectionStatus.CONNECTED;
import static nl.ing.lovebird.activityevents.events.ConnectionStatus.DISCONNECTED;
import static nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.WebhookActivity.USER_REFRESH;
import static nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.WebhookEvent.ACTIVITY_FINISHED;
import static nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.WebhookEvent.DATA_SAVED;
import static nl.ing.lovebird.sitemanagement.health.webhook.dto.WebhookEventEnvelope.WebhookEventType.AIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@Slf4j
@IntegrationTestContext
public class ClientWebhookServiceIntegrationTest {

    private static final Instant NOW = fixed(Instant.now(systemUTC()), ZoneId.systemDefault()).instant();

    private final UUID clientId = randomUUID();

    @Autowired
    private ClientWebhookService service;

    @Autowired
    private TestClientWebhookEventEnvelopeConsumer testWebhookConsumer;

    @Autowired
    private Clock clock;

    @Autowired
    private TestClientTokens testClientTokens;

    @BeforeEach
    public void onBefore() {
        when(clock.instant()).thenReturn(NOW);

        testWebhookConsumer.reset();
    }

    @Test
    public void testPushIngestionFinished() {
        UUID userId = randomUUID();
        UUID activityId = randomUUID();
        UUID userSiteId = randomUUID();
        UUID accountIdA = randomUUID();

        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(randomUUID(), clientId, userId);

        List<AbstractEvent> activityEvents
                = singletonList(new RefreshUserSitesEvent(userId, activityId, ZonedDateTime.now(clock), singletonList(userSiteId)));

        IngestionFinishedEvent ingestionFinishedEvent = IngestionFinishedEvent.builder()
                .userId(userId)
                .activityId(activityId)
                .userSiteId(userSiteId)
                .endYearMonth("1")
                .startYearMonth("12")
                .time(ZonedDateTime.now(clock))
                .accountIdToOldestTransactionChangeDate(Map.of(accountIdA, LocalDate.EPOCH))
                .build();

        service.push(clientUserToken, activityEvents, ingestionFinishedEvent);

        await().untilAsserted(() -> {
            List<Consumed<WebhookEventEnvelope>> consumed = testWebhookConsumer.getConsumed();
            assertThat(consumed).hasSize(1);

            var tok = consumed.get(0).getClientUserToken();
            assertThat(tok.getUserIdClaim()).isEqualTo(userId);

            assertThat(consumed.get(0).getValue()).isEqualTo(WebhookEventEnvelope.builder()
                    .userId(userId)
                    .clientId(clientId)
                    .webhookEventType(AIS)
                    .submittedAt(ZonedDateTime.now(clock).withFixedOffsetZone())
                    .payload(AISWebhookEventPayload.builder()
                            .activityId(activityId)
                            .activity(USER_REFRESH)
                            .event(DATA_SAVED)
                            .dateTime(ZonedDateTime.now(clock).withFixedOffsetZone())
                            .userSites(singleton(AISWebhookEventPayload.UserSiteResult.builder()
                                    .userSiteId(userSiteId)
                                    .connectionStatus(CONNECTED)
                                    .failureReason(empty())
                                    .lastDataFetch(Optional.of(ZonedDateTime.now(clock).withFixedOffsetZone()))
                                    .accounts(List.of(AISWebhookEventPayload.AffectedAccount.builder()
                                            .accountId(accountIdA)
                                            .oldestChangedTransaction(LocalDate.EPOCH)
                                            .build()))
                                    .build()))
                            .build())
                    .build());
        });
    }


    @Test
    public void testPushRefreshFinished() {
        UUID userId = randomUUID();
        UUID activityId = randomUUID();
        UUID userSiteId = randomUUID();

        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(randomUUID(), clientId, userId);
        RefreshedUserSiteEvent refreshedUserSiteEvent = new RefreshedUserSiteEvent(
                userId,
                activityId,
                ZonedDateTime.now(clock),
                userSiteId,
                DISCONNECTED,
                FailureReason.TECHNICAL_ERROR,
                RefreshedUserSiteEvent.Status.FAILED);

        List<AbstractEvent> activityEvents
                = List.of(new RefreshUserSitesEvent(userId, activityId, ZonedDateTime.now(clock), singletonList(userSiteId)), refreshedUserSiteEvent);

        service.push(clientUserToken, activityEvents, refreshedUserSiteEvent);

        await().untilAsserted(() -> {
            List<Consumed<WebhookEventEnvelope>> consumed = testWebhookConsumer.getConsumed();
            assertThat(consumed).hasSize(1);

            var tok = consumed.get(0).getClientUserToken();
            assertThat(tok.getUserIdClaim()).isEqualTo(userId);

            assertThat(consumed.get(0).getValue()).isEqualTo(WebhookEventEnvelope.builder()
                    .userId(userId)
                    .clientId(clientId)
                    .webhookEventType(AIS)
                    .submittedAt(ZonedDateTime.now(clock).withFixedOffsetZone())
                    .payload(AISWebhookEventPayload.builder()
                            .activityId(activityId)
                            .activity(USER_REFRESH)
                            .event(ACTIVITY_FINISHED)
                            .dateTime(ZonedDateTime.now(clock).withFixedOffsetZone())
                            .userSites(singleton(AISWebhookEventPayload.UserSiteResult.builder()
                                    .userSiteId(userSiteId)
                                    .connectionStatus(DISCONNECTED)
                                    .failureReason(Optional.of(FailureReason.TECHNICAL_ERROR))
                                    .lastDataFetch(Optional.of(ZonedDateTime.now(clock).withFixedOffsetZone()))
                                    .accounts(emptyList())
                                    .build()))
                            .build())
                    .build());
        });
    }

    @Test
    public void testPushAggregationFinished() {
        UUID userId = randomUUID();
        UUID clientUserId = randomUUID();
        UUID activityId = randomUUID();
        UUID userSiteId = randomUUID();

        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(randomUUID(), clientId, userId);

        List<AbstractEvent> activityEvents = Lists.newArrayList(
                new RefreshUserSitesEvent(userId, activityId, ZonedDateTime.now(clock), singletonList(userSiteId)),
                new RefreshedUserSiteEvent(
                        userId,
                        activityId,
                        ZonedDateTime.now(clock),
                        userSiteId,
                        ConnectionStatus.DISCONNECTED,
                        FailureReason.TECHNICAL_ERROR,
                        RefreshedUserSiteEvent.Status.FAILED));

        AggregationFinishedEvent aggregationFinishedEvent = AggregationFinishedEvent.builder()
                .activityId(activityId)
                .userId(clientUserId)
                .time(ZonedDateTime.now(clock))
                .initialActivityEventType(EventType.REFRESH_USER_SITES)
                .userSiteIds(singletonList(userSiteId))
                .build();

        service.push(clientUserToken, activityEvents, aggregationFinishedEvent);

        await().untilAsserted(() -> {
            List<Consumed<WebhookEventEnvelope>> consumed = testWebhookConsumer.getConsumed();
            assertThat(consumed).hasSize(1);

            var tok = consumed.get(0).getClientUserToken();
            assertThat(tok.getUserIdClaim()).isEqualTo(userId);

            WebhookEventEnvelope receivedEnvelope = consumed.get(0).getValue();
            assertThat(receivedEnvelope).isEqualTo(WebhookEventEnvelope.builder()
                    .userId(userId)
                    .clientId(clientId)
                    .webhookEventType(AIS)
                    .submittedAt(ZonedDateTime.now(clock).withFixedOffsetZone())
                    .payload(AISWebhookEventPayload.builder()
                            .activityId(activityId)
                            .activity(USER_REFRESH)
                            .event(ACTIVITY_FINISHED)
                            .dateTime(ZonedDateTime.now(clock).withFixedOffsetZone())
                            .userSites(singleton(AISWebhookEventPayload.UserSiteResult.builder()
                                    .userSiteId(userSiteId)
                                    .connectionStatus(DISCONNECTED)
                                    .failureReason(Optional.of(FailureReason.TECHNICAL_ERROR))
                                    .lastDataFetch(Optional.of(ZonedDateTime.now(clock).withFixedOffsetZone()))
                                    .accounts(emptyList())
                                    .build()))
                            .build())
                    .build());

        });
    }
}
