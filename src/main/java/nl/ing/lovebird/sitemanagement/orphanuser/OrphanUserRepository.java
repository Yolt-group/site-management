package nl.ing.lovebird.sitemanagement.orphanuser;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import nl.ing.lovebird.cassandra.CassandraRepository;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

@Repository
public class OrphanUserRepository extends CassandraRepository<OrphanUser> {
    private final Clock clock;

    @Autowired
    public OrphanUserRepository(final Session session, Clock clock) {
        super(session, OrphanUser.class);
        this.clock = clock;
    }

    public List<OrphanUser> listOrphanUsers(ClientId clientId, final String provider, final UUID orphanUserBatchId, final int fetchSize) {
        Select selectByUidQuery = createSelect();
        selectByUidQuery
                .where(eq(OrphanUser.CLIENT_ID_COLUMN, clientId))
                .and(eq(OrphanUser.PROVIDER_COLUMN, provider))
                .and(eq(OrphanUser.ORPHAN_USER_BATCH_ID_COLUMN, orphanUserBatchId));
        selectByUidQuery.setFetchSize(fetchSize);
        return select(selectByUidQuery);
    }

    @Override
    public void save(OrphanUser entity) {
        super.save(entity);
    }

    public void updateStatus(ClientId clientId, final String provider, final UUID orphanUserBatchId, final String externalUserId, final OrphanUser.Status status) {
        Update updateQuery = createUpdate();
        updateQuery.with(set(OrphanUser.UPDATED_TIMESTAMP_COLUMN, Instant.now(clock)))
                .and(set(OrphanUser.STATUS_COLUMN, status))
                .where(eq(OrphanUser.CLIENT_ID_COLUMN, clientId))
                .and(eq(OrphanUser.PROVIDER_COLUMN, provider))
                .and(eq(OrphanUser.ORPHAN_USER_BATCH_ID_COLUMN, orphanUserBatchId))
                .and(eq(OrphanUser.EXTERNAL_USER_ID, externalUserId));

        executeUpdate(updateQuery);
    }

    public void delete(ClientId clientId, final String provider, final UUID orphanUserBatchId) {
        Delete deleteQuery = createDelete();
        deleteQuery
                .where(eq(OrphanUser.CLIENT_ID_COLUMN, clientId))
                .and(eq(OrphanUser.PROVIDER_COLUMN, provider))
                .and(eq(OrphanUser.ORPHAN_USER_BATCH_ID_COLUMN, orphanUserBatchId));

        executeDelete(deleteQuery);
    }
}
