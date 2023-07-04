package nl.ing.lovebird.sitemanagement.orphanuser;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Select;
import nl.ing.lovebird.cassandra.CassandraRepository;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Repository
public class OrphanUserExternalIdRepository extends CassandraRepository<OrphanUserExternalId> {

    @Autowired
    public OrphanUserExternalIdRepository(final Session session) {
        super(session, OrphanUserExternalId.class);
    }

    public List<OrphanUserExternalId> getForBatchAndProvider(ClientId clientId, final String provider, final UUID batchId, final int fetchSize) {
        Select selectQuery = createSelect();
        selectQuery
                .where(eq(OrphanUserExternalId.CLIENT_ID_COLUMN, clientId))
                .and(eq(OrphanUserExternalId.PROVIDER_COLUMN, provider))
                .and(eq(OrphanUserExternalId.ORPHAN_USER_BATCH_ID_COLUMN, batchId));
        selectQuery.setFetchSize(fetchSize);
        return select(selectQuery);
    }

    public void delete(ClientId clientId, final String provider, final UUID batchId) {
        Delete deleteQuery = createDelete();
        deleteQuery
                .where(eq(OrphanUserExternalId.CLIENT_ID_COLUMN, clientId))
                .and(eq(OrphanUserExternalId.PROVIDER_COLUMN, provider))
                .and(eq(OrphanUserExternalId.ORPHAN_USER_BATCH_ID_COLUMN, batchId));

        executeDelete(deleteQuery);
    }
}
