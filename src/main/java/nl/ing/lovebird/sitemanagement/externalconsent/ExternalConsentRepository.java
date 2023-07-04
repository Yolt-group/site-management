package nl.ing.lovebird.sitemanagement.externalconsent;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Select;
import nl.ing.lovebird.cassandra.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Repository
class ExternalConsentRepository extends CassandraRepository<ExternalConsent> {

    ExternalConsentRepository (Session session) {
        super(session, ExternalConsent.class);
    }

    void persist(ExternalConsent consent) {
        super.save(consent);
    }

    Optional<ExternalConsent> findBy(UUID userId, UUID siteId, UUID userSiteId) {
        Select.Where selectByUidQuery = createSelect()
                .where(eq(ExternalConsent.USER_ID_COLUMN, userId))
                .and(eq(ExternalConsent.SITE_ID_COLUMN, siteId))
                .and(eq(ExternalConsent.USER_SITE_ID_COLUMN, userSiteId));
        return selectOne(selectByUidQuery);
    }

    void delete(UUID userId, UUID siteId, UUID userSiteId) {

        Delete deleteQuery = createDelete();
        deleteQuery
                .where(eq(ExternalConsent.USER_ID_COLUMN, userId))
                .and(eq(ExternalConsent.SITE_ID_COLUMN, siteId))
                .and(eq(ExternalConsent.USER_SITE_ID_COLUMN, userSiteId));

        executeDelete(deleteQuery);
    }
}
