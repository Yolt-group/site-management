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
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

@Repository
public class OrphanUserBatchRepository extends CassandraRepository<OrphanUserBatch> {
    private final Clock clock;

    @Autowired
    public OrphanUserBatchRepository(final Session session, Clock clock) {
        super(session, OrphanUserBatch.class);
        this.clock = clock;
    }

    public Optional<OrphanUserBatch> get(ClientId clientId, final String provider, final UUID orphanUserBatchId) {
        Select selectQuery = createSelect();
        selectQuery
                .where(eq(OrphanUserBatch.CLIENT_ID_COLUMN, clientId))
                .and(eq(OrphanUserBatch.PROVIDER_COLUMN, provider))
                .and(eq(OrphanUserBatch.ORPHAN_USER_BATCH_ID_COLUMN, orphanUserBatchId));
        return selectOne(selectQuery);
    }

    public List<OrphanUserBatch> list(ClientId clientId, final String provider) {
        Select selectQuery = createSelect();
        selectQuery
                .where(eq(OrphanUserBatch.CLIENT_ID_COLUMN, clientId))
                .and(eq(OrphanUserBatch.PROVIDER_COLUMN, provider));
        return select(selectQuery);
    }

    @Override
    public void save(OrphanUserBatch entity) {
        super.save(entity);
    }

    public void updateStatus(ClientId clientId, final String provider, final UUID orphanUserBatchId, final OrphanUserBatch.Status status) {
        Optional<OrphanUserBatch> orphanUserBatch = get(clientId, provider, orphanUserBatchId);
        if (orphanUserBatch.isEmpty()) {
            throw new OrphanUserBatchNotFoundException(provider, orphanUserBatchId);
        }

        Update updateQuery = createUpdate();
        updateQuery.with(set(OrphanUserBatch.UPDATED_TIMESTAMP_COLUMN, Instant.now(clock)))
                .and(set(OrphanUserBatch.STATUS_COLUMN, status))
                .where(eq(OrphanUserBatch.CLIENT_ID_COLUMN, clientId))
                .and(eq(OrphanUserBatch.PROVIDER_COLUMN, provider))
                .and(eq(OrphanUserBatch.ORPHAN_USER_BATCH_ID_COLUMN, orphanUserBatchId));

        executeUpdate(updateQuery);
    }

    public void delete(ClientId clientId, final String provider, final UUID orphanUserBatchId) {
        Delete deleteQuery = createDelete();
        deleteQuery
                .where(eq(OrphanUserBatch.CLIENT_ID_COLUMN, clientId))
                .and(eq(OrphanUserBatch.PROVIDER_COLUMN, provider))
                .and(eq(OrphanUserBatch.ORPHAN_USER_BATCH_ID_COLUMN, orphanUserBatchId));

        executeDelete(deleteQuery);
    }
}
