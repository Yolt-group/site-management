package nl.ing.lovebird.sitemanagement.exception;

public class NoSessionException extends RuntimeException {

    public NoSessionException(final String message) {
        super(message);
    }

    public NoSessionException() {
        super();
    }

}
