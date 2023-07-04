package nl.ing.lovebird.sitemanagement.exception;

public class UserNotAllowedToAccessExperimentalSites extends RuntimeException {

    public UserNotAllowedToAccessExperimentalSites(final String message) {
        super(message);
    }
}
