package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues;
import nl.ing.lovebird.providershared.form.Form;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.configuration.SiteManagementDebugProperties;
import nl.ing.lovebird.sitemanagement.exception.*;
import nl.ing.lovebird.sitemanagement.externalconsent.ExternalConsentService;
import nl.ing.lovebird.sitemanagement.forms.FormValidationException;
import nl.ing.lovebird.sitemanagement.forms.FormValidator;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSession;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.uuid.AISState;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.usersite.ProcessedStepResult.loginFailed;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType.CREATE_USER_SITE;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType.UPDATE_USER_SITE;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;

@RequiredArgsConstructor
@Service
@Slf4j
public class CreateOrUpdateUserSiteService {

    private static final String LOGIN_TYPE_NOT_SUPPORTED_MESSAGE = "Login type not supported: ";

    private final Clock clock;
    // This is not OK. Too much dependencies, rework this if you have the chance.
    private final ConsentSessionService userSiteSessionService;
    private final SiteService siteService;
    private final UserSiteProviderService userSiteProviderService;
    private final ClientRedirectUrlService clientRedirectUrlService;
    private final UserSiteService userSiteService;
    private final ExternalConsentService externalConsentService;
    private final SiteLoginService siteLoginService;
    private final ObjectMapper objectMapper;
    private final SiteManagementDebugProperties siteManagementDebugProperties;
    private final SitesProvider sitesProvider;
    private final UserService userService;

    @SuppressWarnings("squid:S3776")
    public ProcessedStepResult processPostedLogin(
            final Login login,
            final ClientUserToken clientUserToken,
            final String psuIpAddress
    ) throws FormValidationException {
        final Site site;
        final ConsentSession userSiteSession;
        if (login instanceof UrlLogin) {

            // Check the redirection URI that was posted to us.
            String redirectionURI = ((UrlLogin) login).getRedirectUrl();
            OAuth2RedirectionURI.RedirectionURIValidationResult url = OAuth2RedirectionURI.parse(redirectionURI);

            // The URI might be invalid, however as long as it contains state we continue processing because we need to update
            // the user-site accordingly and mark it as "login failed".  We can only continue if the state parameter is present
            // however, if that parameter is absent we stop processing immediately.
            if (StringUtils.isEmpty(url.getState())) {
                throw new MissingStateException("No state parameter in the query parameters. Unable to determine the context of this action. Url: " + redirectionURI);
            }

            try {
                userSiteSession = userSiteSessionService.findByStateIdAndRotateStateId(login.getUserId(), UUID.fromString(url.getState()));
                site = siteService.getSite(userSiteSession.getSiteId());
                log.info("User posted a redirect with stateId {} for site {} on {}.", url.getState(), site.getName(), ZonedDateTime.now(clock)); //NOSHERIFF
            } catch (NoSessionException e) {
                log.warn("An url was posted with a stateId that we have never seen before: \"{}\"", redirectionURI); //NOSHERIFF
                throw e;
            }

            // Fail if a URL login was posted for a form step
            if (userSiteSession.getFormStep() != null && userSiteSession.getRedirectUrlStep() == null) {
                log.warn("User posted a redirect step while a form step was expected for site {} with provider {}", site.getName(), site.getProvider());
                throw new IllegalArgumentException("Invalid login information for expected step.");
            }

            // We have an error, we should not continue if the user site was in a connected state before and instead restore its original state. See YCO-1344
            if (url.getError_code() != null && shouldRestoreUserSiteStatus(userSiteSession)) {
                final PostgresUserSite userSite = userSiteService.getUserSite(userSiteSession.getUserId(), userSiteSession.getUserSiteId());
                userSiteService.updateUserSiteStatus(userSite, userSiteSession.getOriginalConnectionStatus(), userSiteSession.getOriginalFailureReason(), null);
                return loginFailed(userSite.getUserSiteId());
            }

        } else if (login instanceof FormLogin formLogin) {
            userSiteSession = userSiteSessionService.findByStateIdAndRotateStateId(formLogin.getUserId(), formLogin.getStateId());
            site = siteService.getSite(userSiteSession.getSiteId());
            log.info("User posted a form with stateId {} for site {} on {}.", formLogin.getStateId(), site.getName(), ZonedDateTime.now(clock)); //NOSHERIFF

            // Fail if a form login was posted for a URL step
            if (userSiteSession.getFormStep() == null && userSiteSession.getRedirectUrlStep() != null) {
                log.warn("User posted a form step while a redirect step was expected for site {} with provider {}", site.getName(), site.getProvider());
                throw new IllegalArgumentException("Invalid login information for expected step.");
            }
        } else {
            throw new NotImplementedException(LOGIN_TYPE_NOT_SUPPORTED_MESSAGE + login.getClass());
        }

        try (LogBaggage b = LogBaggage.builder()
                .userId(clientUserToken.getUserIdClaim())
                .siteId(site.getId())
                .provider(site.getProvider())
                .build()) {
            final List<PostgresUserSite> allUserSitesForUser = userSiteService.getNonDeletedUserSites(login.getUserId());

            if (isPostedLoginForNextStep(userSiteSession)) {
                return handleNextStep(userSiteSession, login, clientUserToken, psuIpAddress);
            }

            // It can be the very first posted form/redirect to 'add-a-bank'.
            if (ConsentSession.Operation.CREATE_USER_SITE == userSiteSession.getOperation() && !isPostedLoginForNextStep(userSiteSession)) {
                return createUserSite(login, site, clientUserToken,
                        userSiteSession, psuIpAddress);
            }

            if (ConsentSession.Operation.UPDATE_USER_SITE != userSiteSession.getOperation()) {
                throw new IllegalStateException("We got a Form or Redirect, but no clue what to do with it.");
            }

            // It's an update.
            PostgresUserSite existingUserSite = allUserSitesForUser.stream()
                    .filter(it -> userSiteSession.getUserSiteId().equals(it.getUserSiteId()))
                    .findFirst()
                    .orElseThrow(() -> new UserSiteNotFoundException("Couldn't find usersite with userSiteId " + userSiteSession.getUserSiteId()));

            if (isScrapingSite(userSiteSession.getProvider())) {
                return updateUserSiteForScrapingProvider((FormLogin) login, clientUserToken, userSiteSession);
            } else {
                return updateUserSiteForDirectConnectionProvider(login, site, existingUserSite, clientUserToken, userSiteSession, psuIpAddress);
            }
        }
    }

