package nl.ing.lovebird.sitemanagement.health.orchestration;

import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.health.dspipeline.TestHealthOrchestrationEventsConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Collections.singletonList;
import static nl.ing.lovebird.sitemanagement.health.orchestration.HealthOrchestrationEventOrigin.CreateUserSite;
import static nl.ing.lovebird.sitemanagement.health.orchestration.HealthOrchestrationEventType.RefreshFinished;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTestContext
public class HealthOrchestrationProducerIntegrationTest {

    @Autowired
    private HealthOrchestrationProducer producer;

    @Autowired
    private TestHealthOrchestrationEventsConsumer testHealthOrchestrationEventsConsumer;

    @Autowired
    private TestClientTokens testClientTokens;

    @BeforeEach
    public void setUp() {
        testHealthOrchestrationEventsConsumer.reset();
    }

    @Test
    public void testRefreshFinished() throws InterruptedException, ExecutionException, TimeoutException {
        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        final UUID activityId = UUID.randomUUID();
        producer.publishRefreshFinishedEvent(clientUserToken, CreateUserSite, activityId, singletonList(new UUID(0, 0))).get(10L, TimeUnit.SECONDS);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<TestHealthOrchestrationEventsConsumer.Consumed> records = testHealthOrchestrationEventsConsumer.getConsumed();
            assertThat(records).isNotEmpty();
            TestHealthOrchestrationEventsConsumer.Consumed record = records.get(0);
            assertThat(record.getKey()).isEqualTo(clientUserToken.getUserIdClaim().toString());
            assertThat(record.getClientUserToken()).isNotNull();
            assertThat(record.getValue()).isEqualToComparingFieldByField(HealthOrchestrationEvent.builder()
                    .origin(CreateUserSite)
                    .correlationId(activityId)
                    .type(RefreshFinished)
                    .userSiteIds(singletonList(new UUID(0, 0)))
                    .build());
        });
    }
}
