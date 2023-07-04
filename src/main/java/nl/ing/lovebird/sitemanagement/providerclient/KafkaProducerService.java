package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> errorKafkaTemplate;
    private final String errorTopic;

    public KafkaProducerService(
            KafkaTemplate<String, String> errorKafkaTemplate,
            @Value("${yolt.kafka.topics.requests-errors.topic-name}")
                    String errorTopic) {
        this.errorTopic = errorTopic;
        this.errorKafkaTemplate = errorKafkaTemplate;
    }

    public void publishDataError(final String message, final UUID messageKey) {

        Message<String> build = MessageBuilder.withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, errorTopic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, messageKey.toString())
                .build();
        errorKafkaTemplate.send(build);
    }

}
