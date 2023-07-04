package nl.ing.lovebird.sitemanagement.exception;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.errorhandling.ErrorInfo;
import nl.ing.lovebird.errorhandling.ExceptionHandlingService;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientConfigurationValidationException;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlNotFoundException;
import nl.ing.lovebird.sitemanagement.forms.FormValidationException;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static nl.ing.lovebird.errorhandling.BaseErrorConstants.METHOD_ARGUMENT_NOT_VALID;
import static nl.ing.lovebird.sitemanagement.exception.ErrorConstants.*;

/**
 * Contains handlers for predefined exception.
 */
@ControllerAdvice
@ResponseBody
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExceptionHandlers {

    private final ExceptionHandlingService exceptionHandlingService;
    private final String prefix;

    public ExceptionHandlers(ExceptionHandlingService exceptionHandlingService, @Value("${yolt.commons.error-handling.prefix}") String prefix) {
        this.exceptionHandlingService = exceptionHandlingService;
        this.prefix = prefix;
    }

    @ExceptionHandler(UserSiteDeleteException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDTO handle(final UserSiteDeleteException e) {
        return exceptionHandlingService.logAndConstruct(USER_SITE_DELETE_FAILED, e);
    }

    @ExceptionHandler(UserSiteIsNotMarkedAsDeletedException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDTO handle(final UserSiteIsNotMarkedAsDeletedException e) {
        return exceptionHandlingService.logAndConstruct(DELETING_NOT_MARKED_RESOURCE, e);
    }

    @ExceptionHandler(InvalidMigrationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final InvalidMigrationException e) {
        // WARN level: the client is trying to migrate a site that is not marked for migration
        return exceptionHandlingService.logAndConstruct(Level.WARN, MIGRATION_ERROR, e);
    }

    @ExceptionHandler(AlreadyMigratedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final AlreadyMigratedException e) {
        // WARN level: the client is trying to migrate an account that was migrated before
        return exceptionHandlingService.logAndConstruct(Level.WARN, ACCOUNT_MIGRATION, e);
    }

    @ExceptionHandler(FromAccountScraperException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final FromAccountScraperException e) {
        // WARN level: the client is trying to migrate an account that is not using a scraper connection
        return exceptionHandlingService.logAndConstruct(Level.WARN, FROM_ACCOUNT_SCRAPER, e);
    }

    @ExceptionHandler(ToAccountOpenBankingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final ToAccountOpenBankingException e) {
        // WARN level: the client is trying to migrate to an account that is not using an open banking connection
        return exceptionHandlingService.logAndConstruct(Level.WARN, TO_ACCOUNT_OPEN_BANKING, e);
    }

    @ExceptionHandler(WrongSiteGroupingByException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final WrongSiteGroupingByException e) {
        // WARN level: the client is trying to migrate to an account with a different site grouping from the new account
        return exceptionHandlingService.logAndConstruct(Level.WARN, DIFFERENT_SITE_GROUPING, e);
    }

    @ExceptionHandler(InvalidMigrationGroupException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDTO handle(final InvalidMigrationGroupException e) {
        return exceptionHandlingService.logAndConstruct(NO_MIGRATION_GROUP_FOUND, e);
    }

    @ExceptionHandler(SiteConsentTemplateNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final SiteConsentTemplateNotFoundException e) {
        return exceptionHandlingService.logAndConstruct(NO_CONSENT_TEMPLATE_FOR_SITE, e);
    }

    @ExceptionHandler(UnknownCountryCodeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final UnknownCountryCodeException e) {
        return exceptionHandlingService.logAndConstruct(COUNTRY_CODE_UNKNOWN, e);
    }

    @ExceptionHandler(CountryCodeIsNullException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final CountryCodeIsNullException e) {
        return exceptionHandlingService.logAndConstruct(COUNTRY_CODE_REQUIRED, e);
    }

    @ExceptionHandler(MissingSiteIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final MissingSiteIdException e) {
        return exceptionHandlingService.logAndConstruct(SITE_ID_REQUIRED, e);
    }

    @ExceptionHandler(MissingStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final MissingStateException e) {
        return exceptionHandlingService.logAndConstruct(Level.WARN, STATE_REQUIRED, e);
    }

    @ExceptionHandler(NoSessionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final NoSessionException e) {
        return exceptionHandlingService.logAndConstruct(Level.WARN, NO_USER_SESSION, e);
    }

    @ExceptionHandler(UserSiteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handle(final UserSiteNotFoundException e) {
        // this is actually a normal event
        // so we log it as INFO instead of ERROR
        // so that people "on Standby" are not alerted in this event
        return exceptionHandlingService.logAndConstruct(Level.INFO, USER_SITE_NOT_FOUND, e);
    }

    @ExceptionHandler(SiteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handle(final SiteNotFoundException e) {
        return exceptionHandlingService.logAndConstruct(SITE_ID_NOT_FOUND, e);
    }

    @ExceptionHandler(UserNotAllowedToAccessExperimentalSites.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorDTO handle(final UserNotAllowedToAccessExperimentalSites e) {
        return exceptionHandlingService.logAndConstruct(ACCESS_TO_EXPERIMENTAL_SITES_NOT_ALLOWED, e);
    }


    @ExceptionHandler(ClientCountryCombinationNotAllowed.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorDTO handle(final ClientCountryCombinationNotAllowed e) {
        return exceptionHandlingService.logAndConstruct(UNSUPPORTED_CLIENT_COUNTRY_COMBINATION, e);
    }

    @ExceptionHandler(UnexpectedJsonElementException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDTO handle(final UnexpectedJsonElementException e) {
        return exceptionHandlingService.logAndConstruct(UNEXPECTED_JSON_PROCESSING_ERROR, e);
    }

    @ExceptionHandler(ClientConfigurationValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final ClientConfigurationValidationException e) {
        ErrorInfo errorInfo = new ErrorInfo() {
            @Override
            public String getCode() {
                return CLIENT_CONFIGURATION_VALIDATION_ERROR.getCode();
            }

            @Override
            public String getMessage() {
                return e.getMessage();
            }
        };
        return exceptionHandlingService.logAndConstruct(errorInfo, e);
    }

    @ExceptionHandler(FormValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleUserSiteStatusNotMfaNeededException(final FormValidationException e) {
        return exceptionHandlingService.logAndConstruct(Level.INFO, new ErrorInfo() {
            @Override
            public String getCode() {
                return FORM_VALIDATION_EXCEPTION.getCode();
            }

            @Override
            public String getMessage() {
                return e.getMessage();
            }
        }, e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        InputErrorDTO inputErrorDTO = new InputErrorDTO(prefix + METHOD_ARGUMENT_NOT_VALID.getCode(), METHOD_ARGUMENT_NOT_VALID.getMessage());
        e.getBindingResult().getFieldErrors()
                .forEach(fieldError -> inputErrorDTO.addFieldError(fieldError.getObjectName() + "." + fieldError.getField() + " : " + fieldError.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors()
                .forEach(globalError -> inputErrorDTO.addGlobalError(globalError.getObjectName() + " : " + globalError.getDefaultMessage()));
        log.warn("Method argument exception. Returning {} with {} errors",
                METHOD_ARGUMENT_NOT_VALID.getMessage(),
                inputErrorDTO.getFieldErrors().size() + inputErrorDTO.getGlobalErrors().size(), e);
        return inputErrorDTO;
    }

    @ExceptionHandler(SiteWithoutCountryException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDTO handleSiteWithoutCountryException(final SiteWithoutCountryException e) {
        return exceptionHandlingService.logAndConstruct(SITE_WITHOUT_COUNTRY, e);
    }

    @ExceptionHandler(ProviderNotEnabledException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleProviderNotEnabledException(final ProviderNotEnabledException e) {
        return exceptionHandlingService.logAndConstruct(PROVIDER_NOT_ENABLED, e);
    }

    @ExceptionHandler(ProviderNotEnabledForServiceTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final ProviderNotEnabledForServiceTypeException e) {
        ErrorInfo errorInfo = switch (e.getServiceType()) {
            case AIS -> SITE_AIS_NOT_ENABLED_FOR_URL;
            case PIS -> SITE_PIS_NOT_ENABLED_FOR_URL;
            case AS, IC -> CLIENT_SITE_NOT_ENABLED; // We don't really do AS/IC..
        };
        return exceptionHandlingService.logAndConstruct(Level.WARN, errorInfo, e);
    }

    @ExceptionHandler(ClientSiteNotEnabledException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final ClientSiteNotEnabledException e) {
        return exceptionHandlingService.logAndConstruct(Level.WARN, CLIENT_SITE_NOT_ENABLED, e);
    }

    @ExceptionHandler(ClientRedirectUrlNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handleClientRedirectUrlNotFoundException(final ClientRedirectUrlNotFoundException e) {
        return exceptionHandlingService.logAndConstruct(CLIENT_REDIRECT_URL_NOT_FOUND, e);
    }

    @ExceptionHandler(KnownProviderRestClientException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorDTO handleKnownProviderRestClientException(final KnownProviderRestClientException e) {
        //Known errors from providers are already logged within the providers service as errors. To avoid double
        //logging of them, we scale down to info here.
        return exceptionHandlingService.logAndConstruct(Level.INFO, PROVIDERS_KNOWN_ERROR, e);
    }

    @ExceptionHandler(UserSiteStatusNotMfaNeededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleUserSiteStatusNotMfaNeededException(final UserSiteStatusNotMfaNeededException e) {
        return exceptionHandlingService.logAndConstruct(Level.INFO, USER_SITE_STATUS_NOT_MFA_NEEDED, e);
    }

    @ExceptionHandler(StateAlreadySubmittedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final StateAlreadySubmittedException e) {
        return exceptionHandlingService.logAndConstruct(Level.WARN, STATE_ALREADY_SUBMITTED, e);
    }

    @ExceptionHandler(StateOverwrittenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final StateOverwrittenException e) {
        return exceptionHandlingService.logAndConstruct(Level.WARN, STATE_OVERWRITTEN, e);
    }

    @ExceptionHandler(ConsentSessionExpiredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final ConsentSessionExpiredException e) {
        return exceptionHandlingService.logAndConstruct(Level.WARN, STATE_EXPIRED, e);
    }

    @ExceptionHandler(NoRedirectUrlIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final NoRedirectUrlIdException e) {
        return exceptionHandlingService.logAndConstruct(Level.WARN, MISSING_ARGUMENT_REDIRECT_URL_ID, e);
    }

    @ExceptionHandler(InvalidRedirectUrlException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final InvalidRedirectUrlException e) {
        return exceptionHandlingService.logAndConstruct(Level.WARN, INVALID_REDIRECT_URL, e);
    }

    @ExceptionHandler(FunctionalityUnavailableForOneOffAISUsersException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handle(final FunctionalityUnavailableForOneOffAISUsersException e) {
        return exceptionHandlingService.logAndConstruct(Level.WARN, FUNCTIONALITY_NOT_AVAILABLE_FOR_ONE_OFF_AIS_USERS, e);
    }

}
