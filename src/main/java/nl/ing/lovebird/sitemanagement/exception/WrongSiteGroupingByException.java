package nl.ing.lovebird.sitemanagement.exception;

import java.util.UUID;

public class WrongSiteGroupingByException extends RuntimeException{
    public WrongSiteGroupingByException(final UUID fromAccountId, final UUID toAccountId, final UUID fromSiteId, final UUID toSiteId) {
        super(String.format("Account: %s with site: %s is not having the same grouping as Account: %s with site: %s", fromAccountId, fromSiteId, toAccountId, toSiteId));
    }
}
