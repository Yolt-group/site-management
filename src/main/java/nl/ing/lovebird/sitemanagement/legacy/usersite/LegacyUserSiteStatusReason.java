package nl.ing.lovebird.sitemanagement.legacy.usersite;

import nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteNeededAction;

/**
 * When any of the reasons are set on a user site, there will be an error displayed in the app for that specific user site with a button.
 * These buttons are the result of a {@link UserSiteNeededAction}, which is determined in {@link UserSiteDerivedAttributes}.
 */
@Deprecated
public enum LegacyUserSiteStatusReason {

    // These statuses are used by the system, as measured between 2021-01-15 15:00 -- 2021-01-18 14:00
    // GENERIC_ERROR
    // NEW_LOGIN_INFO_NEEDED
    // NO_SUPPORTED_ACCOUNTS (removed since 2021-11-02)
    // SITE_ACTION_NEEDED
    // STEP_TIMED_OUT
    // TOKEN_EXPIRED

    /**
     * Displays the error: 'The credentials you provided for [bank] were not accepted by your bank. Please try again.'.
     */
    INCORRECT_CREDENTIALS("The credentials were invalid."),

    /**
     * Displays the error: 'The credentials you provided for [bank] may have expired. Please check on their website.'.
     */
    EXPIRED_CREDENTIALS("The credentials expired."),

    /**
     * Displays the error: 'The extra information you supplied for [bank] seems to be incorrect.'.
     */
    INCORRECT_ANSWER("An MFA answer was answered wrong."),

    /**
     * Displays the error: 'You're unable to access [bank]. Please check your account on their website.'.
     */
    ACCOUNT_LOCKED("The account is locked. The user needs to contact the site to unlock it."),

    /**
     * Displays the error: 'There may be a technical problem at [bank] preventing us from synchronising you account. Please try again later..'.
     */
    SITE_ERROR("There was an error with the site (website down for example)."),

    /**
     * Displays the error: 'Unfortunately, we're unable to synchronise your [bank] account at this time.'.
     */
    GENERIC_ERROR("Generic error."),

    /**
     * Displays the error: 'Logged into your [bank] account elsewhere? Please log out and try again.'.
     */
    MULTIPLE_LOGINS("The user logged in to the site in another session. It needs to end this session first."),

    /**
     * Displays the error: 'Please sign in to [bank] directly before trying again within Yolt.'.
     */
    SITE_ACTION_NEEDED("The user needs to login to the site manually to take some acton."),

    /**
     * Displays the error: 'Your [bank] account is set to an unsupported language which prevents us from synchronising your account. Please set to English and try again.'.
     */
    UNSUPPORTED_LANGUAGE("The user needs to switch the language to a supported language on the site."),

    /**
     * Displays the error: 'We need a new set of credentials to log in to [bank] and synchronise your accounts.'.
     */
    NEW_LOGIN_INFO_NEEDED("There is extra data needed to login the user. This needs to be submitted by the user."),

    /**
     * Displays the error: 'Your details for [bank] were not entered within the required time. Please try again.'.
     */
    STEP_TIMED_OUT("The time to submit the next step expired."),

    /**
     * Displays the error: 'Please try again and double check you logged in with the right [bank] account.'.
     */
    WRONG_CREDENTIALS("The used credentials do not allow access to the request resource."),

    /**
     * Displays the error: 'Please log in with your [bank] account to get Yolt up to speed.'.
     */
    TOKEN_EXPIRED("The user's access is expired."),

    /**
     * Displays the error: 'Unfortunately, we're unable to log in and synchronise your [bank] account at this time.'.
     */
    STEP_NOT_SUPPORTED("We do not support submitting this step for this site."),

    /**
     * This status is set from site-management automatically if applicable.
     */
    CONSENT_EXPIRED("Consent of the user is expired at external site. User needs to renew the consent.");

    final String description;

    LegacyUserSiteStatusReason(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("%s : %s", name(), description);
    }

}
