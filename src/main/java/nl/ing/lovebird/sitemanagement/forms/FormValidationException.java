package nl.ing.lovebird.sitemanagement.forms;

public class FormValidationException extends Exception {
    /**
     * Warning: The message *is* exposed to the client to give hints about what is wrong with the form.
     * Please be aware that the message is sent, so make sure sensitive information isn't used while using this constructor.
     * @param message The message that will be presented to the user about what is wrong with the form.
     */
    public FormValidationException(String message) {
        super("Invalid form: " + message);
    }
}