    private boolean shouldRestoreUserSiteStatus(final ConsentSession userSiteSession) {
        // Assume no original data is present when the original status is null
        return userSiteSession.getOriginalConnectionStatus() != null;
    }

    private boolean isPostedLoginForNextStep(ConsentSession userSiteSession) {
        return userSiteSession.getStepNumber() > 0;
    }

    private ProcessedStepResult handleNextStep(
            ConsentSession userSiteSession,
            Login login,
            final ClientUserToken clientUserToken,
            final String psuIpAddress
    ) throws FormValidationException {
        PostgresUserSite userSite = userSiteService.getUserSite(clientUserToken.getUserIdClaim(), userSiteSession.getUserSiteId());

        validateUserSiteStatusStepNeeded(userSite);

        if (userSiteSession.getFormStep() != null) {
            // If we're dealing with a posted form, validateValues it.
            validateFormStep(userSiteSession, (FormLogin) login);
        }

        if (!userSiteService.attemptLock(userSite, userSiteSession.getActivityId())) {
            // This should not happen, log it anyway for good measure.
            log.error("Failed to lock user-site {} when submitting a step. The user-site should not be locked as it cannot be refreshed right now.", userSite.getUserSiteId());
        }

        if (!isScrapingSite(userSite.getProvider())) {

            String baseClientRedirectUrl = clientRedirectUrlService.getBaseClientRedirectUrlOrThrow(clientUserToken, userSiteSession.getRedirectUrlId());

            return userSiteProviderService.processStepForDirectConnectionProvider(
                    login,
                    userSite,
                    userSiteSession,
                    psuIpAddress,
                    clientUserToken,
                    CREATE_USER_SITE,
                    baseClientRedirectUrl
            );
        } else {
            if (userSiteSession.getRedirectUrlStep() != null) {
                // We currently only support redirect urls for direct (API) providers.
                throw new UnsupportedOperationException("We don't support redirect URL steps from scraping providers at this moment");
            }
            //At the moment we only support MFA of type FORM.
            final FilledInUserSiteFormValues filledInUserSiteFormValues = new FilledInUserSiteFormValues();
            ((FormLogin) login).getFilledInUserSiteFormValues()
                    .getValueMap()
                    .forEach(filledInUserSiteFormValues::add);

            final FormLogin mfaLogin = new FormLogin(clientUserToken.getUserIdClaim(),
                    filledInUserSiteFormValues, userSiteSession.getStateId());

            return submitMfa(mfaLogin, userSiteSession, clientUserToken, userSiteSession.getUserSiteId());
        }
    }

