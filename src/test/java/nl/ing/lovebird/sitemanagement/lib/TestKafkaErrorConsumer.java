package nl.ing.lovebird.sitemanagement.lib;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.assertj.core.util.Lists.newArrayList;

@Slf4j
@Component
public class TestKafkaErrorConsumer {
    private final List<ConsumerRecord> consumed = newArrayList();

    @KafkaListener(
            topics = "${yolt.kafka.topics.requests-errors.topic-name}",
            concurrency = "${yolt.kafka.topics.requests-errors.listener-concurrency}"
    )
    public void consume(ConsumerRecord<?, ?> record) {
        log.info("received error message!");
        consumed.add(record);
    }

    public List<ConsumerRecord> getConsumed() {
        return consumed;
    }

}
