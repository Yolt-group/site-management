package nl.ing.lovebird.sitemanagement.orphanuser;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class OrphanCassaCassaUserExternalIdRepositoryIntegrationTest {

    @Autowired
    private OrphanUserExternalIdRepository repository;

    @Autowired
    private Session session;

    @AfterEach
    void tearDown() {
        CassandraHelper.truncate(session, OrphanUserExternalId.class);
    }

    @Test
    void testOrphanUserExternalIdRepository() {
        // 0. Init data
        ClientId clientId = ClientId.random();
        String provider = "YODLEE";
        UUID batchId = UUID.randomUUID();
        String externalId = "externalId";
        OrphanUserExternalId orphanUserExternalId = new OrphanUserExternalId(clientId, provider, batchId, externalId);

        // 1. Check that data is not present in repository
        List<OrphanUserExternalId> externalIds = repository.getForBatchAndProvider(clientId, provider, batchId, 100);
        assertThat(externalIds).isEmpty();

        // 2. Save data and check that it was saved successfully
        repository.saveBatch(Collections.singletonList(orphanUserExternalId), 1);
        externalIds = repository.getForBatchAndProvider(clientId, provider, batchId, 100);
        assertThat(externalIds).containsExactlyInAnyOrder(orphanUserExternalId);

        // 3. Delete data and check that it is not present anymore
        repository.delete(clientId, provider, batchId);
        externalIds = repository.getForBatchAndProvider(clientId, provider, batchId, 100);
        assertThat(externalIds).isEmpty();
    }
}
