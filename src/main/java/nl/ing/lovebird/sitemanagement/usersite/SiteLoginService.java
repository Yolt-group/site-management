package nl.ing.lovebird.sitemanagement.usersite;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.providershared.api.AuthenticationMeansReference;
import nl.ing.lovebird.providershared.form.Form;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSiteService;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import nl.ing.lovebird.sitemanagement.exception.FunctionalityUnavailableForOneOffAISUsersException;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.exception.KnownProviderRestClientException;
import nl.ing.lovebird.sitemanagement.exception.UnexpectedJsonElementException;
import nl.ing.lovebird.sitemanagement.forms.LoginFormParser;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.nonlicensedclients.AuthenticationMeansFactory;
import nl.ing.lovebird.sitemanagement.providerclient.ApiGetLoginDTO;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsDTO;
import nl.ing.lovebird.sitemanagement.providerclient.EncryptionDetailsService;
import nl.ing.lovebird.sitemanagement.providerclient.ProviderRestClient;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.uuid.AISState;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static java.util.Map.entry;
import static nl.ing.lovebird.sitemanagement.SiteManagementMetrics.ProvidersFunction.GET_STEP;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteLoginService {

    private final ClientRedirectUrlService clientRedirectUrlService;
    private final LoginFormService loginFormService;
    private final ConsentSessionService userSiteSessionService;
    private final ClientSiteService clientSiteService;
    private final ProviderRestClient providerRestClient;
    private final EncryptionDetailsService encryptionDetailsService;
    private final SiteManagementMetrics siteManagementMetrics;
    private final AuthenticationMeansFactory authenticationMeansFactory;
    private final SitesProvider sitesProvider;

    public Pair<Step, UUID> createLoginStepForNewUserSite(
            final ClientUserToken clientUserToken,
            final UUID siteId,
            final UUID redirectUrlId,
            final String psuIpAddress) {
        var site = sitesProvider.findByIdOrThrow(siteId);

        var sessionStateId = AISState.random();
        final Step step = getFirstStep(clientUserToken, site, sessionStateId, redirectUrlId, null, psuIpAddress);

        final String providerState = step.getProviderState();
        String externalConsentId = null;
        if (step instanceof RedirectStep) {
            externalConsentId = ((RedirectStep) step).getExternalConsentId();
        }

        // Start a ConsentSession to create a new UserSite.
        var userSiteSession = userSiteSessionService.createConsentSession(
                sessionStateId,
                new ClientId(clientUserToken.getClientIdClaim()),
                clientUserToken.getUserIdClaim(),
                siteId,
                step,
                redirectUrlId,
                providerState,
                externalConsentId
        );


        return Pair.of(step, userSiteSession.getUserSiteId());
    }

    /**
     * Create the first 'LoginStep'. This is either a Form, or a redirectUrlId.
     * The redirectUrlId is only required if this is a site that returns a redirectUrl.
     * This step will always include the state id that was sent to the bank.
     */
    @SuppressWarnings("squid:S00107")
    public Step getFirstStep(@NonNull ClientUserToken clientUserToken,
                             @NonNull Site site,
                             @NonNull AISState stateId,
                             UUID redirectUrlId,
                             String externalConsentId,
                             String psuIpAddress) {
        final ClientId clientId = new ClientId(clientUserToken.getClientIdClaim());

        try {
            clientSiteService.validateIsClientSiteEnabledForServiceTypeAndRedirectUrlId(clientUserToken, site.getId(), redirectUrlId, ServiceType.AIS);
        } catch (RuntimeException e) {
            if (isScrapingSite(site.getProvider())) {
                // This try catch should be removed *if* scrapering clients do not do 'invalid' operations.
                log.warn("validation failed!", e);
            } else {
                throw e;
            }
        }

        final UUID siteId = site.getId();

        try {
            final Step loginStep;
            if (isScrapingSite(site.getProvider())) {
                loginStep = this.getFirstFormStepForScraper(site, stateId.state(), clientUserToken);
            } else {
                final String baseClientRedirectUrl = clientRedirectUrlService.getBaseClientRedirectUrlOrThrow(clientUserToken, redirectUrlId);
                final AuthenticationMeansReference authenticationMeansReference = authenticationMeansFactory.createAuthMeans(clientUserToken, redirectUrlId);
                final boolean forceExperimentalVersion = clientSiteService.isClientUsingExperimentalVersion(clientId, siteId);
                final ApiGetLoginDTO apiGetLoginDTO = new ApiGetLoginDTO(
                        baseClientRedirectUrl,
                        stateId.state().toString(),
                        authenticationMeansReference,
                        externalConsentId,
                        determinePsuIpAddressForApiGetLogin(site, psuIpAddress)
                );

                try {
                    loginStep = providerRestClient.getLoginInfo(site.getProvider(), siteId, apiGetLoginDTO, clientUserToken, forceExperimentalVersion);
                } catch (HttpException e) {
                    siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(GET_STEP, site.getProvider(), e);
                    log.warn("Failed to get step for provider {}", site.getProvider());
                    // Nothing we can reasonably do at this point.
                    throw new KnownProviderRestClientException(e);
                }
            }
            // Add the stateId for the session to the step that we received from providers.
            // In the case of a redirect login step, this state id can be used by the client to relate redirectUrls from
            // the bank to a user-site in their system. Upon uploading the redirectUrl we can use the state parameter to
            // correlate to which session the form belongs.
            // In the case of a form login step, the client should submit the form (filled-in) back to us, and we use
            // the stateId to correlate to which session the form belongs.
            loginStep.setStateId(stateId.state());
            siteManagementMetrics.incrementCounterGetFirstStepSuccessFailure(site, true);
            return loginStep;

        } catch (Exception e) {
            siteManagementMetrics.incrementCounterGetFirstStepSuccessFailure(site, false);
            throw e;
        }
    }

    private FormStep getFirstFormStepForScraper(
            final Site site,
            final UUID stateId,
            final ClientUserToken clientUserToken) {
        SiteLoginForm siteLoginForm = loginFormService.getLoginForm(site, clientUserToken);
        EncryptionDetailsDTO encryptionDetails = encryptionDetailsService.getEncryptionDetails(site.getId(), site.getProvider(), clientUserToken);
        final Form form;
        try {
            form = LoginFormParser.parseLoginForm(siteLoginForm.getLoginForm());
        } catch (IOException e) {
            throw new UnexpectedJsonElementException(e);
        }
        return new FormStep(form, null, encryptionDetails, siteLoginForm.getLoginFormJson(), stateId);
    }

    /*
     * A hack to enable testing the form-first flows in Consent Starter.
     *
     * bespoke-yoltprovider exposes sites with form-first flows. When the consent flow is initiated,
     * bespoke-yoltprovider returns the forms for specific PSU IP addresses. This method maps the form-first
     * siteId's to those PSU IP addresses.
     * The siteId's can be found in the class YoltProviderAisDetailsProvider in bespoke-yoltprovider.
     */
    private static final Map<UUID, String> SITE_ID_2_PSU_IP_ADDRESS_MAP = Map.ofEntries(
            entry(UUID.fromString("DECC7E83-DA09-4BCA-B9CF-BDFD8AEFDD7B"), "127.0.1.1"), // region selection
            entry(UUID.fromString("c9624c3b-5082-461f-8c02-ecfa6805fc0d"), "127.0.1.2"), // IBAN text field
            entry(UUID.fromString("6f16d556-2845-45c4-a3bd-73054dacada5"), "127.0.1.3"), // bank selection
            entry(UUID.fromString("3acc39d0-3e38-4140-b8c4-ff53c9b0f5d3"), "127.0.1.4"), // account type selection
            entry(UUID.fromString("a0f4753f-94ca-4024-8f67-dc122f86f593"), "127.0.1.5"), // IBAN text field and language selection
            entry(UUID.fromString("c493be6a-137c-4aa8-bb22-d6cedea2efce"), "127.0.1.6"), // branch and account text fields
            entry(UUID.fromString("f22b571a-6557-42d6-a3a3-0b5cd7f58c9c"), "127.0.1.7"), // username text field
            entry(UUID.fromString("475e541e-94ae-4b9f-9b26-4004d81d6374"), "127.0.1.8"),  // email text field
            entry(UUID.fromString("fa1fa884-c6db-11ec-9d64-0242ac120002"), "127.0.1.9"),  // language selection
            entry(UUID.fromString("6f08fdc5-bc8d-444c-8c02-456715731689"), "127.0.1.10"), // LoginID text field
            entry(UUID.fromString("8ef927ce-18c3-4fb6-b1d5-7b92ae2e99d7"), "127.0.1.11"), // IBAN and username text field
            entry(UUID.fromString("e9a8490e-8d3a-4766-a94d-70ef20fe4012"), "127.0.1.12"),  // Username and password
            entry(UUID.fromString("16382d08-ed34-4989-a60b-f4d6afcdb15c"), "127.0.1.13")  // Berlin embedded flow
    );

    private String determinePsuIpAddressForApiGetLogin(@NonNull Site site, String psuIpAddress) {
        return SITE_ID_2_PSU_IP_ADDRESS_MAP.getOrDefault(site.getId(), psuIpAddress);
    }
}
