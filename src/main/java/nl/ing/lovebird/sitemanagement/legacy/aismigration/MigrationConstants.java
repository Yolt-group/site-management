package nl.ing.lovebird.sitemanagement.legacy.aismigration;

import java.util.List;

@Deprecated
public class MigrationConstants {
    public static final List<MigrationStatus> IN_MIGRATION_STATUSES = List.of(MigrationStatus.MIGRATING_FROM, MigrationStatus.MIGRATING_TO);
}
