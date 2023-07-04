package nl.ing.lovebird.sitemanagement.usersite;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.*;
import javax.validation.Valid;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static org.springframework.data.jpa.repository.support.JpaEntityInformationSupport.getEntityInformation;


@Slf4j
@Repository
public class PostgresUserSiteRepository {

    private final Clock clock;

    @PersistenceContext(unitName = "entityManager")
    private final EntityManager entityManager;

    public PostgresUserSiteRepository(Clock clock,
                                      @Qualifier("entityManager") EntityManager entityManager) {
        this.clock = clock;
        this.entityManager = entityManager;
    }

    /**
     * Return a {@see UserSite} for the given <code>userId</code> and <code>userSiteId</code>
     *
     * @param userId     the user-id
     * @param userSiteId the user site-id
     * @return the {@see UserSite} if any, null otherwise
     */
    @Transactional
    public Optional<PostgresUserSite> getUserSite(final UUID userId, final UUID userSiteId) {
        return getUserSiteWithLock(userId, userSiteId, LockModeType.NONE);
    }

    /**
     * Return a {@see UserSite} for the given <code>clientId</code> and <code>userSiteId</code>
     *
     * @param clientId   the client-id
     * @param userSiteId the user site-id
     * @return the {@see UserSite} if any, null otherwise
     */
    @Transactional
    public Optional<PostgresUserSite> getUserSiteByClientId(final ClientId clientId, final UUID userSiteId) {
        TypedQuery<PostgresUserSite> result = entityManager.createQuery("select p from PostgresUserSite p where p.userSiteId = :userSiteId and p.clientId = :clientId", PostgresUserSite.class)
                .setParameter("userSiteId", userSiteId)
                .setParameter("clientId", clientId.unwrap());
        try {
            return Optional.of(result.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Return a {@see UserSite} for the given <code>userId</code> and <code>userSiteId</code>
     * with a <b>Pessimistic Write Lock</b>
     *
     * @param userId     the user-id
     * @param userSiteId the user site-id
     * @return the {@see UserSite} if any, null otherwise
     */
    @Transactional
    public Optional<PostgresUserSite> getUserSiteWithWriteLock(UUID userId, final UUID userSiteId) {
        return getUserSiteWithLock(userId, userSiteId, LockModeType.PESSIMISTIC_WRITE);
    }

    private Optional<PostgresUserSite> getUserSiteWithLock(final UUID userId, final UUID userSiteId, LockModeType lockModeType) {
        TypedQuery<PostgresUserSite> result = entityManager.createQuery("select p from PostgresUserSite p where p.userSiteId = :userSiteId and p.userId = :userId", PostgresUserSite.class)
                .setParameter("userSiteId", userSiteId)
                .setParameter("userId", userId)
                .setLockMode(lockModeType);
        try {
            return Optional.of(result.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /**
     * Return a {@see List} of {@see UserSite}'s for the given user
     *
     * @param userId the user-id
     * @return the a {@see List} of {@see UserSite}'s
     */
    @Transactional
    public List<PostgresUserSite> getUserSites(final UUID userId) {
        Query q = entityManager
                .createNativeQuery("select * from user_site us where us.user_id = ?1", PostgresUserSite.class);
        q.setParameter(1, userId);

        return (List<PostgresUserSite>) q.getResultList();
    }


    @Transactional(readOnly = true)
    public List<PostgresUserSite> getUserSitesBySite(UUID siteId, int limit) {
        Assert.isTrue(limit > 0, "Limit must be greater then zero.");

        Query q = entityManager
                .createNativeQuery("select * from user_site us where us.site_id = :siteId limit :limit", PostgresUserSite.class);
        q.setParameter("siteId", siteId);
        q.setParameter("limit", limit);

        return (List<PostgresUserSite>) q.getResultList();
    }

    /**
     * Return a {@see List} of {@see UserSite}'s for the given status.
     *
     * @param statusCode the status code
     * @return the a {@see List} of {@see UserSite}'s
     */
    @Transactional(readOnly = true)
    public List<PostgresUserSite> getUserSitesWithStatus(final ConnectionStatus statusCode, final int limit) {
        Query q = entityManager
                .createNativeQuery("select * from user_site where connection_status = cast(?1 as user_site_connection_status_t) LIMIT ?2", PostgresUserSite.class);
        q.setParameter(1, statusCode.name());
        q.setParameter(2, limit);

        return (List<PostgresUserSite>) q.getResultList();
    }


    @Transactional
    public PostgresUserSite save(@Valid PostgresUserSite userSite) {
        JpaEntityInformation<PostgresUserSite, ?> entityInformation = getEntityInformation(PostgresUserSite.class, this.entityManager);

        if (entityInformation.isNew(userSite)) {
            this.entityManager.persist(userSite);
            return userSite;
        } else {
            return this.entityManager.merge(userSite);
        }
    }

    @Transactional(readOnly = true)
    public Set<ClientId> getClientIdsWithUserSite() {
        Query q = entityManager
                .createNativeQuery("select distinct CAST(client_id as text) from user_site where is_deleted = false;");

        return ((List<String>) q.getResultList()).stream()
                .map(it -> new ClientId(UUID.fromString(it)))
                .collect(toSet());
    }

    @Transactional(readOnly = true)
    public List<UUID> getUserIdsBetween(UUID minInclusiveUUID, UUID maxExclusive, ClientId clientId) {
        final int limit = 1000;

        Query q = entityManager
                .createNativeQuery("select distinct cast(user_id as text) from user_site where is_deleted = false and user_id >= :minUuid AND user_id < :maxUuid and client_id = :clientId limit " + limit)
                .setParameter("minUuid", minInclusiveUUID)
                .setParameter("maxUuid", maxExclusive)
                .setParameter("clientId", clientId.unwrap());

        List<String> resultList = (List<String>) q.getResultList();
        if (resultList.size() == limit) {
            log.error("We have fetched {} user ids for client {}. This is more then expected. Please fix this.", limit, clientId); //NOSHERIFF
        }
        return resultList.stream()
                .map(UUID::fromString)
                .toList();
    }

    @Transactional
    public void deleteUserSite(final UUID userSiteId) {
        Query q = entityManager.createNativeQuery("delete from user_site where id = ?1");
        q.setParameter(1, userSiteId);
        int deleted = q.executeUpdate();

        log.info("Deleted {} user-sites with id  {}", deleted, userSiteId);
    }

    /**
     * @param clientId the client-id to get the connection totals for
     * @return the connection info totals
     */
    @Transactional(readOnly = true)
    public Map<UUID, UserSiteTotalsInfo> getUserSiteTotalsInfo(final ClientId clientId) {
        Query q = entityManager
                .createNativeQuery("SELECT CAST(site_id as varchar), CAST(COUNT(u.user_id) as integer) as connections, CAST(COUNT(distinct u.user_id) as integer) as users FROM user_site u WHERE u.client_id = ?1  GROUP BY u.site_id");
        q.setParameter(1, clientId.unwrap());

        return ((List<Object[]>) q.getResultList()).stream()
                .map(record -> UserSiteTotalsInfo.builder()
                        .siteId(UUID.fromString((String) record[0]))
                        .nrOfUniqueConnections((Integer) record[1])
                        .nrOfUniqueUsers((Integer) record[2])
                        .build())
                .collect(toMap(userSiteTotalsInfo -> userSiteTotalsInfo.siteId, identity()));
    }

    /**
     * Output format:
     * <pre>
     *                    site_id                |       status       | status_count
     * --------------------------------------+--------------------+--------------
     *  8d25dc30-7dc9-11e8-adc0-fa7ae01bbebc | REFRESH_FINISHED   |         6096
     *  33aca8b9-281a-4259-8492-1b37706af6db | REFRESH_FINISHED   |         8073
     *  33aca8b9-281a-4259-8492-1b37706af6db | LOGIN_FAILED       |            2
     *  ca8a362a-a351-4358-9f1c-a8b4b91ed65b | LOGIN_FAILED       |           49
     *  33aca8b9-281a-4259-8492-1b37706af6db | UNKNOWN            |            1
     *  8d25dc30-7dc9-11e8-adc0-fa7ae01bbebc | REFRESH_FAILED     |            1
     *  ca8a362a-a351-4358-9f1c-a8b4b91ed65b | REFRESH_FAILED     |            7
     *  8d25dc30-7dc9-11e8-adc0-fa7ae01bbebc | LOGIN_FAILED       |           74
     *  8d25dc30-7dc9-11e8-adc0-fa7ae01bbebc | UNKNOWN            |           97
     *  33aca8b9-281a-4259-8492-1b37706af6db | LOGIN_SUCCEEDED    |            7
     *  33aca8b9-281a-4259-8492-1b37706af6db | INITIAL_PROCESSING |        15573
     *  33aca8b9-281a-4259-8492-1b37706af6db | REFRESH_FAILED     |           47
     * </pre>
     * <p>
     * Query plan:
     * <pre>
     *  HashAggregate  (cost=1760.64..1762.95 rows=231 width=36)
     *    Output: site_id, status, count(status), count(user_id)
     *    Group Key: user_site.site_id, user_site.status
     *    ->  Seq Scan on public.user_site  (cost=0.00..1460.70 rows=29994 width=36)
     *          Output: id, user_id, site_id, client_id, external_id, status, reason, status_timeout_time, created, updated, last_data_fetch, provider, migration_status, redirect_url_id, persisted_form_step_answers, version
     *          Filter: (user_site.client_id = '43d71a36-cf05-47c6-a2ca-c426eb5537e3'::uuid)
     * </pre>
     *
     * @param clientId the client-id
     * @return a {@link List} of {@link UserSiteConnectionInfo}}'s per site
     */
    @Transactional(readOnly = true)
    public Map<UUID, List<UserSiteConnectionInfo>> getConnectionStatusBySite(final ClientId clientId) {
        Query q = entityManager
                .createNativeQuery("select cast(site_id as varchar), connection_status, failure_reason, cast(count(*) as integer) as c from user_site where client_id = ?1 group by site_id, connection_status, failure_reason");
        q.setParameter(1, clientId.unwrap());

        // TODO
        return ((List<Object[]>) q.getResultList()).stream()
                .map(record -> UserSiteConnectionInfo.builder()
                        .siteId(UUID.fromString((String) record[0]))
                        .connectionStatus(ConnectionStatus.valueOf((String) record[1]))
                        .failureReason(Optional.ofNullable((String) record[2]).map(FailureReason::valueOf).orElse(null))
                        .count((Integer) record[3])
                        .build())
                .collect(groupingBy(o -> o.siteId, mapping(identity(), toList())));
    }


    @Transactional(readOnly = true)
    public List<UniqueRefreshesPerClientInfo> getNumberOfRefreshesPerClient(int daysInPast) {
        var query = entityManager.createNativeQuery("SELECT cast(client_id as varchar), COUNT(distinct user_id) FROM user_site WHERE last_data_fetch >= ?1 GROUP BY client_id");
        query.setParameter(1, Instant.now(clock).minus(Duration.ofDays(daysInPast)));

        return ((List<Object[]>) query.getResultList()).stream()
                .map(row -> UniqueRefreshesPerClientInfo.builder()
                        .clientId(new ClientId((String) row[0]))
                        .uniqueRefreshes((BigInteger) row[1])
                        .build())
                .collect(toList());
    }

    @Transactional(readOnly = true)
    public String userSiteConnectionStatusesAndFailureReasonsCounts() {
        var query = entityManager.createNativeQuery("select connection_status, failure_reason, count(*) from user_site group by 1, 2 order by 3 desc");
        return ((List<Object[]>) query.getResultList()).stream()
                .map(r -> r[0] + "_" + r[1] + ":" + r[2]) // <status>_<reason>:<count>
                .collect(joining(","));
    }


    @Transactional(readOnly = true)
    public String userSiteMigrationStatussesCounts() {
        var query = entityManager.createNativeQuery("select migration_status, count(*) from user_site group by 1 order by 2 desc");
        return ((List<Object[]>) query.getResultList()).stream()
                .map(r -> r[0] + ":" + r[1]) // <migration_status>:<count>
                .collect(joining(","));
    }


    @Builder
    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class UserSiteTotalsInfo {
        @NonNull
        public final UUID siteId;
        @NonNull
        public final Integer nrOfUniqueConnections;
        @NonNull
        public final Integer nrOfUniqueUsers;
    }

    @Builder
    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class UserSiteConnectionInfo {
        @NonNull
        public final UUID siteId;
        @NonNull
        public final ConnectionStatus connectionStatus;
        public final FailureReason failureReason;
        @NonNull
        public final int count;
    }

    @Builder
    @Value
    public static class UniqueRefreshesPerClientInfo {
        @NonNull
        ClientId clientId;
        @NonNull
        BigInteger uniqueRefreshes;

        @Override
        public String toString() {
            return "ClientId: " + clientId + " --- Unique refreshes: " + uniqueRefreshes;
        }
    }

    public static <T> Optional<T> some(T value) {
        return Optional.ofNullable(value);
    }
}
