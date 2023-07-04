package nl.ing.lovebird.sitemanagement.orphanuser;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@IntegrationTestContext
public class OrphanUserBatchRepositoryIntegrationTest {

    @Autowired
    private OrphanUserBatchRepository repository;

    @Autowired
    private Session session;

    @AfterEach
    void tearDown() {
        CassandraHelper.truncate(session, OrphanUserBatch.class);
    }

    @Test
    void testOrphanUserBatchRepository() {
        // 0. Init data
        ClientId clientId = ClientId.random();
        String provider = "YODLEE";
        UUID batchId = UUID.randomUUID();
        OrphanUserBatch.Status status = OrphanUserBatch.Status.PREPARE_INITIATED;
        OrphanUserBatch batch = new OrphanUserBatch(clientId, provider, batchId, Instant.now(systemUTC()).truncatedTo(ChronoUnit.MILLIS), Instant.now(systemUTC()).truncatedTo(ChronoUnit.MILLIS), status);

        // 1. Check that data is not present in repository
        List<OrphanUserBatch> orphanUserBatches = repository.list(clientId, provider);
        assertThat(orphanUserBatches).isEmpty();

        // 2. Update status on absent entry should not write this entry to db
        try {
            repository.updateStatus(clientId, provider, batchId, status);
            fail("Should throw OrphanUserBatchNotFoundException");
        } catch (RuntimeException e) {
            assertThat(e).isInstanceOf(OrphanUserBatchNotFoundException.class);
        }
        orphanUserBatches = repository.list(clientId, provider);
        assertThat(orphanUserBatches).isEmpty();

        // 3. Save data and check that it was saved successfully
        repository.save(batch);
        Optional<OrphanUserBatch> orphanUserBatch = repository.get(clientId, provider, batchId);
        assertThat(orphanUserBatch).isPresent();
        assertThat(orphanUserBatch.get()).isEqualTo(batch);

        // 4. Update status and check that is was applied
        repository.updateStatus(clientId, provider, batchId, OrphanUserBatch.Status.EXECUTE_FINISHED_SUCCESS);
        orphanUserBatch = repository.get(clientId, provider, batchId);
        assertThat(orphanUserBatch).isPresent();
        assertThat(orphanUserBatch.get().getStatus()).isEqualTo(OrphanUserBatch.Status.EXECUTE_FINISHED_SUCCESS);

        // 5. Delete data and check that it is not present anymore
        repository.delete(clientId, provider, batchId);
        orphanUserBatches = repository.list(clientId, provider);
        assertThat(orphanUserBatches).isEmpty();
    }
}
