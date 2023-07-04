package nl.ing.lovebird.sitemanagement.providerclient;

import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.logging.test.CaptureLogEvents;
import nl.ing.lovebird.logging.test.LogEvents;
import nl.ing.lovebird.providershared.ProviderServiceResponseDTO;
import nl.ing.lovebird.providershared.ProviderServiceResponseStatus;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.TestKafkaErrorConsumer;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequest;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequestRepository;
import nl.ing.lovebird.sitemanagement.providerresponse.ProviderServiceResponseConsumer;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.FailureReason;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.awaitility.Durations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@IntegrationTestContext
@CaptureLogEvents
class ProviderServiceResponseDTOConsumerTest {

    @Autowired
    private KafkaTemplate<String, ProviderServiceResponseDTO> providerServiceResponseKafkaTemplate;

    @Autowired
    private ProviderRequestRepository providerRequestRepository;

    @Autowired
    private UserSiteService userSiteService;

    @Autowired
    private PostgresUserSiteRepository userSiteRepository;


    @Value("${yolt.kafka.topics.providerAccounts.topic-name}")
    private String topic;

    @Autowired
    private TestKafkaErrorConsumer testKafkaErrorConsumer;

    @Autowired
    private TestClientTokens testClientTokens;

    @BeforeEach
    void setUp() {
        testKafkaErrorConsumer.getConsumed().clear();
    }


