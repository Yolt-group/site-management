package nl.ing.lovebird.sitemanagement.exception;

public class ProviderNotEnabledForRedirectUrlIdException extends RuntimeException {

    public ProviderNotEnabledForRedirectUrlIdException(final String message) {
        super(message);
    }
}