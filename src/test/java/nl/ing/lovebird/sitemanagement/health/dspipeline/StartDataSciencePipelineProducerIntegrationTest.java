package nl.ing.lovebird.sitemanagement.health.dspipeline;

import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTestContext
public class StartDataSciencePipelineProducerIntegrationTest {

    private static final UUID TRACE_ID = new UUID(0, 0);

    @Autowired
    private StartDatasciencePipelineProducer producer;

    @Autowired
    private TestDataScienceEventsConsumer testDataScienceEventsConsumer;

    @Autowired
    private TestClientTokens testClientTokens;

    @BeforeEach
    public void setUp() {
        testDataScienceEventsConsumer.reset();
    }

    @Test
    public void producerShouldSendMessage() throws InterruptedException, ExecutionException, TimeoutException {
        var userId = randomUUID();
        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(UUID.randomUUID(), UUID.randomUUID(), userId);
        DatasciencePipelineValue value = new DatasciencePipelineValue(
                new Headers("messageType", userId, TRACE_ID),
                new DatasciencePipelinePayload(randomUUID(), null, new UserContext("EUR", "NL", Collections.emptyList(), clientUserToken.getClientIdClaim(), clientUserToken.getUserIdClaim()), Collections.emptyList())
        );

        producer.sendRefreshTriggeredEvent(clientUserToken, value).get(10L, TimeUnit.SECONDS);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<TestDataScienceEventsConsumer.Consumed> records = testDataScienceEventsConsumer.getConsumed();
            assertThat(records).isNotEmpty();
            TestDataScienceEventsConsumer.Consumed record = records.get(0);
            assertThat(record.getKey()).isEqualTo(userId.toString());
            assertThat(record.getValue()).isEqualToComparingFieldByField(value);
        });
    }
}