    private void validateFormStep(ConsentSession session, FormLogin login) throws FormValidationException {
        try {
            FormStep formStep = objectMapper.readValue(session.getFormStep(), FormStep.class);
            Form form = formStep.getForm();
            new FormValidator(false, siteManagementDebugProperties).validateValues(form, login.getFilledInUserSiteFormValues());
        } catch (IOException e) {
            throw new RuntimeException("Invalid form.", e);
        }
    }

    @SuppressWarnings("squid:S00107")
    private ProcessedStepResult createUserSite(
            final Login login,
            final Site site,
            final ClientUserToken clientUserToken,
            final ConsentSession userSiteSession,
            final String psuIpAddress
    ) {
        final UUID userId = login.getUserId();

        final PostgresUserSite userSite = new PostgresUserSite(
                userId, userSiteSession.getUserSiteId(), site.getId(), null, ConnectionStatus.DISCONNECTED, null, null, Instant.now(clock), Instant.now(clock), null, new ClientId(clientUserToken.getClientIdClaim()), site.getProvider(), null, null, null, false, null
        );

        if (userSiteSession.getExternalConsentId() != null) {
            externalConsentService.createOrUpdateConsent(userId, site, userSite.getUserSiteId(), userSiteSession.getExternalConsentId());
        }

        String baseClientRedirectUrl = null;
        if (!isScrapingSite(site.getProvider())) {
            userSite.setRedirectUrlId(userSiteSession.getRedirectUrlId());

            baseClientRedirectUrl = clientRedirectUrlService.getBaseClientRedirectUrlOrThrow(clientUserToken, userSiteSession.getRedirectUrlId());
        }
        userSiteService.createNew(userSite);

        log.info("Created new user site with id {}", userSite.getUserSiteId());

        final UUID activityId = userSiteSession.getActivityId();
        if (!userSiteService.attemptLock(userSite, activityId)) {
            // This should not happen, log it anyway for good measure.
            log.error("Failed to lock user-site {} immediately after creating it, should not happen.", userSite.getUserSiteId());
        }

        if (!isScrapingSite(site.getProvider())) {
            return userSiteProviderService.processStepForDirectConnectionProvider(
                    login,
                    userSite,
                    userSiteSession,
                    psuIpAddress,
                    clientUserToken,
                    CREATE_USER_SITE,
                    baseClientRedirectUrl
            );
        } else {
            return userSiteProviderService.createUserSiteForScrapingProvider(login, site, userSite, activityId, clientUserToken);
        }
    }

    public ProcessedStepResult updateUserSiteForScrapingProvider(final FormLogin formLogin, final ClientUserToken clientUserToken, final ConsentSession userSiteSession) {
        UUID userId = formLogin.getUserId();
        final PostgresUserSite userSite = userSiteService.getUserSite(userId, userSiteSession.getUserSiteId());
        UUID activityId = userSiteSession.getActivityId();

        try (LogBaggage b = new LogBaggage(userSite)) {
            final Site site = siteService.getSite(userSite.getSiteId());

            checkIfNotDeleted(userSite);
            if (!userSiteService.attemptLock(userSite, activityId)) {
                log.warn("updateUserSiteRequestForScrapingProvider: failed to acquire user-site-lock for user-site {}, continuing anyway.", userSite.getUserSiteId());
            }

            // Something went wrong during the creation of a user-site, a user-site was never created at a scraping provider.
            if (userSite.getExternalId() == null) {
                log.info("Got an update on a user-site that has no external id yet. Will create a new one...");
                return userSiteProviderService.createUserSiteForScrapingProvider(
                        formLogin,
                        site,
                        userSite,
                        activityId,
                        clientUserToken
                );
            }

            return userSiteProviderService.updateUserSiteForScrapingProvider(formLogin, userSite, site, activityId, clientUserToken);
        }
    }

