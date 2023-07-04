package nl.ing.lovebird.sitemanagement.health.dspipeline;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.health.HealthMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.Collections;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.springframework.kafka.support.KafkaHeaders.MESSAGE_KEY;
import static org.springframework.kafka.support.KafkaHeaders.TOPIC;

@Component
@Slf4j
public class StartDatasciencePipelineProducer {

    private final String topic;
    private final boolean enabled;
    private final KafkaTemplate<String, DatasciencePipelineValue> kafkaTemplate;
    private final HealthMetrics healthMetrics;

    @Autowired
    public StartDatasciencePipelineProducer(@Value("${yolt.kafka.topics.datasciencepipeline.topic-name}") final String topic,
                                            @Value("${yolt.kafka.producing.enabled}") final boolean enabled,
                                            final KafkaTemplate<String, DatasciencePipelineValue> kafkaTemplate,
                                            HealthMetrics healthMetrics) {
        this.topic = topic;
        this.enabled = enabled;
        this.kafkaTemplate = kafkaTemplate;
        this.healthMetrics = healthMetrics;
    }

    public ListenableFuture<SendResult<String, DatasciencePipelineValue>> sendRefreshTriggeredEvent(
            final ClientUserToken clientUserToken,
            final DatasciencePipelineValue value) {
        if (!enabled) {
            log.info("Producing disabled, skipping DatasciencePipelineValue");
            return null;
        }

        UUID userId = clientUserToken.getUserIdClaim();
        // DS requires these fields to be present in the UserContext.  Hence we create a fake UserContext.
        // See https://git.yolt.io/datascience/datascience-commons/-/blob/master/commons/src/main/scala/com/yolt/datascience/infrastructure/tracing/UserContext.scala
        final Message<DatasciencePipelineValue> message = MessageBuilder
                .withPayload(value)
                .setHeader(TOPIC, topic)
                .setHeader(MESSAGE_KEY, userId.toString())
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .setHeader(UserContext.USER_CONTEXT_HEADER_KEY, value.getPayload().getUserContext().toJson())
                .build();

        ListenableFuture<SendResult<String, DatasciencePipelineValue>> future = kafkaTemplate.send(message);
        future.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onSuccess(SendResult<String, DatasciencePipelineValue> response) {
                healthMetrics.incrementSentDSPipelineTriggerEvents();
                log.debug("Sent datascience pipeline message to topic {}", topic);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                String errorMessage = String.format("Failed sending message to topic: %s, key: %s", topic, userId);
                log.error(errorMessage, t);
            }
        });
        return future;
    }

}
