package nl.ing.lovebird.sitemanagement.clientconfiguration;

import com.github.tomakehurst.wiremock.client.WireMock;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests;
import static org.awaitility.Awaitility.await;

@IntegrationTestContext
public class ClientSitesUpdatesConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void when_anyMessageIsReceived_theClientSitesShouldBeRetrieved() {

        WireMock.removeServeEvents(allRequests());

        Message<String> message = MessageBuilder
                .withPayload("some signal")
                .setHeader(KafkaHeaders.TOPIC, "ycs_clientSitesUpdates")
                .setHeader(KafkaHeaders.MESSAGE_KEY, "something")
                .build();


        // when
        kafkaTemplate.send(message);
        kafkaTemplate.send(message);
        kafkaTemplate.send(message);

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                verify(exactly(3), getRequestedFor(urlEqualTo("/clients/internal/v2/sites-per-client")))
        );

    }

}
