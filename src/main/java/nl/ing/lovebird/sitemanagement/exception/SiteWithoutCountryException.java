package nl.ing.lovebird.sitemanagement.exception;

import java.util.UUID;

public class SiteWithoutCountryException extends RuntimeException {

    public SiteWithoutCountryException(final UUID siteId) {
        super("Found site without a country, siteId=" + siteId);
    }
}