    @Test
    void testConsumeProviderAccountsMessage() {
        UUID userId = UUID.randomUUID();
        UUID userSiteId = UUID.randomUUID();
        UUID providerRequestId = UUID.randomUUID();
        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(UUID.randomUUID(), UUID.randomUUID(), userId);

        ProviderServiceResponseDTO providerServiceResponseDTO = new ProviderServiceResponseDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), ProviderServiceResponseStatus.TOKEN_INVALID, providerRequestId);

        ProviderRequest mockedProviderRequest = new ProviderRequest(providerRequestId, UUID.randomUUID(), userId, userSiteId, UserSiteActionType.USER_REFRESH);
        providerRequestRepository.saveValidated(mockedProviderRequest);
        userSiteRepository.save(new PostgresUserSite(userId, userSiteId, SiteService.ID_YOLTBANK_YOLT_PROVIDER, null, ConnectionStatus.DISCONNECTED, null, null, new Date().toInstant(), null, null, ClientId.random(), "YODLEE", null, null, null, false, null));

        Message<ProviderServiceResponseDTO> message = MessageBuilder
                .withPayload(providerServiceResponseDTO)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, userId.toString())
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .build();

        ListenableFuture<SendResult<String, ProviderServiceResponseDTO>> future = providerServiceResponseKafkaTemplate.send(message);
        future.addCallback(
                result -> log.debug("Sent provider service response."),
                ex -> log.error("Failed to send provider service response.", ex)
        );

        await().atMost(Durations.FIVE_SECONDS).until(() -> {
            PostgresUserSite userSite = userSiteService.getUserSite(userId, userSiteId);
            log.info("usersite {}", userSite);
            return userSite.getConnectionStatus().equals(ConnectionStatus.DISCONNECTED) && FailureReason.CONSENT_EXPIRED.equals(userSite.getFailureReason());
        });
    }

    @Test
    void testAMissingHeaderParameter(LogEvents events) {

        UUID userId = UUID.randomUUID();
        UUID providerRequestId = UUID.randomUUID();
        ProviderServiceResponseDTO providerServiceResponseDTO = new ProviderServiceResponseDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), ProviderServiceResponseStatus.TOKEN_INVALID, providerRequestId);

        Message<ProviderServiceResponseDTO> message = MessageBuilder
                .withPayload(providerServiceResponseDTO)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, userId.toString())
                .build();
        providerServiceResponseKafkaTemplate.send(message);

        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            assertLogMessageDoesContain(events, KafkaErrorHandlingService.class, "Could not invoke @KafkaListener for topic providerAccounts. See exception on errortopic. It can't be logged due to sensitive information. Search for correlation id ");
            assertLogMessageDoesNotContain(events, userId.toString());
            assertLogMessageDoesNotContain(events, providerRequestId.toString());
            assertThat(testKafkaErrorConsumer.getConsumed().isEmpty()).isFalse();

            assertThat(testKafkaErrorConsumer.getConsumed().isEmpty()).isFalse();
        });

        /**
         * It might be nice to actually verify that the kafkaClient publishes to the errortopic. This is currently not possible due to:
         * - Multiple application contexts are started, so multiple '@Listeners' are attached to the same static embedded kafka
         * - The @MockBean KafkaClient (wired in KafkaProducerService) that you will get if you Autowire it in this test, will be a
         *   reference of a mockbean on the current application context. In practice, any other consumer from another cached application-
         *   context might steal the published message, because they are in the same consumergroup. Thus, your @MockBean won't be called,
         *   but another one that lives on some other cached applicationcontext.
         *   We either:
         *   - shouldn't reload applicationContext in integrationtests.
         *       requires quite some refactoring
         *   - or attach this producer and consumer both to a seperate kafka server so message don't get hijacked.
         *       is hard because the loaded '@Listeners' should be reloaded so they listen to this particular embeddedKafka. Now on
         *       startup of TestConfiguration, they will point to KAFKA_EMBEDDED
         */


    }

    @Test
    void testAnUnparseableHeader(LogEvents events) {

        UUID userId = UUID.randomUUID();
        UUID providerRequestId = UUID.randomUUID();
        ProviderServiceResponseDTO providerServiceResponseDTO = new ProviderServiceResponseDTO(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), ProviderServiceResponseStatus.TOKEN_INVALID, providerRequestId);
        String clientTokenHeader = "something that cant be parsed";

        Message<ProviderServiceResponseDTO> message = MessageBuilder
                .withPayload(providerServiceResponseDTO)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, userId.toString())
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientTokenHeader)
                .build();
        providerServiceResponseKafkaTemplate.send(message);

        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            assertLogMessageDoesContain(events, KafkaErrorHandlingService.class,
                    "Could not invoke @KafkaListener for topic providerAccounts. " +
                    "See exception on errortopic. It can't be logged due to sensitive information. " +
                    "Search for correlation id");
            assertLogMessageDoesNotContain(events, providerRequestId.toString());
            assertLogMessageDoesNotContain(events, userId.toString());
            assertThat(testKafkaErrorConsumer.getConsumed().isEmpty()).isFalse();
        });
    }

    @Test
    void testAnUnparseablePayload(LogEvents events) {
        String messagePayload = "something that cant be parsed";
        UUID userId = UUID.randomUUID();
        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(UUID.randomUUID(), UUID.randomUUID(), userId);

        Message<String> message = MessageBuilder
                .withPayload(messagePayload)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, userId.toString())
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .build();
        providerServiceResponseKafkaTemplate.send(message);

        await().atMost(Durations.TEN_SECONDS).untilAsserted(() -> {
            assertLogMessageDoesContain(events, ProviderServiceResponseConsumer.class,
                    "Unexpected exception reading provider accounts. " +
                    "The exception cannot be logged because it might contain sensitive data. " +
                    "Check the errortopic for more information. Correlation Id: ");
            assertLogMessageDoesNotContain(events, messagePayload);
            assertLogMessageDoesNotContain(events, userId.toString());
            assertThat(testKafkaErrorConsumer.getConsumed().isEmpty()).isFalse();
        });
    }

    private void assertLogMessageDoesNotContain(LogEvents events, String messagePart) {
        assertThat(events.stream())
                .extracting(ILoggingEvent::getFormattedMessage)
                .filteredOn(message -> message.contains(messagePart))
                .isEmpty();
    }

    private void assertLogMessageDoesContain(LogEvents events, Class<?> clazz, String messagePart) {
        assertThat(events.stream(clazz))
                .extracting(ILoggingEvent::getFormattedMessage)
                .filteredOn(message -> message.contains(messagePart))
                .isNotEmpty();
    }
}
