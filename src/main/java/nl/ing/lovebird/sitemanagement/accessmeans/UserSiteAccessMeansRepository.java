package nl.ing.lovebird.sitemanagement.accessmeans;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.cassandra.CassandraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Slf4j
@Repository
class UserSiteAccessMeansRepository extends CassandraRepository<UserSiteAccessMeans> {

    @Autowired
    public UserSiteAccessMeansRepository(final Session session) {
        super(session, UserSiteAccessMeans.class);
        setAuditLoggingEnabled(false);
    }

    /**
     * The created timestamp of the usersite accessmeans should never be null. We will default it to the epoch timestamp
     * if it's not set before saving it to cassandra.
     */
    @Override
    public void save(final UserSiteAccessMeans userSiteAccessMeans) {
        userSiteAccessMeans.setUpdated(new Date());
        if (userSiteAccessMeans.getCreated() == null) {
            // Temporary error log to monitor whether this doesn't ever happen, will throw a MalformedAccessMeansException
            // in the future.
            log.error("Created timestamp not set before persisting accessmeans, this should not happen.");
        }
        super.save(userSiteAccessMeans);
    }

    public Collection<UserSiteAccessMeans> getForUser(final UUID userId) {
        final Select selectQuery = createSelect();
        selectQuery.where(eq(UserSiteAccessMeans.USER_ID_COLUMN, userId));
        return select(selectQuery);
    }

    public Optional<UserSiteAccessMeans> get(final UUID userId, final UUID userSiteId, final String provider) {
        final Select selectQuery = createSelect();
        selectQuery.where(eq(UserSiteAccessMeans.USER_ID_COLUMN, userId))
                .and(eq(UserSiteAccessMeans.USER_SITE_ID_COLUMN, userSiteId))
                .and(eq(UserSiteAccessMeans.PROVIDER_COLUMN, provider));
        return selectOne(selectQuery);
    }

    public void delete(final UUID userId, final UUID userSiteId, final String provider) {
        final Delete deleteQuery = createDelete();
        deleteQuery.where(eq(UserSiteAccessMeans.USER_ID_COLUMN, userId))
                .and(eq(UserSiteAccessMeans.USER_SITE_ID_COLUMN, userSiteId))
                .and(eq(UserSiteAccessMeans.PROVIDER_COLUMN, provider));
        super.executeDelete(deleteQuery);
    }
}
