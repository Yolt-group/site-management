package nl.ing.lovebird.sitemanagement.exception;

public abstract class InvalidRedirectUrlException extends RuntimeException {

    public InvalidRedirectUrlException(String message) {
        super(message);
    }
}
