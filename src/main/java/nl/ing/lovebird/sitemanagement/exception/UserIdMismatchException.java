package nl.ing.lovebird.sitemanagement.exception;

import java.util.UUID;

public class UserIdMismatchException extends Exception {
    private final UUID clientUserTokenUserId;
    private final UUID pathUserId;

    public UserIdMismatchException(final UUID clientUserTokenUserId, final UUID pathUserId) {
        this.clientUserTokenUserId = clientUserTokenUserId;
        this.pathUserId = pathUserId;
    }

    @Override
    public String getMessage() {
        return String.format("UserId from client-user-token %s is not equal to UserId from URL %s", clientUserTokenUserId, pathUserId);
    }
}
