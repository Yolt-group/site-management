package nl.ing.lovebird.sitemanagement.usersite;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class SiteLoginFormRepositoryIntegrationTest {

    @Autowired
    private Session session;

    private SiteLoginFormRepository siteLoginFormRepository;

    @BeforeEach
    void setUp() {
        siteLoginFormRepository = new SiteLoginFormRepository(session);
    }

    @Test
    void testSelectSiteLogin() {
        final UUID barclaysSiteId = UUID.fromString("e278a008-bf45-4d19-bb5d-b36ff755be58");

        final SiteLoginForm siteLoginForm = siteLoginFormRepository.selectSiteLogin(barclaysSiteId);

        assertThat(siteLoginForm).isNotNull();
    }
}
