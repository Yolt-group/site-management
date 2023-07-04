package nl.ing.lovebird.sitemanagement.exception;

public class UserSiteDeleteException extends RuntimeException {

    public UserSiteDeleteException(final String message, final Exception e) {
        super(message, e);
    }

}
