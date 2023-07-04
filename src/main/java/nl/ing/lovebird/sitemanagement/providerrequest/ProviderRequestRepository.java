package nl.ing.lovebird.sitemanagement.providerrequest;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import nl.ing.lovebird.cassandra.CassandraRepository;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

@Repository
@Validated
public class ProviderRequestRepository extends CassandraRepository<ProviderRequest> {

    @Autowired
    public ProviderRequestRepository(final Session session) {
        super(session, ProviderRequest.class);
        super.registerEnum(UserSiteActionType.class);
    }

    public Optional<ProviderRequest> get(final UUID userId, final UUID id) {
        final Select selectByUidQuery = QueryBuilder.select().from(ProviderRequest.TABLE_NAME);
        selectByUidQuery.where(eq(ProviderRequest.USER_ID_COLUMN, userId));
        selectByUidQuery.where(eq(ProviderRequest.ID_COLUMN, id));
        return selectOne(selectByUidQuery);
    }

    public List<ProviderRequest> find(final UUID userid, final UUID activityId) {
        final Select query = QueryBuilder.select().from(ProviderRequest.TABLE_NAME);
        query.where(eq(ProviderRequest.USER_ID_COLUMN, userid));
        return select(query).stream()
                .filter(it -> it.getActivityId().equals(activityId))
                .toList();
    }

    public void saveValidated(final @Valid ProviderRequest entity) {
        super.save(entity);
    }

}

