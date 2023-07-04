package nl.ing.lovebird.sitemanagement.exception;

import nl.ing.lovebird.errorhandling.ErrorInfo;

public enum ErrorConstants implements ErrorInfo {
    COUNTRY_CODE_UNKNOWN("002", "Unknown country code"),
    COUNTRY_CODE_REQUIRED("004", "Country code is required"),
    SITE_ID_REQUIRED("006", "SiteEntity id is required"),
    USER_SITE_NOT_FOUND("008", "User-site not found"),
    SITE_ID_NOT_FOUND("010", "SiteEntity ID not found"),
    USER_SITE_STATUS_NOT_MFA_NEEDED("011", "User site status is not STEP_NEEDED"),
    ACCESS_TO_EXPERIMENTAL_SITES_NOT_ALLOWED("018", "User is not allowed to access experimental sites"),
    UNSUPPORTED_CLIENT_COUNTRY_COMBINATION("020", "User is not allowed to access sites for this country."),
    STATE_REQUIRED("021", "State is required"),
    UNEXPECTED_JSON_PROCESSING_ERROR("024", "An error occurred fetching and/or parsing the form."),
    NO_CONSENT_TEMPLATE_FOR_SITE("025", "Consent template not found for site."),
    MIGRATION_ERROR("027", "User-site needs to be in a group, but isn't."),
    NO_MIGRATION_GROUP_FOUND("028", "Could not find sites in the same group for migration."),
    CLIENT_CONFIGURATION_VALIDATION_ERROR("029", "The client configuration is invalid."),
    CLIENT_REDIRECT_URL_NOT_FOUND("030", "The client redirect url could not be found."),
    SITE_WITHOUT_COUNTRY("032", "Found site without a country code."),
    PROVIDER_NOT_ENABLED("033", "The site provider is not enabled. Enable the provider by providing authentication means before continuing."),
    PROVIDERS_KNOWN_ERROR("034", "Got an error from providers."),
    DELETING_NOT_MARKED_RESOURCE("039", "Trying to delete a resource which is not marked for deletion"),
    USER_SITE_DELETE_FAILED("040", "Failed to delete user-site"),
    NO_USER_SESSION("042", "Session unknown. Can't find a related session for the posted redirect URL or form."),
    // Message will be overwritten by message from the exception to allow for more useful dynamic (validation) messages to the user.
    FORM_VALIDATION_EXCEPTION("043", null),
    STATE_ALREADY_SUBMITTED("044", "This stateId in the posted redirectUrl or form is already submitted."),
    STATE_OVERWRITTEN("045", "The session/state is invalid since another session has been created already."),
    STATE_INCORRECT("047", "State incorrect"),
    STATE_EXPIRED("048", "State is expired."),
    ACCOUNT_MIGRATION("050", "One of the accounts you're trying to migrate was already migrated"),
    FROM_ACCOUNT_SCRAPER("051", "The account you're trying to migrate is not using a scraper connection"),
    TO_ACCOUNT_OPEN_BANKING("052", "The account you're trying to migrate to it is not using an open banking connection"),
    DIFFERENT_SITE_GROUPING("054", "The account you're trying to migrate doesn't have the same site grouping as the new account"),
    MISSING_ARGUMENT_REDIRECT_URL_ID("056", "The redirect url id is required for this action."),
    SITE_AIS_NOT_ENABLED_FOR_URL("057", "The AIS service for this site is not enabled for the given redirect url id."),
    SITE_PIS_NOT_ENABLED_FOR_URL("063", "The PIS service for this site is not enabled for the given redirect url id."),
    WRONG_USER_ID("064", "The wrong userId provided."),
    INVALID_REDIRECT_URL("065", "The redirect URL is invalid."),
    FUNCTIONALITY_NOT_AVAILABLE_FOR_ONE_OFF_AIS_USERS("072", "Functionality is not available for one-off AIS users."),
    CLIENT_SITE_NOT_ENABLED("075", "The site is not enabled.");

    private final String code;
    private final String message;

    ErrorConstants(final String code, final String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