    private ProcessedStepResult updateUserSiteForDirectConnectionProvider(
            Login login,
            Site site,
            PostgresUserSite userSite,
            ClientUserToken clientUserToken,
            ConsentSession userSiteSession,
            final String psuIpAddress
    ) {
        final UUID activityId = userSiteSession.getActivityId();
        UUID userId = login.getUserId();

        checkIfNotDeleted(userSite);
        if (!userSiteService.attemptLock(userSite, activityId)) {
            log.warn("updateUserSite: failed to acquire user-site-lock for user-site {}, continuing anyway.", userSite.getUserSiteId());
        }

        if (login instanceof UrlLogin) {
            UrlLogin urlLogin = (UrlLogin) login;
            // This could happen when a usersite is updated in another application of the same client. e.g. via app instead of browser.
            // In that case we used different redirectUrl and authentication means on the authentication process.
            // During a refresh we should again use these different, new, authentication means.
            userSiteService.updateRedirectUrlId(userSite, userSiteSession.getRedirectUrlId());

            // Check the redirection URI that was posted to us.
            OAuth2RedirectionURI.RedirectionURIValidationResult url = OAuth2RedirectionURI.parse(urlLogin.getRedirectUrl());

            if (StringUtils.isEmpty(url.getState())) {
                throw new MissingStateException("no state parameter found in redirect url: " + urlLogin.getRedirectUrl());
            }
            if (!url.isValid()) {
                throw new InvalidAISRedirectUrlException("Invalid redirection URI posted: " + url);
            }
            if (userSiteSession.getExternalConsentId() != null) {
                externalConsentService.createOrUpdateConsent(userId, site, userSite.getUserSiteId(), userSiteSession.getExternalConsentId());
            }
        }

        String baseClientRedirectUrl = clientRedirectUrlService.getBaseClientRedirectUrlOrThrow(clientUserToken, userSiteSession.getRedirectUrlId());

        return userSiteProviderService.processStepForDirectConnectionProvider(
                login,
                userSite,
                userSiteSession,
                psuIpAddress,
                clientUserToken,
                UPDATE_USER_SITE,
                baseClientRedirectUrl
        );
    }

    public ProcessedStepResult submitMfa(
            final FormLogin formLogin,
            final ConsentSession userSiteSession,
            final ClientUserToken clientUserToken,
            final UUID userSiteId
    ) {
        UUID userId = formLogin.getUserId();
        final PostgresUserSite userSite = userSiteService.getUserSite(userId, userSiteId);
        try (LogBaggage b = new LogBaggage(userSite)) {

            validateUserSiteStatusStepNeeded(userSite);
            final Site site = siteService.getSite(userSite.getSiteId());
            final UUID activityId = userSiteSession.getActivityId();
            if (!userSiteService.attemptLock(userSite, activityId)) {
                // It would be very user unfriendly to throw an exception here, since he is now just going trough an MFA flow.
                // The form should end up at the dataprovider.
                log.warn("The user submitted MFA, but the usersite was already locked.");
            }

            return userSiteProviderService.submitMfa(site, formLogin, userSite, userSiteSession, clientUserToken);
        }
    }

    private void validateUserSiteStatusStepNeeded(final PostgresUserSite userSite) {
        ConnectionStatus userSiteStatus = userSite.getConnectionStatus();
        if (ConnectionStatus.STEP_NEEDED != userSiteStatus) {
            throw new UserSiteStatusNotMfaNeededException(
                    String.format("User site %s has the wrong status: %s", userSite.getUserSiteId(), userSiteStatus));
        }
    }

    private void checkIfNotDeleted(final PostgresUserSite userSite) {
        if (userSite.isDeleted()) {
            final String message = String.format("Got a request to refresh a user-site that is in deleting status: %s",
                    userSite.getUserSiteId());
            throw new UserSiteNotFoundException(message);
        }
    }

