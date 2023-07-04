package nl.ing.lovebird.sitemanagement.exception;

import java.util.UUID;

public class ToAccountOpenBankingException extends RuntimeException {
    public ToAccountOpenBankingException(final UUID userSiteId, final UUID accountId) {
        super(String.format("Account: %s with user site: %s is not using an open banking connection.", accountId, userSiteId));
    }
}
