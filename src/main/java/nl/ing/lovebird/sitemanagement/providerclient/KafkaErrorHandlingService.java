package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import nl.ing.lovebird.logging.MDCContextCreator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Marker;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class KafkaErrorHandlingService implements ErrorHandler {

    private final KafkaProducerService kafkaProducerService;

    @Override
    public void handle(Exception thrownException, ConsumerRecord<?, ?> data) {

        final ConsumerRecord<String, byte[]> record = (ConsumerRecord<String, byte[]>) data;

        final UUID messageKey = UUID.randomUUID();
        final String messageValue;
        if (record != null) {
            final String message = "Could not invoke @KafkaListener for topic {}. See exception on errortopic. It can't be logged due to sensitive information. " +
                    "Search for correlation id {}";
            try {
                // Add user_id to the log entry. This works only for new Kafka key format that contains only userId.
                Marker marker = Markers.append(MDCContextCreator.USER_ID_HEADER_NAME, UUID.fromString(record.key()));
                log.error(marker, message, record.topic(), messageKey);
            } catch (Exception e) { // NOSONAR
                log.error(message, record.topic(), messageKey);
            }

            messageValue = new String(record.value(), StandardCharsets.UTF_8);
        } else {
            log.error("Exception thrown without any record data. Search for correlation id {}", messageKey);
            messageValue = "UNKNOWN";
        }

        String theMessage = String.format("Kafka failure. Exception: %s, Data %s: ", thrownException.toString(), messageValue);
        kafkaProducerService.publishDataError(theMessage, messageKey);
    }
}
