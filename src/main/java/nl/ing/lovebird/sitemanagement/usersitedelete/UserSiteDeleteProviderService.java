package nl.ing.lovebird.sitemanagement.usersitedelete;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSiteService;
import nl.ing.lovebird.sitemanagement.configuration.ApplicationConfiguration;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.exception.KnownProviderRestClientException;
import nl.ing.lovebird.sitemanagement.exception.UserSiteDeleteException;
import nl.ing.lovebird.sitemanagement.externalconsent.ExternalConsent;
import nl.ing.lovebird.sitemanagement.externalconsent.ExternalConsentService;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.nonlicensedclients.AuthenticationMeansFactory;
import nl.ing.lovebird.sitemanagement.providerclient.ApiNotifyUserSiteDeleteDTO;
import nl.ing.lovebird.sitemanagement.providerclient.FormDeleteUserSiteDTO;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import nl.ing.lovebird.sitemanagement.providerclient.ProviderRestClient;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.SiteManagementMetrics.ProvidersFunction.DELETE_USER_SITE;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;

/**
 * Responsible for deleting user-sites at the external provider.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSiteDeleteProviderService {

    private final Clock clock;
    private final FormProviderRestClient formProviderRestClient;
    private final ExternalConsentService externalConsentService;
    private final ProviderRestClient providerRestClient;
    private final AccessMeansManager accessMeansManager;
    private final SiteManagementMetrics siteManagementMetrics;
    private final AuthenticationMeansFactory authenticationMeansFactory;
    private final ClientSiteService clientSiteService;

    public void deleteUserSiteBlocking(
            final PostgresUserSite userSite,
            final List<PostgresUserSite> otherUserSitesFromSameProvider,
            final String psuIpAddress,
            final ClientUserToken clientUserToken
    ) {
        deleteUserSite(userSite, otherUserSitesFromSameProvider, psuIpAddress, clientUserToken);
    }

    @Async(ApplicationConfiguration.ASYNC_EXECUTOR)
    public void deleteUserSiteAsync(
            final PostgresUserSite userSite,
            final List<PostgresUserSite> otherUserSitesFromSameProvider,
            final String psuIpAddress,
            final ClientUserToken clientUserToken
    ) {
        try {
            deleteUserSite(userSite, otherUserSitesFromSameProvider, psuIpAddress, clientUserToken);
        } catch (KnownProviderRestClientException e) {
            log.warn("Error while asynchronously deleting a user-site at the provider", e);
        } catch (RuntimeException e) {
            log.error("Error while asynchronously deleting a user-site at the provider", e);
        }
    }

    private void deleteUserSite(
            final PostgresUserSite userSite,
            final List<PostgresUserSite> otherUserSitesFromSameProvider,
            final String psuIpAddress,
            final ClientUserToken clientUserToken
    ) {
        log.info("Deleting user-site={}, provider={}",
                userSite.getUserSiteId(), userSite.getProvider());

        if (isScrapingSite(userSite.getProvider())) {
            deleteFormUserSite(userSite, otherUserSitesFromSameProvider, clientUserToken);
        } else {
            deleteUrlUserSite(userSite, psuIpAddress, clientUserToken);
        }
    }

    private void deleteFormUserSite(
            final PostgresUserSite userSite,
            final List<PostgresUserSite> otherUserSitesFromSameProvider,
            final ClientUserToken clientUserToken
    ) {
        String provider = userSite.getProvider();
        String externalId = userSite.getExternalId();

        if (StringUtils.isEmpty(externalId)) {
            log.info("external id is empty for provider {} and user-site with id {}, not removing it on provider side",
                    provider, userSite.getUserSiteId());
            return;
        }

        if (otherUserSitesWithSameExternalIdExist(externalId, otherUserSitesFromSameProvider)) {
            log.info("Not deleting user-site at the provider, since we have other internal user-sites connected to the same external one");
            return;
        }

        AccessMeansManager.AccessMeansResult accessMeansResult = accessMeansManager.retrieveValidAccessMeans(
                clientUserToken,
                userSite,
                Instant.now(clock),
                null
        );
        if (accessMeansResult.getResultCode() != AccessMeansManager.AccessMeansResult.ResultCode.OK) {
            log.info("Failed to delete user-site at scraping provider, could not retrieve valid access means, reason: {}", accessMeansResult.getResultCode());
            return;
        }

        try {
            FormDeleteUserSiteDTO formDeleteUserSite = new FormDeleteUserSiteDTO(
                    accessMeansResult.getAccessMeans().toAccessMeansDTO().getAccessMeansBlob(),
                    userSite.getExternalId(),
                    userSite.getUserId(),
                    userSite.getClientId()
            );
            formProviderRestClient.deleteUserSite(provider, formDeleteUserSite, clientUserToken, userSite.getSiteId());
        } catch (HttpException e) {
            log.warn("Failed to delete a user-site at scraping provider {} ", provider);
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(DELETE_USER_SITE, userSite.getProvider(), e);
            throw new KnownProviderRestClientException(e);
        }
    }

    private void deleteUrlUserSite(final PostgresUserSite userSite, final String psuIpAddress, final ClientUserToken clientUserToken) {
        try {
            externalConsentService.findById(userSite.getUserId(), userSite.getSiteId(), userSite.getUserSiteId())
                    .map(ExternalConsent::getExternalConsentId)
                    .ifPresent(externalConsentId -> {
                        attemptToRemoveDirectConnectionUserSite(userSite, psuIpAddress, clientUserToken, externalConsentId);

                        // We only want to do this (once) after a successful delete, that's why this statement is placed here
                        externalConsentService.deleteForUserSite(userSite.getUserId(), userSite.getSiteId(), userSite.getUserSiteId());
                    });
        } catch (KnownProviderRestClientException e) {
            log.warn("Failed to delete a user-site at provider {} with id {}, external id: {}", userSite.getProvider(), userSite.getUserSiteId(), userSite.getExternalId(), e); //NOSHERIFF
            throw e;
        } catch (RuntimeException e) {
            final String message = String.format("Failed to delete a user-site at provider with id %s, external id: %s",
                    userSite.getUserSiteId(),
                    userSite.getExternalId());
            throw new UserSiteDeleteException(message, e);
        }
    }

    private void attemptToRemoveDirectConnectionUserSite(
            final PostgresUserSite userSite,
            final String psuIpAddress,
            final ClientUserToken clientUserToken,
            final String externalConsentId) {

        try {
            AccessMeansManager.AccessMeansResult accessMeansResult = accessMeansManager.retrieveValidAccessMeans(
                    clientUserToken,
                    userSite,
                    Instant.now(clock),
                    psuIpAddress
            );

            // Most probably temporary provider issue, give up.
            if (accessMeansResult.getResultCode() == AccessMeansManager.AccessMeansResult.ResultCode.UNKNOWN_ERROR) {
                log.warn("Not deleting user site {} externally at the bank. Failed to acquire access means.", userSite.getUserSiteId());
                return;
            }

            // All other non-recoverable errors from providers connection point of view.
            if (accessMeansResult.getResultCode() != AccessMeansManager.AccessMeansResult.ResultCode.OK) {
                log.info("Not deleting user site {} externally (at scraper or bank). The user access means could not be refreshed {} ", userSite.getUserSiteId(), accessMeansResult.getResultCode());
                return;
            }
            ClientId clientId = new ClientId(clientUserToken.getClientIdClaim());
            UUID siteId = userSite.getSiteId();
            boolean forceExperimentalVersion = clientSiteService.isClientUsingExperimentalVersion(clientId, siteId);
            ApiNotifyUserSiteDeleteDTO request = new ApiNotifyUserSiteDeleteDTO(
                    externalConsentId,
                    authenticationMeansFactory.createAuthMeans(clientUserToken, userSite.getRedirectUrlId()),
                    psuIpAddress,
                    accessMeansResult.getAccessMeans().toAccessMeansDTO()
            );
            providerRestClient.notifyUserSiteDelete(userSite.getProvider(), request, clientUserToken, forceExperimentalVersion);
        } catch (HttpException e) {
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(DELETE_USER_SITE, userSite.getProvider(), e);
            throw new KnownProviderRestClientException(e);
        }
    }

    /**
     * When the user creates a new user-site while it is still existing at the provider's side the provider can return the same external id.
     * In that case we will have multiple user-sites pointing to the same external id and then we should not delete it.
     * The scenario where this can happen is when the async call during the mark for delete failed and the user re-added the user-site.
     */
    private boolean otherUserSitesWithSameExternalIdExist(final String externalId, final List<PostgresUserSite> otherUserSitesFromSameProvider) {
        return StringUtils.isNotEmpty(externalId) &&
                otherUserSitesFromSameProvider.stream().anyMatch(u -> externalId.equals(u.getExternalId()));
    }
}
