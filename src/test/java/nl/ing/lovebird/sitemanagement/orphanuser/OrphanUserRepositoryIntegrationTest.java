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
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class OrphanUserRepositoryIntegrationTest {

    @Autowired
    private OrphanUserRepository repository;

    @Autowired
    private Session session;

    @AfterEach
    void tearDown() {
        CassandraHelper.truncate(session, OrphanUser.class);
    }

    @Test
    void testOrphanUserRepository() {
        // 0. Init data
        ClientId clientId = ClientId.random();
        String provider = "YODLEE";
        UUID batchId = UUID.randomUUID();
        String externalId = "externalId";
        OrphanUser.Status status = OrphanUser.Status.INITIAL;
        OrphanUser orphanUser = new OrphanUser(clientId, provider, batchId, externalId, Instant.now(systemUTC()).truncatedTo(ChronoUnit.MILLIS), Instant.now(systemUTC()).truncatedTo(ChronoUnit.MILLIS), status);

        // 1. Check that data is not present in repository
        List<OrphanUser> orphanUsers = repository.listOrphanUsers(clientId, provider, batchId, 100);
        assertThat(orphanUsers).isEmpty();

        // 2. Save data and check that it was saved successfully
        repository.save(orphanUser);
        orphanUsers = repository.listOrphanUsers(clientId, provider, batchId, 100);
        assertThat(orphanUsers).containsExactlyInAnyOrder(orphanUser);

        // 3. Update status and check that is was applied
        repository.updateStatus(clientId, provider, batchId, externalId, OrphanUser.Status.DELETED);
        orphanUsers = repository.listOrphanUsers(clientId, provider, batchId, 100);
        assertThat(orphanUsers).hasSize(1);
        assertThat(orphanUsers.get(0).getStatus()).isEqualTo(OrphanUser.Status.DELETED);

        // 4. Delete data and check that it is not present anymore
        repository.delete(clientId, provider, batchId);
        orphanUsers = repository.listOrphanUsers(clientId, provider, batchId, 100);
        assertThat(orphanUsers).isEmpty();
    }
}
