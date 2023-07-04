package nl.ing.lovebird.sitemanagement.health.webhook;

import com.google.common.annotations.VisibleForTesting;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.events.*;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.health.activities.IllegalActivityStateException;
import nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload;
import nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.AffectedAccount;
import nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.UserSiteResult;
import nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.WebhookActivity;
import nl.ing.lovebird.sitemanagement.health.webhook.dto.WebhookEventEnvelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.*;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.concat;
import static nl.ing.lovebird.activityevents.events.ConnectionStatus.CONNECTED;
import static nl.ing.lovebird.activityevents.events.TransactionsEnrichmentFinishedEvent.Status.SUCCESS;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static nl.ing.lovebird.sitemanagement.health.activities.ActivityService.getStartEvent;
import static nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.WebhookActivity.*;
import static nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.WebhookEvent.ACTIVITY_FINISHED;
import static nl.ing.lovebird.sitemanagement.health.webhook.dto.AISWebhookEventPayload.WebhookEvent.DATA_SAVED;
import static nl.ing.lovebird.sitemanagement.health.webhook.dto.WebhookEventEnvelope.WebhookEventType.AIS;
import static org.springframework.kafka.support.KafkaHeaders.MESSAGE_KEY;
import static org.springframework.kafka.support.KafkaHeaders.TOPIC;

/**
 * {@link ClientWebhookService} is responsible for sending callbacks to our clients.
 */
@Slf4j
@Component
public class ClientWebhookService {

    private final Clock clock;
    private final KafkaTemplate<UUID, WebhookEventEnvelope> kafkaTemplate;
    private final String webhooksTopics;

    public ClientWebhookService(final Clock clock,
                                final KafkaTemplate<UUID, WebhookEventEnvelope> kafkaTemplate,
                                final @Value("${yolt.kafka.topics.webhooks.topic-name:#{null}}") String webhooksTopic) {

        this.clock = clock;
        this.kafkaTemplate = kafkaTemplate;
        this.webhooksTopics = webhooksTopic;
    }

    public void push(final @NonNull ClientUserToken clientUserToken,
                     final @NonNull List<AbstractEvent> activityEvents,
                     final @NonNull AbstractEvent event) {
        if (webhooksTopics == null) {
            log.debug("Webhooks disabled. Webhooks Kafka topic not defined.");
            return;
        }

        try {
            createWebhookEventPayload(event, activityEvents)
                    .map(payload -> wrapInEnvelope(payload, clientUserToken))
                    .map(envelope -> createMessage(envelope, clientUserToken))
                    .map(kafkaTemplate::send)
                    .ifPresent(this::logResult);
        } catch (Throwable throwable) {
            log.error("An error occurred while pushing web-hook", throwable);
        }
    }

    private WebhookEventEnvelope wrapInEnvelope(AISWebhookEventPayload webhookEvent, ClientUserToken clientUserToken) {
        try {
            var userSiteListSize = webhookEvent.userSites.size();
            log.debug("Creating webhook envelope for activity {} and event {} and has #usersites: {}", webhookEvent.activity, webhookEvent.event, userSiteListSize);
        } catch (Exception e) {
            log.warn("Failed log AIS webhook envelope creation,");
        }
        return WebhookEventEnvelope.builder()
                .clientId(clientUserToken.getClientIdClaim())
                .userId(clientUserToken.getUserIdClaim())
                .webhookEventType(AIS)
                .payload(webhookEvent)
                .submittedAt(ZonedDateTime.now(clock))
                .build();
    }

    private Message<WebhookEventEnvelope> createMessage(WebhookEventEnvelope envelope, ClientUserToken clientUserToken) {
        return MessageBuilder
                .withPayload(envelope)
                .setHeader(TOPIC, webhooksTopics)
                .setHeader(MESSAGE_KEY, clientUserToken.getUserIdClaim().toString())
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .build();
    }

