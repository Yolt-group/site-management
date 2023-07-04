package nl.ing.lovebird.sitemanagement.legacy.aismigration;

import nl.ing.lovebird.sitemanagement.health.service.domain.UserHealth;

@Deprecated
public enum MigrationStatus {
    MIGRATING_TO,
    MIGRATING_FROM,

    /**
     * Don't remove these, they are still used in the database.
     * See {@link nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite#migrationStatus}, this column
     * needs to be deleted (alongside this enum)
     */
    @Deprecated MIGRATION_NEEDED,

    /**
     * {@link UserHealth#getMigrationStatus()} depends on the order of the enum values. Don't move this value without
     * taking that method into account.
     */
    NONE,

    @Deprecated MIGRATION_IN_PROGRESS,
    @Deprecated MIGRATION_DONE,
}
