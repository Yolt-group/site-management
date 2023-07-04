package nl.ing.lovebird.sitemanagement.orphanuser;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTestContext
public class ExternalUserIdsConsumerIntegrationTest {

    private static final ClientId CLIENT_ID = ClientId.random();
    private static final String PROVIDER = "YODLEE";
    private static final UUID BATCH_ID = UUID.randomUUID();

    @Autowired
    private KafkaTemplate<String, ProviderExternalUserIds> kafkaTemplate;

    @Autowired
    private Session session;

    @Autowired
    private OrphanUserBatchRepository orphanUserBatchRepository;

    @Value("${yolt.kafka.topics.formProviderExternalUserIds.topic-name}")
    private String topic;


    @AfterEach
    void tearDown() {
        CassandraHelper.truncate(session, OrphanUserBatch.class);
    }

    @Test
    void whenNotLastDataChunk_shouldSetProperStatus() throws Exception {
        // Saving initial batch entry
        OrphanUserBatch orphanUserBatch = new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_INITIATED);
        orphanUserBatchRepository.save(orphanUserBatch);

        // Check that save was successful
        await().atMost(Durations.TEN_SECONDS)
                .untilAsserted(() -> assertThat(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).isPresent());

        // Prepare and produce external ids chunk
        ProviderExternalUserIds externalUserIds = new ProviderExternalUserIds(CLIENT_ID, BATCH_ID, PROVIDER, Collections.singletonList("123"), false);
        Message<ProviderExternalUserIds> message = MessageBuilder
                .withPayload(externalUserIds)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, BATCH_ID.toString())
                .build();
        kafkaTemplate.send(message).get(10, TimeUnit.SECONDS);

        // Check that status was updated to proper one
        await().atMost(Durations.TEN_SECONDS)
                .untilAsserted(() -> assertThat(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID).get())
                        .isEqualToComparingOnlyGivenFields(new OrphanUserBatch(
                                CLIENT_ID, PROVIDER, BATCH_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_RECEIVING_DATA
                        ), "provider", "orphanUserBatchId", "status"));
    }
}