    private void logResult(ListenableFuture<SendResult<UUID, WebhookEventEnvelope>> listenableFuture) {
        listenableFuture.addCallback(result -> log.debug("Successfully sent message to Kafka."),
                ex -> log.error("Failed to send message to Kafka.", ex));
    }

    private Optional<AISWebhookEventPayload> createWebhookEventPayload(AbstractEvent event, List<AbstractEvent> activityEvents) {
        if (event instanceof IngestionFinishedEvent) {
            return onDataSaved(activityEvents, (IngestionFinishedEvent) event);
        } else if (event instanceof RefreshedUserSiteEvent) {
            // Note: a RefreshedUserSiteEvent indicates an ERROR state.
            // Therefore onActivityFinishedOnRefresh should be considered an error handler
            return onActivityFinishedOnRefresh(activityEvents, (RefreshedUserSiteEvent) event);
        } else if (event instanceof AggregationFinishedEvent) {
            return onActivitySuccess(activityEvents, event);
        } else if (event instanceof TransactionsEnrichmentFinishedEvent) {
            var status = ((TransactionsEnrichmentFinishedEvent) event).getStatus();
            return status == SUCCESS ?
                    onActivitySuccess(activityEvents, event) :
                    onActivityTimedOut(activityEvents, event);
        } else {
            return empty();
        }
    }

    /**
     * Push `DATA_SAVED` event to the registered webhook endpoint(s) for the given client.
     */
    private Optional<AISWebhookEventPayload> onDataSaved(
            final @NonNull List<AbstractEvent> activityEvents,
            final @NonNull IngestionFinishedEvent ingestionFinishedEvent) {
        var activityId = ingestionFinishedEvent.getActivityId();

        // No need to send DATA_SAVED for feedback activities.
        if (isFeedbackActivity(activityEvents, activityId)) {
            return empty();
        }

        var userSiteStartEvent = getFirstUserSiteStartEvent(activityEvents, activityId);
        return createWebhookActivity(userSiteStartEvent)
                .map(webhookActivity -> AISWebhookEventPayload.builder()
                        .activityId(activityId)
                        .activity(webhookActivity)
                        .event(DATA_SAVED)
                        .dateTime(ingestionFinishedEvent.getTime())
                        .userSites(singleton(createUserSiteResult(ingestionFinishedEvent)))
                        .build());
    }

    /**
     * Push `ACTIVITY_FINISHED`
     * Note that this should only be called when *all* usersites within the activity failed.
     * If 1 of them succeeded, the activity would still be ongoing and datascience would be involved. Otherwise, we stop here.
     */
    private Optional<AISWebhookEventPayload> onActivityFinishedOnRefresh(
            final @NonNull List<AbstractEvent> activityEvents,
            final @NonNull RefreshedUserSiteEvent refreshedUserSiteEvent) {

        UUID activityId = refreshedUserSiteEvent.getActivityId();
        UserSiteStartEvent startEvent = getFirstUserSiteStartEvent(activityEvents, activityId);

        Set<UserSiteResult> userSiteResults = getEventsOfType(activityEvents, RefreshedUserSiteEvent.class).stream()
                .map(event -> createUserSiteResult(event, false))
                .collect(toSet());

        return createWebhookActivity(startEvent)
                .map(webhookActivity -> AISWebhookEventPayload.builder()
                        .activityId(activityId)
                        .activity(webhookActivity)
                        .event(ACTIVITY_FINISHED)
                        .dateTime(refreshedUserSiteEvent.getTime())
                        .userSites(userSiteResults)
                        .build());
    }

    /**
     * Push `ACTIVITY_FINISHED` event to the registered webhook endpoint(s) for the given client.
     */
    private Optional<AISWebhookEventPayload> onActivitySuccess(final @NonNull List<AbstractEvent> activityEvents, final @NonNull AbstractEvent event) {
        return onActivityFinish(activityEvents, event, false);
    }

    private Optional<AISWebhookEventPayload> onActivityTimedOut(final @NonNull List<AbstractEvent> activityEvents, final @NonNull AbstractEvent event) {
        return onActivityFinish(activityEvents, event, true);
    }

