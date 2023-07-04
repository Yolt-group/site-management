package nl.ing.lovebird.sitemanagement.exception;

public class UserSiteNotFoundException extends RuntimeException {

    public UserSiteNotFoundException(final String message) {
        super(message);
    }

}
