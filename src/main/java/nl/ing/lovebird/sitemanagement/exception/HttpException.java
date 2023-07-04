package nl.ing.lovebird.sitemanagement.exception;

import lombok.Getter;

@Getter
public class HttpException extends Exception {
    int httpStatusCode;
    String functionalErrorCode;

    public HttpException(int httpStatusCode, String functionalErrorCode) {
        super("http-status-code: " + httpStatusCode + " function-code: " + functionalErrorCode);
        this.httpStatusCode = httpStatusCode;
        this.functionalErrorCode = functionalErrorCode;
    }
}