    private Optional<AISWebhookEventPayload> onActivityFinish(final @NonNull List<AbstractEvent> activityEvents, final @NonNull AbstractEvent event, boolean timeout) {
        if (isFeedbackActivity(activityEvents, event.getActivityId())) {
            log.debug("Creating ACTIVITY_FINISHED payload for webhook for activity {} (origin: FEEDBACK)", event.getActivityId());
            return getFeedbackEventPayload(activityEvents, event);
        } else {
            log.debug("Creating ACTIVITY_FINISHED payload for webhook for activity {} (origin: non-FEEDBACK)", event.getActivityId());
            var startEvent = getFirstUserSiteStartEvent(activityEvents, event.getActivityId());
            return getUserSiteEventPayload(activityEvents, event, timeout, startEvent);
        }
    }

    private Optional<AISWebhookEventPayload> getUserSiteEventPayload(List<AbstractEvent> activityEvents, AbstractEvent event, boolean timeout, UserSiteStartEvent startEvent) {
        return createWebhookActivity(startEvent)
                .map(webhookActivity -> AISWebhookEventPayload.builder()
                        .activity(webhookActivity)
                        .activityId(event.getActivityId())
                        .event(ACTIVITY_FINISHED)
                        .dateTime(event.getTime())
                        .userSites(createUserSiteResults(activityEvents, timeout))
                        .build());
    }

    /*
     * The construction of the Feedback webhook payload is done based on the contents of the TransactionsEnrichmentFinishedEvent.
     * Since the order of the events is not known it might be the event that has arrived or that has arrived previously. Get that
     * event and use it to construct the payload.
     */
    private Optional<AISWebhookEventPayload> getFeedbackEventPayload(List<AbstractEvent> activityEvents, AbstractEvent event) {
        return getEnrichmentFinishedEvent(event, activityEvents)
                .map(enrichmentFinishedEvent -> AISWebhookEventPayload.builder()
                        .activity(USER_FEEDBACK)
                        .activityId(event.getActivityId())
                        .event(ACTIVITY_FINISHED)
                        .dateTime(event.getTime())
                        .userSites(createUserSiteResults(enrichmentFinishedEvent.getUserSiteInfo()))
                        .build());
    }

    /*
     * Check if the incoming event is the TransactionsEnrichmentFinishedEvent or otherwise look for it in the previously received events.
     */
    private Optional<TransactionsEnrichmentFinishedEvent> getEnrichmentFinishedEvent(AbstractEvent event, List<AbstractEvent> events) {
        if (event instanceof TransactionsEnrichmentFinishedEvent) {
            return Optional.of((TransactionsEnrichmentFinishedEvent) event);
        } else {
            var result = getEventsOfType(events, TransactionsEnrichmentFinishedEvent.class).stream().findFirst();
            if (result.isPresent()) {
                log.info("Received event of type {} where a TransactionsEnrichmentFinishedEvent was expected. Found TransactionsEnrichmentFinishedEvent as a past event; using that one instead.", event.getType());
            }
            return result;
        }
    }

