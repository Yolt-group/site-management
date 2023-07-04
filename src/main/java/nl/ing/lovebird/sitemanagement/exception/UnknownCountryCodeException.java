package nl.ing.lovebird.sitemanagement.exception;

public class UnknownCountryCodeException extends RuntimeException {

    public UnknownCountryCodeException(final String message) {
        super(message);
    }

}
