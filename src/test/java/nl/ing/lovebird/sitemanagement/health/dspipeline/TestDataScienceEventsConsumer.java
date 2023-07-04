package nl.ing.lovebird.sitemanagement.health.dspipeline;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.assertj.core.util.Lists.newArrayList;

@Slf4j
@Component
public class TestDataScienceEventsConsumer {
    private final List<Consumed> consumed = newArrayList();

    @KafkaListener(
            topics = "${yolt.kafka.topics.datasciencepipeline.topic-name}",
            concurrency = "${yolt.kafka.topics.datasciencepipeline.listener-concurrency}",
            groupId = "test-consumer"
    )
    public void consume(
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
            @Header(value = UserContext.USER_CONTEXT_HEADER_KEY, required = false) UserContext userContext,
            @Payload DatasciencePipelineValue payload
    ) {
        log.info("received {}", payload);
        consumed.add(new Consumed(key, userContext, payload));
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
        private final UserContext userContext;
        private final DatasciencePipelineValue value;
    }
}
