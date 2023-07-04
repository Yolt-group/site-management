package nl.ing.lovebird.sitemanagement.exception;

/**
 * A client posted an invalid redirect URI to us.
 */
public class InvalidAISRedirectUrlException extends InvalidRedirectUrlException {

    public InvalidAISRedirectUrlException(String message) {
        super(message);
    }
}
