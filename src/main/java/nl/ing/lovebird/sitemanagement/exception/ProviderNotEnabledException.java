package nl.ing.lovebird.sitemanagement.exception;

public class ProviderNotEnabledException extends RuntimeException {

    public ProviderNotEnabledException(final String message) {
        super(message);
    }
}
