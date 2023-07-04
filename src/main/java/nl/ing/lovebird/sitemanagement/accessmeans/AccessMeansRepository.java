package nl.ing.lovebird.sitemanagement.accessmeans;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Select;
import nl.ing.lovebird.cassandra.CassandraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Repository
class AccessMeansRepository extends CassandraRepository<AccessMeans> {

    @Autowired
    public AccessMeansRepository(final Session session) {
        super(session, AccessMeans.class);
    }

    @Override
    public void save(final AccessMeans accessMeans) {
        accessMeans.setUpdated(new Date());
        super.save(accessMeans);
    }

    public Optional<AccessMeans> get(final UUID userId, final String provider) {
        final Select selectQuery = createSelect();
        selectQuery.where(eq(AccessMeans.USER_ID_COLUMN, userId))
                .and(eq(AccessMeans.PROVIDER_COLUMN, provider));
        return selectOne(selectQuery);
    }

    public void delete(final UUID userId, final String provider) {
        final Delete deleteQuery = createDelete();
        deleteQuery.where(eq(AccessMeans.USER_ID_COLUMN, userId))
                .and(eq(AccessMeans.PROVIDER_COLUMN, provider));
        super.executeDelete(deleteQuery);
    }
}
