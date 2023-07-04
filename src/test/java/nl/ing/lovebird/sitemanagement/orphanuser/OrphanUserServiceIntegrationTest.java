package nl.ing.lovebird.sitemanagement.orphanuser;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.providercallback.UserExternalId;
import nl.ing.lovebird.sitemanagement.providercallback.UserExternalIdRepository;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.time.Clock.systemUTC;
import static nl.ing.lovebird.sitemanagement.orphanuser.OrphanUserBatch.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@IntegrationTestContext
public class OrphanUserServiceIntegrationTest {

    private static final ClientId CLIENT_ID = ClientId.random();
    private static final String PROVIDER = "YODLEE";
    private static final String EXTERNAL_ID_1 = "123";
    private static final String EXTERNAL_ID_2 = "456";
    private static final UUID USER_ID_1 = UUID.randomUUID();
    private static final UUID USER_ID_2 = UUID.randomUUID();

    @Autowired
    private KafkaTemplate<String, ProviderExternalUserIds> kafkaTemplate;

    @Autowired
    private OrphanUserService orphanUserService;

    @Autowired
    private OrphanUserBatchRepository orphanUserBatchRepository;

    @Autowired
    private OrphanUserExternalIdRepository orphanUserExternalIdRepository;

    @Autowired
    private FormProviderRestClient formProviderRestClient;

    @Value("${yolt.kafka.topics.formProviderExternalUserIds.topic-name}")
    private String topic;

    @Autowired
    private UserExternalIdRepository userExternalIdRepository;

    @Test
    void testBatchPrepareAndExecuteHappyFlow() throws Exception {
        ClientToken clientToken = mock(ClientToken.class);
        when(clientToken.getClientIdClaim())
                .thenReturn(CLIENT_ID.unwrap());
        when(clientToken.getSerialized())
                .thenReturn("mocked-client-token-value");

        // 0. Prepare fake orphaned users situation
        userExternalIdRepository.save(new UserExternalId(USER_ID_1, PROVIDER, EXTERNAL_ID_1));
        userExternalIdRepository.save(new UserExternalId(USER_ID_2, PROVIDER, EXTERNAL_ID_2));

        when(formProviderRestClient.fetchProviderExternalUserIds(PROVIDER, clientToken)).thenReturn(UUID.randomUUID());

        // 1. Start preparing batch
        UUID batchId = orphanUserService.startPreparingBatch(clientToken, PROVIDER);
        await().atMost(Durations.ONE_MINUTE).untilAsserted(() ->
                assertThat(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, batchId).get()).isEqualToComparingOnlyGivenFields(
                        new OrphanUserBatch(CLIENT_ID, PROVIDER, batchId, Instant.now(systemUTC()), Instant.now(systemUTC()), PREPARE_INITIATED),
                        "provider", "orphanUserBatchId", "status" ));

        // 2. Prepare and produce external ids chunks
        kafkaTemplate.send(externalIdsMessage(batchId, EXTERNAL_ID_1, false)).get(10, TimeUnit.SECONDS);
        await().atMost(Durations.ONE_MINUTE).untilAsserted(() ->
                assertThat(orphanUserExternalIdRepository.getForBatchAndProvider(CLIENT_ID, PROVIDER, batchId, 100)).hasSize(1));

        kafkaTemplate.send(externalIdsMessage(batchId, EXTERNAL_ID_2, true)).get(10, TimeUnit.SECONDS);
        await().atMost(Durations.ONE_MINUTE).untilAsserted(() ->
                assertThat(orphanUserExternalIdRepository.getForBatchAndProvider(CLIENT_ID, PROVIDER, batchId, 100)).hasSize(2));

        await().atMost(Durations.ONE_MINUTE).untilAsserted(() ->
                assertThat(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, batchId).get()).isEqualToComparingOnlyGivenFields(
                        new OrphanUserBatch(CLIENT_ID, PROVIDER, batchId, Instant.now(systemUTC()), Instant.now(systemUTC()), PREPARE_PROCESSING_FINISHED),
                        "provider", "orphanUserBatchId", "status" ));

        // 3. Execute batch
        orphanUserService.executeOrphanUserBatch(clientToken, PROVIDER, batchId);
        await().atMost(Durations.ONE_MINUTE).untilAsserted(() ->
                assertThat(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, batchId).get()).isEqualToComparingOnlyGivenFields(
                        new OrphanUserBatch(CLIENT_ID, PROVIDER, batchId, Instant.now(systemUTC()), Instant.now(systemUTC()), EXECUTE_FINISHED_SUCCESS),
                        "provider", "orphanUserBatchId", "status" ));

        // 4. Check fake external users were deleted successfully
        List<String> statuses = orphanUserService.listOrphanUsers(clientToken, PROVIDER, batchId).stream()
                .map(OrphanUserDTO::getStatus)
                .collect(Collectors.toList());
        assertThat(statuses).containsExactlyInAnyOrder(OrphanUser.Status.DELETED.name(), OrphanUser.Status.DELETED.name());

        // 5. Remove data after batch run
        orphanUserService.deleteBatchData(clientToken, PROVIDER, batchId);
        assertThat(orphanUserService.listOrphanUserBatches(clientToken, PROVIDER)).isEmpty();
        assertThat(orphanUserService.listOrphanUsers(clientToken, PROVIDER, batchId)).isEmpty();
    }

    private Message<ProviderExternalUserIds> externalIdsMessage(UUID batchId, String externalId, boolean isLast) {
        ProviderExternalUserIds externalUserIds = new ProviderExternalUserIds(CLIENT_ID, batchId, PROVIDER, Collections.singletonList(externalId), isLast);
        return MessageBuilder.withPayload(externalUserIds)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, batchId.toString())
                .build();
    }

}
