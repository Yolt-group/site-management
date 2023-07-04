package nl.ing.lovebird.sitemanagement.accessmeans;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class AccessMeansRepositoryIntegrationTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PROVIDER = "SALTEDGE";

    @Autowired
    private Session session;

    @Autowired
    private AccessMeansRepository accessMeansRepository;

    @AfterEach
    void tearDown() {
        CassandraHelper.truncate(session, AccessMeans.class);
    }

    @Test
    void testAccessMeans() {
        // 1. Check that table is empty
        Optional<AccessMeans> accessMeansOptional = accessMeansRepository.get(USER_ID, PROVIDER);
        assertThat(accessMeansOptional).isNotPresent();

        // 2. Save access means and check that they can be retrieved
        AccessMeans accessMeans = new AccessMeans(USER_ID, PROVIDER, "am1", new Date(), new Date());
        accessMeansRepository.save(accessMeans);
        accessMeansOptional = accessMeansRepository.get(USER_ID, PROVIDER);
        assertThat(accessMeansOptional).isPresent();
        assertThat(accessMeansOptional.get()).isEqualToComparingOnlyGivenFields(accessMeans,
                "userId", "provider", "accessMeans");

        // 3. Delete access means and check that they are not present anymore
        accessMeansRepository.delete(USER_ID, PROVIDER);
        accessMeansOptional = accessMeansRepository.get(USER_ID, PROVIDER);
        assertThat(accessMeansOptional).isNotPresent();
    }
}
