package nl.ing.lovebird.sitemanagement.clientconfiguration;

/**
 * Exception to inform the client what was wrong.
 * Note that the message is presented to the client, and should only have information to help the client in order to get a correct request.
 */
public class ClientConfigurationValidationException extends RuntimeException {

    /**
     * Constructor with message argument. This message will be presented in the response. You can notify the client about errors in the
     * response. Make sure you don't put sensitive information in this exception.
     * @param message The message that will be presented in the response.
     */
    public ClientConfigurationValidationException(String message) {
        super(message);
    }
}
