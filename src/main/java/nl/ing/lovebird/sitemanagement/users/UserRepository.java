package nl.ing.lovebird.sitemanagement.users;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import nl.ing.lovebird.cassandra.CassandraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Repository
class UserRepository extends CassandraRepository<User> {

    @Autowired
    UserRepository(final Session session) {
        super(session, User.class);
    }

    Optional<User> getUser(final UUID userId) {
        return selectOne(eq(User.USER_ID_COLUMN, userId));
    }

    /**
     * Take into account that it will not save nulls values and thus will not override previously stored values
     * with nulls (if any).
     *
     * @param user - a user to be saved or updated
     */
    void upsertUser(final User user) {
        save(user, Mapper.Option.saveNullFields(false));
    }

    void deleteUser(final UUID userId) {
        executeDelete(createDelete(eq(User.USER_ID_COLUMN, userId)));
    }

}
