package nl.ing.lovebird.sitemanagement.clientconfiguration;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.cassandra.CassandraRepository;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Slf4j
@Repository
class ClientRedirectUrlRepository extends CassandraRepository<ClientRedirectUrl> {

    @Autowired
    protected ClientRedirectUrlRepository(Session session) {
        super(session, ClientRedirectUrl.class);
    }

    public void saveClientRedirectUrl(ClientRedirectUrl clientRedirectUrl) {
        log.info("Saved clientRedirectUrl for client {} and redirectUrlId {}", clientRedirectUrl.getClientId().unwrap(), clientRedirectUrl.getRedirectUrlId());
        super.save(clientRedirectUrl);
    }

    public Optional<ClientRedirectUrl> get(ClientId clientId, UUID redirectUrlId) {
        Select.Where selectQuery = QueryBuilder.select().from(ClientRedirectUrl.TABLE_NAME)
                .where(eq(ClientRedirectUrl.CLIENT_ID_COLUMN, clientId))
                .and(eq(ClientRedirectUrl.REDIRECT_URL_ID_COLUMN, redirectUrlId));
        return selectOne(selectQuery);
    }

    public void delete(ClientId clientId, UUID redirectUrlId) {
        final Delete deleteQuery = createDelete();
        deleteQuery.where(eq(ClientRedirectUrl.CLIENT_ID_COLUMN, clientId))
                .and(eq(ClientRedirectUrl.REDIRECT_URL_ID_COLUMN, redirectUrlId));
        super.executeDelete(deleteQuery);
    }
}
