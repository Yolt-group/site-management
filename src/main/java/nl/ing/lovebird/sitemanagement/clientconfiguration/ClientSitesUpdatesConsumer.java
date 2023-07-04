package nl.ing.lovebird.sitemanagement.clientconfiguration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
class ClientSitesUpdatesConsumer {

    private final ClientSitesProvider clientSitesProvider;

    @KafkaListener(
            topics = "${yolt.kafka.topics.client-sites-updates.topic-name}",
            concurrency = "${yolt.kafka.topics.client-sites-updates.listener-concurrency}",
            groupId = "#{T(java.util.UUID).randomUUID().toString()}"
    )
    void updateSignal(@Payload final Object event) {

        try {
            log.info("got signal on client sites updates topic. Refreshing client sites.");
            clientSitesProvider.update();
        } catch (Exception e) {
            log.error("Unexpected exception updating client sites provider: {}", e.getMessage(), e);
        }
    }
}
