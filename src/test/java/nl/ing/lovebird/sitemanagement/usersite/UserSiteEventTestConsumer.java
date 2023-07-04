package nl.ing.lovebird.sitemanagement.usersite;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.ing.lovebird.sitemanagement.usersiteevent.UserSiteEventAbstract;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.util.Lists.newArrayList;

@Component
public class UserSiteEventTestConsumer {
    private final List<Consumed> consumed = newArrayList();

    @KafkaListener(
            topics = "${yolt.kafka.topics.user-site-events.topic-name}",
            concurrency = "1"
    )
    public void consume(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, @Payload UserSiteEventAbstract payload,
                        @Header("type") String messageType) {
        consumed.add(new Consumed(key, payload, messageType));
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
        private final UserSiteEventAbstract value;
        private final String messageType;
    }
}
