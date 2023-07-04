package nl.ing.lovebird.sitemanagement.exception;

public class SiteNotFoundException extends RuntimeException {
    public SiteNotFoundException(final String message) {
        super(message);
    }
}
