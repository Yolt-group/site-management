package nl.ing.lovebird.sitemanagement.exception;

import java.util.UUID;

public class FromAccountScraperException extends RuntimeException {
    public FromAccountScraperException(final UUID userSiteId, final UUID accountId) {
        super(String.format("Account: %s with user site: %s is not using a scraper connection.", accountId, userSiteId));
    }
}
