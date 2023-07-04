package nl.ing.lovebird.sitemanagement.usersite;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.sql.Date;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * This repository represents the database interface for operations on the {@link PostgresUserSiteLock} entity.
 */
@Slf4j
@Repository
public class PostgresUserSiteLockRepository {

    private final Clock clock;

    @PersistenceContext(unitName = "entityManager")
    private final EntityManager entityManager;

    public PostgresUserSiteLockRepository(Clock clock,
                                          @Qualifier("entityManager") EntityManager entityManager) {
        this.clock = clock;
        this.entityManager = entityManager;
    }

    /**
     * Attempt to lock a {@link PostgresUserSite} for exclusive access.
     * <p/>
     * The lock is held for a maximum of <b>10 minutes</b>, when the lock has expired, the lock is automatically released and
     * can be taken by another process.
     *
     * @param userSiteId the user-site id
     * @param activityId the activity id (as meta data)
     * @return true if the lock is claimed, false otherwise
     */
    @Transactional
    public boolean attemptLock(final UUID userSiteId, final UUID activityId) {

        var sql = """
                insert into user_site_lock (user_site_id, activity_id, locked_at) 
                    values (:userSiteId, :activityId, :now)
                    on conflict on constraint user_site_lock_pkey
                        do update 
                            set activity_id  = :activityId,
                                locked_at    = :now
                            where user_site_lock.locked_at is null or 
                                  user_site_lock.locked_at <= :nowMin10Minutes
                """;

        boolean isLocked = entityManager.createNativeQuery(sql)
                .setParameter("userSiteId", userSiteId)
                .setParameter("activityId", activityId)
                .setParameter("now", Date.from(clock.instant()))
                .setParameter("nowMin10Minutes", Date.from(clock.instant().minus(10, ChronoUnit.MINUTES)))
                .executeUpdate() > 0;


        if (isLocked) {
            log.debug("user-site {} successfully locked.", userSiteId);
        } else {
            log.debug("user-site {} could not be locked.", userSiteId);
        }

        return isLocked;
    }
    /**
     * Unlock a (potentially) locked {@link PostgresUserSite}.
     * <p/>
     * The {@link PostgresUserSite} is unlocked by resetting the <code>activityId</code> (not strict necessary) and </code></><code>lockedAt</code>
     * and implemented using and `UPDATE` instead of a `DELETE`. A manual DELETE (lock)/ INSERT (re-acquire lock) will modify the
     * index on the primary key while an HOT update should not touch the index (as frequently)
     *
     * @param userSiteId the user-site id to unlock
     * @return true if unlocked, false otherwise (unlock failed or user-site was not locked to begin with)
     */
    @Transactional
    public boolean unlockUserSite(final UUID userSiteId) {

        var sql = """
                update user_site_lock
                    set activity_id  = null,
                        locked_at    = null
                    where user_site_id  = :userSiteId
                """;

        boolean isUnlocked = entityManager.createNativeQuery(sql)
                .setParameter("userSiteId", userSiteId)
                .executeUpdate() > 0;

        if (isUnlocked) {
            log.debug("user-site {} successfully unlocked.", userSiteId);
        } else {
            log.debug("user-site {} could not be unlocked (might not exist).", userSiteId);
        }

        return isUnlocked;
    }

    /**
     * Return the potentially held {@link PostgresUserSiteLock}.
     *
     * @param userSiteId the user-site id to unlock
     * @return a {@link PostgresUserSiteLock} if the lock is held, nothing otherwise.
     */
    @Transactional
    public Optional<PostgresUserSiteLock> get(final UUID userSiteId) {

        var sql = """
                select * from user_site_lock
                    where user_site_id  = :userSiteId and
                          activity_id   is not null and
                          (
                              locked_at is not null or locked_at >= :nowMin10Minutes
                          )
                for update 
                """;

        PostgresUserSiteLock lock;
        try {
            lock = (PostgresUserSiteLock) entityManager.createNativeQuery(sql, PostgresUserSiteLock.class)
                    .setParameter("userSiteId", userSiteId)
                    .setParameter("nowMin10Minutes", Date.from(clock.instant().minus(10, ChronoUnit.MINUTES)))
                    .getSingleResult();
        } catch (NoResultException e) {
            log.debug("No lock available for user-site {}.", userSiteId);
            return Optional.empty();
        }
        return Optional.of(lock);
    }

    /**
     * Returns a {@see PostgresUserSiteLock} for the given user-site if exists.
     * This method is for testing purposes only so we do not have to rely on SQL in the test-suite
     *
     * @return a {@see PostgresUserSiteLock}s if exists, otherwise empty
     */
    @VisibleForTesting
    Optional<PostgresUserSiteLock> getUnconditionally(final UUID userSiteId) {

        var sql = """
                select * from user_site_lock
                    where user_site_id  = :userSiteId
                for update 
                """;

        PostgresUserSiteLock lock;
        try {
            lock = (PostgresUserSiteLock) entityManager.createNativeQuery(sql, PostgresUserSiteLock.class)
                    .setParameter("userSiteId", userSiteId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return Optional.empty();
        }
        return Optional.of(lock);
    }
}