    /**
     * Create user-site results for ingestion finished events and refresh user-site events (success vs failed).
     * <p/>
     * This method contains logic errors. The (transaction enrichment) timeout property is propagated
     * while there is no good way of conveying this information.
     * <p>
     * See https://yolt.atlassian.net/browse/YCO-1710
     */
    @VisibleForTesting
    Set<UserSiteResult> createUserSiteResults(List<AbstractEvent> activityEvents, boolean timeout) {

        // collect the results for every user-site that was ingested within this activity
        var ingestionFinishedUserSiteResult = getEventsOfType(activityEvents, IngestionFinishedEvent.class).stream()
                .map(ClientWebhookService::createUserSiteResult);

        // collect the results for every user-site that failed within this activity
        var failedUserSiteResults = getEventsOfType(activityEvents, RefreshedUserSiteEvent.class).stream()
                .map(event -> createUserSiteResult(event, timeout));

        // collect the oldest changed transaction dates from the enrichments per account across all user-sites in this activity
        var enrichmentResultsForAllUserSites = getEventsOfType(activityEvents, TransactionsEnrichmentFinishedEvent.class).stream()
                .findFirst()
                .map(enrichmentFinishedEvent -> enrichmentFinishedEvent.getUserSiteInfo().stream()
                        .collect(groupingBy(TransactionsEnrichmentFinishedEvent.UserSiteInfo::getUserSiteId,
                                collectingAndThen(toList(), ClientWebhookService::createAffectedAccount))))
                .orElse(emptyMap());

        return concat(ingestionFinishedUserSiteResult, failedUserSiteResults)
                .map(userSiteResult -> {
                    // if there are enrichments which apply to accounts where the oldest transaction change date
                    // is further in the past then the oldest transaction change date currently known for this user-site result,
                    // choose the oldest of the two.
                    var consolidatedAffectedAccounts = consolidateAffectedAccounts(
                            userSiteResult.accounts,
                            enrichmentResultsForAllUserSites.getOrDefault(userSiteResult.userSiteId, emptyList()));

                    return userSiteResult.toBuilder()
                            .accounts(consolidatedAffectedAccounts) // override the accounts
                            .build();
                })
                .collect(toSet());
    }

    /**
     * Given two sets of {@see AffectedAccount}s, remove any duplicates by choosing the {@see AffectedAccount} with
     * the oldest transaction change date.
     *
     * @param a a set of {@see AffectedAccount}s
     * @param b a set of {@see AffectedAccount}s
     * @return a list of consolidated {@see AffectedAccount}s
     */
    private static List<AffectedAccount> consolidateAffectedAccounts(
            final @Nullable List<AffectedAccount> a,
            final @Nullable List<AffectedAccount> b) {

        if (a == null && b == null) {
            return emptyList();
        } else if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        }

