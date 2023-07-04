package nl.ing.lovebird.sitemanagement.exception;

public class StateOverwrittenException extends RuntimeException {
    public StateOverwrittenException(String message) {
        super(message);
    }
}
