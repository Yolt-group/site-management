package nl.ing.lovebird.sitemanagement.usersite;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

/**
 * This repository represents the database interface for operations on the {@link PostgresUserSiteAuditLog} entity.
 * <p/>
 * This database table can be used to store information about the lifecycle of a user-site which is useful
 * for debug and audit purposes. Currently the only historical event that is stored is the user-site delete.
 * <p/>
 * Note: The deleted event is automatically recorded when a user-site is marked as deleted by
 * the plpsql trigger <code>audit_deleted_fn</code>
 */
@Slf4j
@Repository
public class PostgresUserSiteAuditLogRepository {

    @PersistenceContext(unitName = "entityManager")
    private final EntityManager entityManager;

    public PostgresUserSiteAuditLogRepository(@Qualifier("entityManager") EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @VisibleForTesting
    public List<PostgresUserSiteAuditLog> getForUser(final UUID userId) {
        return entityManager
                .createQuery("select p from PostgresUserSiteAuditLog p where p.userId = :userId", PostgresUserSiteAuditLog.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