        return concat(a.stream(), b.stream())
                .collect(groupingBy(AffectedAccount::getAccountId))
                .values()
                .stream()
                .flatMap(affectedAccounts -> {
                    // select the oldest changed transaction from the list of candidates
                    var selected = affectedAccounts.stream()
                            .filter(account -> nonNull(account.getOldestChangedTransaction())) // filter out accounts for which there is no oldest transaction change date information.
                            .min(comparing(AffectedAccount::getOldestChangedTransaction));

                    return selected.stream(); // flatten optional
                })
                .sorted(AffectedAccount::compareTo)
                .collect(toList());
    }

    private Set<UserSiteResult> createUserSiteResults(List<TransactionsEnrichmentFinishedEvent.UserSiteInfo> userSiteInfos) {
        if (userSiteInfos == null) {
            return emptySet();
        }

        var groupedUserSiteInfos = userSiteInfos.stream()
                .collect(groupingBy(TransactionsEnrichmentFinishedEvent.UserSiteInfo::getUserSiteId));

        return groupedUserSiteInfos.entrySet()
                .stream()
                .map(it -> UserSiteResult.builder()
                        .userSiteId(it.getKey())
                        .connectionStatus(CONNECTED)
                        .failureReason(empty())
                        .lastDataFetch(empty())
                        .accounts(createAffectedAccount(it.getValue()))
                        .build())
                .collect(toSet());
    }

    private static UserSiteResult createUserSiteResult(final RefreshedUserSiteEvent refreshedUserSiteEvent, boolean timeout) {

        FailureReason failureReason = null;
        if (refreshedUserSiteEvent.getFailureReason() != null) {
            failureReason = refreshedUserSiteEvent.getFailureReason();
        } else {
            if (timeout) {
                failureReason = FailureReason.TECHNICAL_ERROR;
            }
        }

        return UserSiteResult.builder()
                .userSiteId(refreshedUserSiteEvent.getUserSiteId())
                .connectionStatus(refreshedUserSiteEvent.getConnectionStatus())
                .failureReason(Optional.ofNullable(failureReason))
                .accounts(emptyList()) // we have no account information in RefreshedUserSiteEvent
                .lastDataFetch(maybe(refreshedUserSiteEvent.getTime()))
                .build();
    }

    // IngestionFinishedEvent imply an always connected status.
    private static UserSiteResult createUserSiteResult(final IngestionFinishedEvent ingestionFinishedEvent) {

        // Create a list of AffectedAccounts with account-id and oldest transaction change date after ingestion (but before datascience)
        // These oldest transaction change dates are not to be confused with enrichments oldest transaction change dates.
        // IngestionFinishedEvent#getAccountIdToOldestTransactionChangeDate is nullable.
        var oldestChangedTransactionPerAccount = maybe(ingestionFinishedEvent.getAccountIdToOldestTransactionChangeDate()).orElse(emptyMap())
                .entrySet()
                .stream()
                .filter(e -> nonNull(e.getValue())) // this should never be null, just in case.
                .map(e -> AffectedAccount.builder()
                        .accountId(e.getKey())
                        .oldestChangedTransaction(e.getValue())
                        .build())
                .sorted(AffectedAccount::compareTo)
                .collect(toList());

        return UserSiteResult.builder()
                .userSiteId(ingestionFinishedEvent.getUserSiteId())
                .connectionStatus(CONNECTED)
                .failureReason(empty())
                .lastDataFetch(maybe(ingestionFinishedEvent.getTime()))
                .accounts(oldestChangedTransactionPerAccount)
                .build();
    }

    public static UserSiteStartEvent getFirstUserSiteStartEvent(final @NonNull List<AbstractEvent> activityEvents, final @NonNull UUID activityId) {
        StartEvent startEvent = getStartEvent(activityEvents, activityId);
        if (!(startEvent instanceof UserSiteStartEvent)) {
            throw new IllegalActivityStateException(String.format("Start event of activity %s is not of type UserSiteStartEvent", activityId));
        }
        return (UserSiteStartEvent) startEvent;
    }

    private static <T extends AbstractEvent> Set<T> getEventsOfType(List<? extends AbstractEvent> activityEvents,
                                                                    final Class<T> targetClass) {
        return activityEvents.stream()
                .flatMap(abstractEvent -> abstractEvent.getClass().isAssignableFrom(targetClass) ?
                        Stream.of(targetClass.cast(abstractEvent)) :
                        Stream.empty())
                .collect(toSet());
    }

    private static Optional<WebhookActivity> createWebhookActivity(final UserSiteStartEvent startEvent) {
        final Map<? extends Class<? extends UserSiteStartEvent>, WebhookActivity> m = Map.of(
                CreateUserSiteEvent.class, CREATE_USER_SITE,
                UpdateUserSiteEvent.class, UPDATE_USER_SITE,
                RefreshUserSitesEvent.class, USER_REFRESH,
                RefreshUserSitesFlywheelEvent.class, BACKGROUND_REFRESH);

        return maybe(m.get(startEvent.getClass()));
    }

    private static <T> Optional<T> maybe(final @Nullable T value) {
        return Optional.ofNullable(value);
    }

    private static List<AffectedAccount> createAffectedAccount(List<TransactionsEnrichmentFinishedEvent.UserSiteInfo> userSiteInfos) {
        return userSiteInfos.stream()
                .map(userSiteInfo -> AffectedAccount.builder()
                        .accountId(userSiteInfo.getAccountId())
                        .oldestChangedTransaction(userSiteInfo.getOldestChangedTransaction())
                        .build())
                .sorted(AffectedAccount::compareTo)
                .collect(toList());
    }

    private static boolean isFeedbackActivity(@NonNull final List<AbstractEvent> allEvents, final UUID activityId) {
        var firstEvent = getStartEvent(allEvents, activityId);
        return firstEvent instanceof CounterpartiesFeedbackEvent ||
                firstEvent instanceof CategorizationFeedbackEvent ||
                firstEvent instanceof TransactionCyclesFeedbackEvent;
    }
}
