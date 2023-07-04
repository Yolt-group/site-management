package nl.ing.lovebird.sitemanagement.users;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private Session session;

    @AfterEach
    void tearDown() {
        CassandraHelper.truncate(session, User.class);
    }

    @Test
    void testUserRepository() {
        final User user = new User(UUID.randomUUID(), Instant.now(systemUTC()).truncatedTo(ChronoUnit.MILLIS), ClientId.random(), StatusType.BLOCKED, true);

        // 1. Check user is not retrievable
        Optional<User> optionalUser = repository.getUser(user.getUserId());
        assertThat(optionalUser).isNotPresent();

        // 2. Add user and check that it is retrievable
        repository.upsertUser(user);
        optionalUser = repository.getUser(user.getUserId());
        assertThat(optionalUser).isPresent();
        assertThat(optionalUser.get()).isEqualTo(user);

        // 3. Check null-safe update
        final ClientId clientId = ClientId.random();
        repository.upsertUser(new User(user.getUserId(), null, clientId, null, true));
        optionalUser = repository.getUser(user.getUserId());
        assertThat(optionalUser.get().getClientId()).isEqualTo(clientId);
        assertThat(optionalUser.get().getLastLogin()).isEqualTo(user.getLastLogin());
        assertThat(optionalUser.get().getStatus()).isEqualTo(StatusType.BLOCKED);
        assertThat(optionalUser.get().isOneOffAis()).isEqualTo(true);

        // 4. Delete user and check that it is not retrievable anymore
        repository.deleteUser(user.getUserId());
        optionalUser = repository.getUser(user.getUserId());
        assertThat(optionalUser).isNotPresent();
    }
}
