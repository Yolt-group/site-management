package nl.ing.lovebird.sitemanagement.exception;

public class UserSiteIsNotMarkedAsDeletedException extends RuntimeException {
    public UserSiteIsNotMarkedAsDeletedException(final String message) {
        super(message);
    }
}
