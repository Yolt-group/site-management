package nl.ing.lovebird.sitemanagement.exception;

/**
 * A 'known' exception coming from providers.
 * This should only be used when we get an 'errorDTO' from providers. We assume that providers now already logged the error.
 * We will not log any message on error level.
 */
public class KnownProviderRestClientException extends RuntimeException {

    public KnownProviderRestClientException(final HttpException e) {
        super(e);
    }

    public KnownProviderRestClientException(final String message) {
        super(message);
    }

}
