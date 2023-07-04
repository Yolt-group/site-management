package nl.ing.lovebird.sitemanagement.exception;

public class ConsentSessionExpiredException extends RuntimeException {

    public ConsentSessionExpiredException(final String message) {
        super(message);
    }

}
