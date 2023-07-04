package nl.ing.lovebird.sitemanagement.health.activities;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.health.webhook.dto.WebhookEventEnvelope;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class WebhookEventsTestConsumer {
    private final List<WebhookEventEnvelope> consumed = new ArrayList<>();

    @KafkaListener(topics = "${yolt.kafka.topics.webhooks.topic-name}")
    public void receiveWebhookEvents(@Payload final WebhookEventEnvelope event) {
        log.info("Got webhook event {}", event.webhookEventType);
        consumed.add(event);
    }

    public List<WebhookEventEnvelope> getConsumed() {
        return consumed;
    }
}
