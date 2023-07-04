package nl.ing.lovebird.sitemanagement.exception;

public class StateAlreadySubmittedException extends RuntimeException {
    public StateAlreadySubmittedException(String message) {
        super(message);
    }
}
