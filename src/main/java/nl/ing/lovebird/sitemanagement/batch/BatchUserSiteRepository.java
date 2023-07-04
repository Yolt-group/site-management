package nl.ing.lovebird.sitemanagement.batch;

import com.google.common.base.Preconditions;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationStatus;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.type.CustomType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
public class BatchUserSiteRepository {

    public static final CustomType MIGRATION_STATUS_TYPE = new CustomType(new PostgreSQLEnumType(MigrationStatus.class));
    private final Clock clock;

    @PersistenceContext(unitName = "batchEntityManager")
    private final EntityManager batchEntityManager;

    public BatchUserSiteRepository(Clock clock,
                                   @Qualifier("batchEntityManager") EntityManager batchEntityManager) {
        this.clock = clock;
        this.batchEntityManager = batchEntityManager;
    }

    /**
     * Sets the status, reason, connection status and failure reason of all user-sites of the provided site to
     * respectively {@code LOGIN_FAILED}, {@code CONSENT_EXPIRED}, {@code DISCONNECTED} and {@code CONSENT_EXPIRED},
     * thereby making it obvious to the user that the user-site can no longer be used.
     *
     * @param siteId the id of the site to disconnect all user-sites for
     * @param dryrun whether to rollback changes
     * @return the list of affected user-sites
     */
    long disconnectUserSitesForSite(final UUID siteId, boolean dryrun) {
        var sql = """
                UPDATE user_site u
                    SET connection_status = 'DISCONNECTED',
                        failure_reason = 'CONSENT_EXPIRED'
                FROM (
                    SELECT id FROM user_site
                    WHERE site_id = :siteId AND
                          connection_status != 'DISCONNECTED' AND
                          is_deleted = false
                ) AS prev
                WHERE u.id = prev.id;""";

        try (StatelessSession statelessSession = createStatelessSession()) {
            try (TransactionWrapper transactionWrapper = TransactionWrapper.wrap(statelessSession.beginTransaction(), dryrun)) {
                return statelessSession.createNativeQuery(sql, Tuple.class)
                        .setParameter("siteId", siteId)
                        .executeUpdate();
            }
        }
    }

    /**
     * Sets the status, reason, connection status and failure reason of all user-sites with one of the provided
     * migration statuses to respectively {@code LOGIN_FAILED}, {@code CONSENT_EXPIRED}, {@code DISCONNECTED} and
     * {@code CONSENT_EXPIRED}, thereby making it obvious to the user that the user-site can no longer be used.
     *
     * @param migrationStatuses the migration statuses for which to disconnect the user-sites
     * @param dryrun            whether to rollback changes
     * @return the list of affected user-sites
     */
    long disconnectUserSitesWithMigrationStatus(List<MigrationStatus> migrationStatuses, boolean dryrun) {
        if (migrationStatuses.isEmpty()) {
            return 0L;
        }

        var sql = """
                UPDATE user_site u
                    SET connection_status = 'DISCONNECTED',
                        failure_reason = 'CONSENT_EXPIRED'
                FROM (
                    SELECT id FROM user_site
                    WHERE migration_status IN ( :migration_statuses ) AND
                          connection_status != 'DISCONNECTED' AND
                          is_deleted = false
                ) AS prev
                WHERE u.id = prev.id;""";

        try (StatelessSession statelessSession = createStatelessSession()) {
            try (TransactionWrapper transactionWrapper = TransactionWrapper.wrap(statelessSession.beginTransaction(), dryrun)) {
                return statelessSession.createNativeQuery(sql, Tuple.class)
                        .setParameterList("migration_statuses", migrationStatuses, MIGRATION_STATUS_TYPE)
                        .executeUpdate();
            }
        }
    }

    /**
     * Sets the status, reason, connection status and failure reason of all user-sites that have a last data fetch time
     * of more than 90 days ago to respectively {@code LOGIN_FAILED}, {@code CONSENT_EXPIRED}, {@code DISCONNECTED}
     * and {@code CONSENT_EXPIRED}, thereby making it obvious to the user that the user-site can no longer be used.
     * The PPS site is ignored because it does not require explicit consent from the user. All yolt app users give implicit consent.
     *
     * @param dryrun whether to rollback changes
     * @return the list of affected user-sites
     */
    long disconnectUserSitesNotRefreshedFor90Days(boolean dryrun) {
        //TODO remove filter on PPS site_id (b1fa25e2-f696-45c1-b59b-59c5fd40c175) in selection after app support drops
        var sql = """
                UPDATE user_site u
                    SET connection_status = 'DISCONNECTED',
                        failure_reason = 'CONSENT_EXPIRED'
                FROM (
                    SELECT id FROM user_site
                    WHERE (last_data_fetch < :instant OR last_data_fetch IS NULL) AND
                        created < :instant AND
                        connection_status != 'DISCONNECTED' AND
                        is_deleted = false AND
                        site_id != 'b1fa25e2-f696-45c1-b59b-59c5fd40c175'
                ) AS prev
                WHERE u.id = prev.id""";

        try (StatelessSession statelessSession = createStatelessSession()) {
            try (TransactionWrapper transactionWrapper = TransactionWrapper.wrap(statelessSession.beginTransaction(), dryrun)) {
                return statelessSession.createNativeQuery(sql, Tuple.class)
                        .setParameter("instant", Instant.now(clock).minus(90, ChronoUnit.DAYS))
                        .executeUpdate();
            }
        }
    }

    /**
     * Create a stateless hibernate session
     *
     * @return the stateless session
     */
    private StatelessSession createStatelessSession() {
        return Preconditions.checkNotNull(batchEntityManager.getEntityManagerFactory().unwrap(SessionFactory.class).openStatelessSession(), "EntityManagerFactory does not support Hibernate");
    }

    @RequiredArgsConstructor(staticName = "wrap")
    private static class TransactionWrapper implements AutoCloseable {
        public final Transaction transaction;
        public final boolean dryRun;

        @Override
        public void close() {
            if (dryRun) {
                transaction.rollback();
            } else {
                transaction.commit();
            }
        }
    }
}
