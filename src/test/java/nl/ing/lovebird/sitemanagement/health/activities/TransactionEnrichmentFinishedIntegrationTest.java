package nl.ing.lovebird.sitemanagement.health.activities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.*;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.health.Activity;
import nl.ing.lovebird.sitemanagement.health.ActivityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static java.util.function.Predicate.not;
import static nl.ing.lovebird.activityevents.events.ConnectionStatus.CONNECTED;
import static nl.ing.lovebird.activityevents.events.FailureReason.TECHNICAL_ERROR;
import static nl.ing.lovebird.activityevents.events.TransactionsEnrichmentFinishedEvent.Status.SUCCESS;
import static nl.ing.lovebird.activityevents.events.TransactionsEnrichmentFinishedEvent.Status.TIMEOUT;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.WebhookActivity.UPDATE_USER_SITE;
import static nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.WebhookEvent.ACTIVITY_FINISHED;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.kafka.support.KafkaHeaders.MESSAGE_KEY;
import static org.springframework.kafka.support.KafkaHeaders.TOPIC;

@IntegrationTestContext
class TransactionEnrichmentFinishedIntegrationTest {

    @Value("${yolt.kafka.topics.activityEvents.topic-name}")
    private String topic;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, AbstractEvent> kafkaTemplate;

    @Autowired
    private ActivityEventService activityEventService;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private WebhookEventsTestConsumer testConsumer;

    @Autowired
    private PersistedActivityService persistedActivityService;

    @Autowired
    private Clock clock;

    @Autowired
    private TestClientTokens testClientTokens;

    private static final ZonedDateTime ACTIVITY_START_TIME = ZonedDateTime.now().minusHours(6);
    private static final ZonedDateTime ACTIVITY_END_TIME = ACTIVITY_START_TIME.plusSeconds(2);

    @Test
    public void testReceiptOfEnrichmentFinishedEvent() throws JsonProcessingException {
        var activityId = randomUUID();
        var userId = randomUUID();
        var userSiteId = randomUUID();
        var clientToken = testClientTokens.createClientUserToken(randomUUID(), randomUUID(), userId);
        var startEvent = new UpdateUserSiteEvent(userId, randomUUID(), activityId, "sitename", ACTIVITY_START_TIME, userSiteId);
        startActivity(startEvent);

        activityEventService.storeEvent(new IngestionFinishedEvent(userId, activityId, ZonedDateTime.now(clock), userSiteId, "2021-01", "2021-02", emptyMap(), emptyMap()));
        activityEventService.storeEvent(new AggregationFinishedEvent(userId, activityId, ZonedDateTime.now(clock), EventType.UPDATE_USER_SITE, List.of(userSiteId)));

        kafkaTemplate.send(MessageBuilder
                .withPayload((AbstractEvent) new TransactionsEnrichmentFinishedEvent(userId, activityId, emptyList(), ACTIVITY_END_TIME, SUCCESS))
                .setHeader(TOPIC, topic)
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(MESSAGE_KEY, objectMapper.writeValueAsString(
                        new ActivityEventKey(userId, activityId, randomUUID())))
                .build());

        await().timeout(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> testConsumer.getConsumed(),
                        events -> events.stream()
                                .map(event -> event.payload)
                                .anyMatch(payload ->
                                        payload.activityId.equals(activityId) &&
                                                payload.event.equals(ACTIVITY_FINISHED) &&
                                                payload.activity.equals(UPDATE_USER_SITE) &&
                                                payload.userSites.size() == 1 &&
                                                payload.userSites.stream()
                                                        .anyMatch(not(userSiteResult -> userSiteResult.failureReason.isPresent()))
                                ));

        // additionally check if the activity has been marked as finished in the database.
        assertThat(activityRepository.findById(activityId))
                .isPresent()
                .get()
                .returns(ACTIVITY_START_TIME.toInstant().truncatedTo(ChronoUnit.MILLIS), Activity::getStartTime)
                .returns(ACTIVITY_END_TIME.toInstant().truncatedTo(ChronoUnit.MILLIS), Activity::getEndTime);
    }

    /**
     * This unit test does not make sense. This tests a situation that can never occur.
     * See https://yolt.atlassian.net/browse/YCO-1710 for more information about this issue.
     * This test is left here to be amended when this ticket is closed.
     */
    @Test
    public void testReceiptOfEnrichmentFinishedEventForTimeout() throws JsonProcessingException {
        var aDate = ZonedDateTime.now().minusHours(6);
        var activityId = randomUUID();
        var userId = randomUUID();
        var clientToken = testClientTokens.createClientUserToken(randomUUID(), randomUUID(), userId);

        var startEvent = new UpdateUserSiteEvent(userId, randomUUID(), activityId, "sitename", ACTIVITY_START_TIME, randomUUID());
        startActivity(startEvent);
        activityEventService.storeEvent(new RefreshedUserSiteEvent(userId, activityId, aDate.plusSeconds(1), randomUUID(), CONNECTED, null, RefreshedUserSiteEvent.Status.FAILED));

        kafkaTemplate.send(MessageBuilder
                .withPayload((AbstractEvent) new TransactionsEnrichmentFinishedEvent(userId, activityId, emptyList(), ACTIVITY_END_TIME, TIMEOUT))
                .setHeader(TOPIC, topic)
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(MESSAGE_KEY, objectMapper.writeValueAsString(
                        new ActivityEventKey(userId, activityId, randomUUID())))
                .build());

        await().timeout(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> testConsumer.getConsumed(),
                        events -> events.stream()
                                .map(event -> event.payload)
                                .anyMatch(payload ->
                                        payload.activityId.equals(activityId) &&
                                                payload.event.equals(ACTIVITY_FINISHED) &&
                                                payload.activity.equals(UPDATE_USER_SITE) &&
                                                payload.userSites.size() == 1 &&
                                                payload.userSites.stream()
                                                        .flatMap(userSiteResult -> userSiteResult.failureReason.stream())
                                                        .anyMatch(failureReason -> failureReason == TECHNICAL_ERROR)
                                ));
    }

    private void startActivity(UpdateUserSiteEvent startEvent) {
        activityEventService.storeEvent(startEvent);
        persistedActivityService.persistNewActivity(startEvent);
    }
}
