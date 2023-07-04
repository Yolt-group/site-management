package nl.ing.lovebird.sitemanagement.exception;

import java.io.IOException;

public class FailedToSerializeMfaFormException extends RuntimeException {

    public FailedToSerializeMfaFormException(IOException e) {
        super(e);
    }

}
