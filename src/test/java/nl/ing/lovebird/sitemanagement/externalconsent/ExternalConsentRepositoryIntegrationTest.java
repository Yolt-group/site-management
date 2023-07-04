package nl.ing.lovebird.sitemanagement.externalconsent;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@IntegrationTestContext
public class ExternalConsentRepositoryIntegrationTest {

    @Autowired
    private Session session;

    @Autowired
    private ExternalConsentRepository repository;

    @AfterEach
    void tearDown() {
        CassandraHelper.truncate(session, ExternalConsent.class);
    }

    @Test
    void whenCreateOrUpdateConsentWithNewConsent_thenTableIsUpdated() {

        // Given
        long originalCount = countAll(session);

        ExternalConsent toSave = new ExternalConsent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "2018-07", Instant.now(systemUTC()), Instant.now(systemUTC()), "externalId");

        // When
        repository.persist(toSave);

        // Then
        assertThat(countAll(session)).isEqualTo(originalCount + 1);

        // And when
        Optional<ExternalConsent> storedConsentOptional = repository.findBy(toSave.getUserId(), toSave.getSiteId(), toSave.getUserSiteId());

        // Then
        assertThat(storedConsentOptional.isPresent()).isTrue();
        assertEquals(toSave.getExternalConsentId(), storedConsentOptional.get().getExternalConsentId());

    }

    @Test
    void whenCreateOrUpdateConsentWithoutConsent_thenTableIsUpdated() {

        // Given
        long originalCount = countAll(session);

        ExternalConsent toSave = new ExternalConsent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "2018-07", Instant.now(systemUTC()), Instant.now(systemUTC()), null);

        // When
        repository.persist(toSave);

        // Then
        assertThat(countAll(session)).isEqualTo(originalCount + 1);

        // And when
        Optional<ExternalConsent> storedConsentOptional = repository.findBy(toSave.getUserId(), toSave.getSiteId(), toSave.getUserSiteId());

        // Then
        assertThat(storedConsentOptional.isPresent()).isTrue();
        ExternalConsent storedConsent = storedConsentOptional.get();
        assertNull(storedConsent.getExternalConsentId());
    }

    @Test
    void testDeleteConsent() {

        // Given an existing consent
        ExternalConsent existing = new ExternalConsent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "2018-07", Instant.now(systemUTC()), Instant.now(systemUTC()), null);
        repository.persist(existing);

        long originalCount = countAll(session);

        // When consent is deleted
        repository.delete(existing.getUserId(), existing.getSiteId(), existing.getUserSiteId());

        // Data store contains one less consent
        long newCount = countAll(session);
        assertThat(newCount).isEqualTo(originalCount - 1);

        // Assert consent is deleted
        assertFalse(repository.findBy(existing.getUserId(), existing.getSiteId(), existing.getUserSiteId()).isPresent());
    }

    private long countAll(final Session session) {
        Select select = QueryBuilder.select().countAll().from(ExternalConsent.TABLE_NAME);

        ResultSet resultSet = session.execute(select);
        Row row = resultSet.one();

        return row.getLong(0);
    }


}
