package nl.ing.lovebird.sitemanagement.exception;

import java.util.UUID;

public class AlreadyMigratedException extends RuntimeException {
    public AlreadyMigratedException(final UUID userSiteId, final UUID accountId) {
        super(String.format("Account: %s with user site: %s was already migrated.", accountId, userSiteId));
    }
}
