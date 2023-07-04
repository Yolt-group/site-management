package nl.ing.lovebird.sitemanagement.clientconfiguration;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.TestUtil;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static org.awaitility.Awaitility.await;

@IntegrationTestContext
public class ClientRedirectUrlsConsumerIntegrationTest {

    public static final String CLIENT_REDIRECT_URLS_TOPIC = "clientRedirectUrls";

    @Autowired
    private ClientRedirectUrlRepository clientRedirectUrlRepository;

    @Autowired
    private Session session;

    private CassandraHelper.OpenRepository<ClientRedirectUrl> openRepository;

    @Autowired
    private KafkaTemplate<String, ClientRedirectUrlDTO> kafkaTemplate;

    @Autowired
    private TestClientTokens testClientTokens;

    @BeforeEach
    public void beforeEach() {
        openRepository = CassandraHelper.openRepository(session, ClientRedirectUrl.class);
    }

    @Test
    public void shouldCreateClientRedirectUrlBasedOnKafkaMessageForSendWithClientRedirectUrlCreatedMessageType() {
        // given
        ClientId clientId = TestUtil.YOLT_APP_CLIENT_ID;
        UUID clientRedirectUrl1 = UUID.randomUUID();
        UUID clientRedirectUrl2 = UUID.randomUUID();
        Message<ClientRedirectUrlDTO> message1 = createTestMessageCreateClientRedirectUrl(clientId, clientRedirectUrl1);
        Message<ClientRedirectUrlDTO> message2 = createTestMessageCreateClientRedirectUrl(clientId, clientRedirectUrl2);

        // when
        kafkaTemplate.send(message1);
        kafkaTemplate.send(message2);

        // then
        await().atMost(Duration.ofSeconds(5)).until(() ->
                clientRedirectUrlRepository.get(clientId, clientRedirectUrl1).isPresent() &&
                        clientRedirectUrlRepository.get(clientId, clientRedirectUrl2).isPresent()
        );

    }

    @Test
    public void shouldCreateClientRedirectUrlBasedOnKafkaMessageForSendWithClientRedirectUrlUpdatedMessageType() {
        // given
        ClientId clientId = TestUtil.YOLT_APP_CLIENT_ID;
        UUID clientRedirectUrl1 = UUID.randomUUID();
        Message<ClientRedirectUrlDTO> message1 = createTestMessageCreateClientRedirectUrl(clientId, clientRedirectUrl1);
        Message<ClientRedirectUrlDTO> message2 = createTestMessageUpdateClientRedirectUrl(clientId, clientRedirectUrl1);

        // when
        kafkaTemplate.send(message1);
        kafkaTemplate.send(message2);

        // then
        await().atMost(Duration.ofSeconds(5)).until(() ->
                clientRedirectUrlRepository.get(clientId, clientRedirectUrl1).isPresent() &&
                        clientRedirectUrlRepository.get(clientId, clientRedirectUrl1).get().url.equals("updated")
        );

    }

    @Test
    public void shouldDeleteClientRedirectUrlBasedOnKafkaMessageForSendWithClientRedirectUrlDeletedMessageType() {
        // given
        ClientId clientId = TestUtil.YOLT_APP_CLIENT_ID;
        UUID clientRedirectUrlId = UUID.randomUUID();
        openRepository.save(new ClientRedirectUrl(clientId, clientRedirectUrlId, "somestring", Instant.now()));
        Message<ClientRedirectUrlDTO> message = createTestMessageDeleteClientRedirectUrl(clientId, clientRedirectUrlId);

        // when
        kafkaTemplate.send(message);

        // then
        await().atMost(Duration.ofSeconds(5)).until(() ->
                thereAreNoMoreStoredRecordsFor(clientId.unwrap(), clientRedirectUrlId)
        );
    }

    private boolean thereAreNoMoreStoredRecordsFor(UUID clientId, UUID clientRedirectUrlId) {
        Select select = QueryBuilder.select().from(ClientRedirectUrl.TABLE_NAME);
        select.where(eq(ClientRedirectUrl.CLIENT_ID_COLUMN, clientId));
        select.where(eq(ClientRedirectUrl.REDIRECT_URL_ID_COLUMN, clientRedirectUrlId));
        return openRepository.select(select).isEmpty();
    }

    private Message<ClientRedirectUrlDTO> createTestMessageDeleteClientRedirectUrl(final ClientId clientId, UUID clientRedirectUrlId) {
        ClientToken clientToken = testClientTokens.createClientToken(UUID.randomUUID(), clientId.unwrap());
        return MessageBuilder
                .withPayload(new ClientRedirectUrlDTO(
                        clientId,
                        clientRedirectUrlId,
                        "some super beautiful url"))
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.TOPIC, CLIENT_REDIRECT_URLS_TOPIC)
                .setHeader(KafkaHeaders.MESSAGE_KEY, "mykey")
                .setHeader("message_type", ClientRedirectUrlMessageType.CLIENT_REDIRECT_URL_DELETED.name())
                .build();
    }

    private Message<ClientRedirectUrlDTO> createTestMessageCreateClientRedirectUrl(ClientId clientId, UUID clientRedirectUrlId) {
        ClientToken clientToken = testClientTokens.createClientToken(UUID.randomUUID(), clientId.unwrap());
        return MessageBuilder
                .withPayload(new ClientRedirectUrlDTO(
                        clientId,
                        clientRedirectUrlId,
                        "some super beautiful url"))
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.TOPIC, CLIENT_REDIRECT_URLS_TOPIC)
                .setHeader(KafkaHeaders.MESSAGE_KEY, "mykey")
                .setHeader("message_type", ClientRedirectUrlMessageType.CLIENT_REDIRECT_URL_CREATED.name())
                .build();
    }

    private Message<ClientRedirectUrlDTO> createTestMessageUpdateClientRedirectUrl(ClientId clientId, UUID clientRedirectUrlId) {
        ClientToken clientToken = testClientTokens.createClientToken(UUID.randomUUID(), clientId.unwrap());
        return MessageBuilder
                .withPayload(new ClientRedirectUrlDTO(
                        clientId,
                        clientRedirectUrlId,
                        "updated"))
                .setHeader(CLIENT_TOKEN_HEADER_NAME, clientToken.getSerialized())
                .setHeader(KafkaHeaders.TOPIC, CLIENT_REDIRECT_URLS_TOPIC)
                .setHeader(KafkaHeaders.MESSAGE_KEY, "mykey")
                .setHeader("message_type", ClientRedirectUrlMessageType.CLIENT_REDIRECT_URL_UPDATED.name())
                .build();
    }

}