    public Step createLoginStepToRenewAccess(
            ClientUserToken clientUserToken,
            UUID userSiteId,
            UUID clientRedirectUrlId,
            final String psuIpAddress
    ) {
        var userSite = userSiteService.getUserSite(clientUserToken.getUserIdClaim(), userSiteId);
        if (userSite.getLastDataFetch() != null && clientUserToken.hasOneOffAIS() && userService.getUser(clientUserToken.getUserIdClaim()).map(User::isOneOffAis).orElse(false)) {
            throw new FunctionalityUnavailableForOneOffAISUsersException();
        }

        var site = sitesProvider.findByIdOrThrow(userSite.getSiteId());
        var externalConsentId = externalConsentService.getExternalConsentIdForValidExternalConsent(userSite);

        var sessionStateId = AISState.random();
        var step = siteLoginService.getFirstStep(clientUserToken, site, sessionStateId, clientRedirectUrlId, externalConsentId, psuIpAddress);

        var providerState = step.getProviderState();
        if (step instanceof RedirectStep redirectStep) {
            externalConsentId = redirectStep.getExternalConsentId();
        }

        // Start a ConsentSession to update a IUserSite.
        var userSiteSession = userSiteSessionService.createConsentSessionForRenewAccess(
                sessionStateId,
                new ClientId(clientUserToken.getClientIdClaim()),
                userSite,
                step,
                clientRedirectUrlId,
                providerState,
                externalConsentId
        );

        // In some cases we can complete a FormStep 'automatically' without user intervention.
        if (step instanceof FormStep && userSite.getPersistedFormStepAnswers() != null) {
            final FormStep formStep = (FormStep) step;

            if (!UserSitePersistedFormAnswersService.canCompleteFormStepWithoutUserIntervention(formStep, userSite)) {
                log.info("FormStep autocomplete: failed. (provider={}, haveFields={}, needFields={}).",
                        site.getProvider(),
                        userSite.getPersistedFormStepAnswers().keySet(),
                        UserSitePersistedFormAnswersService.listFormFieldKeys(formStep.getForm())
                ); //NOSHERIFF
                return step;
            }

            // We will now complete the step automatically using the stored values for the answers to the FormStep.
            final FilledInUserSiteFormValues formValues = new FilledInUserSiteFormValues();
            formValues.setValueMap(userSite.getPersistedFormStepAnswers());
            final FormLogin login = new FormLogin(userSite.getUserId(), formValues, formStep.getStateId());
            ProcessedStepResult nextStep = updateUserSiteForDirectConnectionProvider(
                    login,
                    site,
                    userSite,
                    clientUserToken,
                    userSiteSession,
                    psuIpAddress
            );
            if (nextStep.getStep() == null) {
                // We've just auto-completed a FormStep and the result were (apparently) renewed AccessMeans, otherwise there would be a Step instead of null.
                // This is perhaps theoretically possible but shouldn't occur in practice: *some* user action is always required when renewing access means.
                throw new IllegalStateException("Auto-completed a FormStep, expected a Step but didn't get one.  Should not happen.");
            }

            log.info("FormStep autocomplete: succeeded. (provider={}, haveFields={}).", site.getProvider(), userSite.getPersistedFormStepAnswers().keySet()); //NOSHERIFF
            return nextStep.getStep();
        }

        return step;
    }

    public Step getNextStepFromSession(UUID userId, UUID userSiteId) {
        final var session = userSiteSessionService.findByUserSiteId(userId, userSiteId)
                .orElseThrow(() -> new NoSessionException("No session found for user: " + userId + " and user-site: " + userSiteId + ". Possibly expired by TTL."));

        return deserializeStepObject(session);
    }

    private Step deserializeStepObject(ConsentSession session) {
        Step step;
        try {
            if (session.getFormStep() != null) {
                step = objectMapper.readValue(session.getFormStep(), FormStep.class);
            } else if (session.getRedirectUrlStep() != null) {
                step = objectMapper.readValue(session.getRedirectUrlStep(), RedirectStep.class);
            } else {
                throw new NoSessionException("There was no session with a form or redirect step found for this user site");
            }
            return step;
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
