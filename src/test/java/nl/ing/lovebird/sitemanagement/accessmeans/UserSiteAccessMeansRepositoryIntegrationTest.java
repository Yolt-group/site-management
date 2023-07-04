package nl.ing.lovebird.sitemanagement.accessmeans;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class UserSiteAccessMeansRepositoryIntegrationTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID USER_SITE_ID = UUID.randomUUID();
    private static final String PROVIDER = "SALTEDGE";

    @Autowired
    private Session session;

    @Autowired
    private UserSiteAccessMeansRepository userSiteAccessMeansRepository;

    @AfterEach
    void tearDown() {
        CassandraHelper.truncate(session, UserSiteAccessMeans.class);
    }

    @Test
    void testAccessMeans() {
        // Check that table is empty
        Optional<UserSiteAccessMeans> userSiteAccessMeansOptional = userSiteAccessMeansRepository.get(USER_ID, USER_SITE_ID, PROVIDER);
        assertThat(userSiteAccessMeansOptional).isNotPresent();

        // Save access means and check that they can be retrieved
        UserSiteAccessMeans userSiteAccessMeans = new UserSiteAccessMeans(USER_ID, USER_SITE_ID, PROVIDER, "am1", new Date(), new Date(), Instant.EPOCH);
        userSiteAccessMeansRepository.save(userSiteAccessMeans);
        userSiteAccessMeansOptional = userSiteAccessMeansRepository.get(USER_ID, USER_SITE_ID, PROVIDER);
        assertThat(userSiteAccessMeansOptional).isPresent();
        assertThat(userSiteAccessMeansOptional.get()).isEqualToComparingOnlyGivenFields(userSiteAccessMeans,
                "userId", "userSiteId", "provider", "accessMeans");

        // Delete access means and check that they are not present anymore
        userSiteAccessMeansRepository.delete(USER_ID, USER_SITE_ID, PROVIDER);
        userSiteAccessMeansOptional = userSiteAccessMeansRepository.get(USER_ID, USER_SITE_ID, PROVIDER);
        assertThat(userSiteAccessMeansOptional).isNotPresent();
    }
}
