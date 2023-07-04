package nl.ing.lovebird.sitemanagement.health.dspipeline;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.sitemanagement.health.orchestration.HealthOrchestrationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.assertj.core.util.Lists.newArrayList;

@Component
public class TestHealthOrchestrationEventsConsumer {
    private final List<Consumed> consumed = newArrayList();

    @KafkaListener(
            topics = "${yolt.kafka.topics.healthOrchestration.topic-name}",
            concurrency = "${yolt.kafka.topics.healthOrchestration.listener-concurrency}",
            groupId = "test-consumer"
    )
    public void consume(
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
            @Header(value = ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME) ClientUserToken clientUserToken,
            @Payload HealthOrchestrationEvent payload
    ) {
        consumed.add(new Consumed(key, clientUserToken, payload));
    }

    public List<Consumed> getConsumed() {
        return consumed;
    }

    public void reset() {
        consumed.clear();
    }

    @Data
    @AllArgsConstructor
    public static class Consumed {
        private final String key;
        private final ClientUserToken clientUserToken;
        private final HealthOrchestrationEvent value;
    }
}
