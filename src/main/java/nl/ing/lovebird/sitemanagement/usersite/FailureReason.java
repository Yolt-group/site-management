package nl.ing.lovebird.sitemanagement.usersite;

public enum FailureReason {
    /**
     * We failed to fetch data because of a technical error, probable causes:
     * - a bank is unreachable / returns an error / is in maintenance
     * - an internal error occurred within the Yolt systems
     */
    TECHNICAL_ERROR,
    /**
     * We failed to fetch data because the user needs to do something on the website of the bank.
     * THIS IS ONLY APPLICABLE FOR SCRAPERS.
     * This is due to the fact that the scraper can't login on behalf of the user because the website shows, for example, a popup with
     * new terms and agreements.
     */
    ACTION_NEEDED_AT_SITE,
    /**
     * A functional error occurred during the consent flow.
     */
    AUTHENTICATION_FAILED,
    /**
     * We failed to authenticate at the bank because the consent has expired.  The end-user needs to renew their consent.
     */
    CONSENT_EXPIRED,
}
