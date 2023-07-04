package nl.ing.lovebird.sitemanagement.clientconfiguration;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.assertj.core.util.Lists.newArrayList;

@Component
public class TestConsumer {
    private final List<Consumed> consumed = newArrayList();

    @KafkaListener(
            topics = "${yolt.kafka.topics.clientRedirectUrls.topic-name}",
            concurrency = "${yolt.kafka.topics.clientRedirectUrls.listener-concurrency}"
    )
    public void consume(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, @Payload ClientRedirectUrlDTO payload,
                        @Header("message_type") String messageType) {
        consumed.add(new Consumed(key, payload, messageType));
    }

    public List<Consumed> getConsumed() {
        return consumed;
    }


    @Data
    @AllArgsConstructor
    public static class Consumed {
        private final String key;
        private final ClientRedirectUrlDTO value;
        private final String messageType;
    }

}
