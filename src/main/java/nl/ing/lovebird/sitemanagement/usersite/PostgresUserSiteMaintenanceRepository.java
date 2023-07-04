package nl.ing.lovebird.sitemanagement.usersite;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.sites.Site;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Tuple;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

/**
 * This repository does not manage an entity, it merely contains operations which are applied to multiple user-sites at once.
 * </p>
 * Note: these operations make use of a {@link StatelessSession} <b>without</b> Spring transaction management,
 * therefore these operations/ methods cannot be joined-in with other transactions by using the Spring @Transactional annotation
 * or by any other means.
 * <p/>
 * Most SQL statements below are executed without a transaction being explicitly started by the application,
 * postgres will automatically start a new transaction for all statements on a session without an active one (implicit transaction).
 * However, some SQL statements do require explicit transaction management (such as <pre>insert into ... on conflict do update ...</pre>),
 * this can be done by explicitly beginning and finishing via {@link StatelessSession#beginTransaction()} - execute statement) - {@link Transaction#commit()}
 * <br />
 * Also, if it is required to execute multiple SQL statements in a single transaction these have to be all
 * wrapped in an explicit overarching transaction.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PostgresUserSiteMaintenanceRepository {

    private final Clock clock;
    private final EntityManagerFactory entityManagerFactory;

    /**
     * Reset the last-data-fetch property of all {@link PostgresUserSite}s for a specific <code>clientId</code and <code>siteId</code>
     * for {@link PostgresUserSite}s created and/or updated in the last 180 days.
     *
     * @param clientId the identifier of the client
     * @param siteId   the identifier of the {@link Site} to reset
     * @return the number of the affected {@link PostgresUserSite}s
     */
    public List<SimpleUserSiteId> resetLastDataFetchForSiteLast180Days(final ClientId clientId, final UUID siteId) {

        var sql = """
                UPDATE user_site 
                    SET last_data_fetch = NULL
                WHERE   client_id = :clientId AND   
                        site_id   = :siteId AND
                        last_data_fetch IS NOT NULL AND
                        coalesce(updated, created) >= :nowMinus180days AND
                        is_deleted = false
                RETURNING   cast(id as varchar), 
                            cast(user_id as varchar)
                """;

        try (StatelessSession statelessSession = createStatelessSession()) {
            List<Tuple> result = statelessSession.createNativeQuery(sql, Tuple.class)
                    .setParameter("clientId", clientId.unwrap())
                    .setParameter("siteId", siteId)
                    .setParameter("nowMinus180days", clock.instant().minus(180, ChronoUnit.DAYS))
                    .getResultList();

            return SimpleUserSiteId.fromUntypedJDBCTuples(result);
        }
    }

    /**
     * Return a list of maximum 1000 {@link PostgresUserSite}s for a given client.
     * <p/>
     * <b>The method is for *testing* purposes only and should be moved to a test scoped repository at some point.<b/>
     *
     * @param clientId the client-id for which to return the {@link PostgresUserSite}s
     * @return the list of {@link PostgresUserSite}s
     */
    @VisibleForTesting
    List<PostgresUserSite> getUserSitesForClient(final ClientId clientId) {
        try (StatelessSession statelessSession = createStatelessSession()) {
            return statelessSession.createNativeQuery("select * from user_site where client_id = :clientId", PostgresUserSite.class)
                    .setParameter("clientId", clientId.unwrap())
                    .setMaxResults(1000)
                    .getResultList();
        }
    }

    @Builder
    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class SimpleUserSiteId {
        @NonNull
        public final UUID id;
        @NonNull
        public final UUID userId;

        static List<SimpleUserSiteId> fromUntypedJDBCTuples(final List<Tuple> tuples) {
            return tuples
                    .stream()
                    .map(tuple -> SimpleUserSiteId.builder()
                            .id(UUID.fromString(tuple.get("id", String.class)))
                            .userId(UUID.fromString(tuple.get("user_id", String.class)))
                            .build())
                    .collect(toList());
        }
    }

    /**
     * Create a stateless hibernate session
     *
     * @return the stateless session
     */
    private StatelessSession createStatelessSession() {
        return Preconditions.checkNotNull(entityManagerFactory.unwrap(SessionFactory.class).openStatelessSession(), "EntityManagerFactory does not support Hibernate");
    }
}
