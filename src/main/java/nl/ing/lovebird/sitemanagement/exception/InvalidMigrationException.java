package nl.ing.lovebird.sitemanagement.exception;

public class InvalidMigrationException extends RuntimeException {
    public InvalidMigrationException(final String message) {
        super(message);
    }
}
