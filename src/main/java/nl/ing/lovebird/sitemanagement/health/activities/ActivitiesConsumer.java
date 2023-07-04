package nl.ing.lovebird.sitemanagement.health.activities;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.events.AbstractEvent;
import nl.ing.lovebird.activityevents.events.ActivityEventKey;
import nl.ing.lovebird.activityevents.events.TransactionsEnrichmentFinishedEvent;
import nl.ing.lovebird.activityevents.events.serializer.ActivityEventSerializer;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.sitemanagement.health.HealthMetrics;
import nl.ing.lovebird.sitemanagement.health.dspipeline.UserContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@RequiredArgsConstructor
@Component
@Slf4j
class ActivitiesConsumer {

    private final ActivityService activityService;
    private final HealthMetrics healthMetrics;
    private final ClientTokenRequesterService clientTokenRequesterService;

    @KafkaListener(
            topics = "${yolt.kafka.topics.activityEvents.topic-name}",
            concurrency = "${yolt.kafka.topics.activityEvents.listener-concurrency}",
            groupId = "site-management-health-consumer"
    )
    void activityUpdate(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) @NonNull final String key,
                        @Header(value = UserContext.USER_CONTEXT_HEADER_KEY, required = false) final String userContextHeader,
                        @Header(value = CLIENT_TOKEN_HEADER_NAME, required = false) ClientUserToken clientUserToken,
                        @Payload final AbstractEvent event) {
        try {
            // There is 1 case where we don't get a ClientUserToken, and that is upon receiving a TRANSACTIONS_ENRICHMENT_FINISHED
            // event with status TIMEOUT from A&T.  It doesn't contain a ClientUserToken because A&T can't request one.
            if (event instanceof TransactionsEnrichmentFinishedEvent tefe && tefe.getStatus() == TransactionsEnrichmentFinishedEvent.Status.TIMEOUT && clientUserToken == null) {
                if (userContextHeader == null) {
                    throw new IllegalStateException("Received a TransactionsEnrichmentFinishedEvent with status TIMEOUT without either a ClientUserToken or UserContext.  Expecting at least one.");
                }
                UserContext userContext = parse(userContextHeader);
                log.info("Hack: requesting clientUserToken on behalf of accounts-and-transactions.");
                clientUserToken = clientTokenRequesterService.getClientUserToken(userContext.getClientId(), userContext.getUserId());
            }

            if (clientUserToken == null) {
                throw new IllegalStateException("Expected a ClientUserToken, did not receive it.");
            }

            ActivityEventKey eventKey = ActivityEventSerializer.deserializeKey(key);
            if (!eventKey.getActivityId().equals(event.getActivityId())) {
                throw new IllegalArgumentException("activity event with eventKey.activityId != event.activityId");
            }

            if (eventKey.getRequestTraceId() != null && !eventKey.getRequestTraceId().equals(new UUID(0, 0))) {
                log.warn("Received ActivityEventKey(type={}) with variable requestTraceId({}).", event.getType(), eventKey.getRequestTraceId());
            }

            healthMetrics.incrementReceivedActivityEventOfType(event.getType());

            activityService.handleEvent(clientUserToken, event);

        } catch (Exception e) {
            log.error("Unexpected exception reading activities: {}", e.getMessage(), e);
        }
    }

    private UserContext parse(String userContextHeader) {
        // Handle JSON encoded strings. Remove when all pods on > 13.0.25
        // See: https://yolt.atlassian.net/browse/CHAP-145
        if (userContextHeader.length() > 1 && userContextHeader.startsWith("\"") && userContextHeader.endsWith("\"")) {
            userContextHeader = userContextHeader.substring(1, userContextHeader.length() - 1);
        }
        return UserContext.fromJson(userContextHeader);
    }
}
