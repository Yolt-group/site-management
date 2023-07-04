package nl.ing.lovebird.sitemanagement.health.orchestration;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.health.orchestration.HealthOrchestrationEventType.RefreshFinished;

@Component
@Slf4j
public class HealthOrchestrationProducer {

    private final String topic;
    private final boolean enabled;
    private final KafkaTemplate<String, HealthOrchestrationEvent> kafkaTemplate;

    @Autowired
    public HealthOrchestrationProducer(@Value("${yolt.kafka.topics.healthOrchestration.topic-name}") final String topic,
                                       @Value("${yolt.kafka.producing.enabled}") final boolean enabled,
                                       final KafkaTemplate<String, HealthOrchestrationEvent> kafkaTemplate) {
        this.topic = topic;
        this.enabled = enabled;
        this.kafkaTemplate = kafkaTemplate;
    }

    public ListenableFuture<SendResult<String, HealthOrchestrationEvent>> publishRefreshFinishedEvent(
            final ClientUserToken clientUserToken,
            final HealthOrchestrationEventOrigin origin,
            final UUID correlationId,
            final List<UUID> userSiteIds
    ) {
        final HealthOrchestrationEvent event = HealthOrchestrationEvent.builder()
                .type(RefreshFinished)
                .origin(origin)
                .correlationId(correlationId)
                .userSiteIds(userSiteIds)
                .build();

        if (!enabled) {
            log.info("Producing disabled, skipping {} event", event.getType().name());
            return null;
        }

        final Message<HealthOrchestrationEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, clientUserToken.getUserIdClaim().toString())
                .setHeader(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .build();

        final ListenableFuture<SendResult<String, HealthOrchestrationEvent>> future = kafkaTemplate.send(message);
        future.addCallback(
                result -> log.debug("Published event: {}", event.getType().name()),
                ex -> log.error("Failed to publish event: " + event.getType().name(), ex)
        );

        return future;
    }

}
