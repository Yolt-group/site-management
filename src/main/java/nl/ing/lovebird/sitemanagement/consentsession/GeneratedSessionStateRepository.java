package nl.ing.lovebird.sitemanagement.consentsession;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Update;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.cassandra.CassandraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

@Repository
@Slf4j
public class GeneratedSessionStateRepository extends CassandraRepository<GeneratedSessionState> {

    @Autowired
    public GeneratedSessionStateRepository(final Session session) {
        super(session, GeneratedSessionState.class);
    }

    public List<GeneratedSessionState> get(final UUID userId) {

        return select(QueryBuilder.select().from(GeneratedSessionState.TABLE_NAME)
                .where(eq(GeneratedSessionState.USER_ID_COLUMN, userId)));
    }

    public void markAsSubmitted(final UUID userId, final String stateId) {

        Update update = createUpdate();
        update.with(set(GeneratedSessionState.SUBMITTED_COLUMN, true))
                .and(set(GeneratedSessionState.SUBMITTED_TIME_COLUMN, new Date()))
                .where(eq(GeneratedSessionState.USER_ID_COLUMN, userId))
                .and(eq(GeneratedSessionState.STATE_ID_COLUMN, stateId));
        executeUpdate(update);
    }

    public void store(final GeneratedSessionState generatedSessionState) {
        super.save(generatedSessionState);
    }

    public void delete(UUID userId) {
        Delete delete = createDelete();
        delete.where(eq(GeneratedSessionState.USER_ID_COLUMN, userId));
        super.executeDelete(delete);
    }
}
