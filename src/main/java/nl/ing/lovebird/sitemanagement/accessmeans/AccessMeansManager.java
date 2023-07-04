package nl.ing.lovebird.sitemanagement.accessmeans;

import com.yolt.securityutils.crypto.SecretKey;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.secretspipeline.VaultKeys;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSiteService;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.exception.KnownProviderRestClientException;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.nonlicensedclients.AuthenticationMeansFactory;
import nl.ing.lovebird.sitemanagement.providercallback.UserExternalId;
import nl.ing.lovebird.sitemanagement.providercallback.UserExternalIdRepository;
import nl.ing.lovebird.sitemanagement.providerclient.*;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.Duration.of;
import static java.time.temporal.ChronoUnit.MINUTES;
import static nl.ing.lovebird.sitemanagement.SiteManagementMetrics.ProvidersFunction.*;
import static nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager.AccessMeansResult.ResultCode.ACCESS_MEANS_DO_NOT_EXIST;
import static nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansMapper.*;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;

/**
 * {@link AccessMeansManager} can manage {@link AccessMeans} and {@link UserSiteAccessMeans} for {@link PostgresUserSite}s.  This means:
 * <ul>
 *     <li>retrieving/updating the means in the database</li>
 *     <li>renewing the means with a provider (when applicable)</li>
 * </ul>
 */
@Slf4j
@Service
@SuppressWarnings("squid:SwitchLastCaseIsDefaultCheck")
public class AccessMeansManager {

    private static final String AES256_ENCRYPTION_KEY_NAME = "encryption-key";

    private final ClientSiteService clientSiteService;
    private final AccessMeansRepository accessMeansRepository;
    private final UserSiteAccessMeansRepository userSiteAccessMeansRepository;
    private final FormProviderRestClient formProviderRestClient;
    private final ProviderRestClient providerRestClient;
    private final SiteManagementMetrics siteManagementMetrics;
    private final UserExternalIdRepository userExternalIdRepository;
    private final AuthenticationMeansFactory authenticationMeansFactory;
    private final SecretKey encryptionKey;
    private final Clock clock;

    public AccessMeansManager(ClientSiteService clientSiteService, AccessMeansRepository accessMeansRepository, UserSiteAccessMeansRepository userSiteAccessMeansRepository, FormProviderRestClient formProviderRestClient, ProviderRestClient providerRestClient, SiteManagementMetrics siteManagementMetrics, UserExternalIdRepository userExternalIdRepository, AuthenticationMeansFactory authenticationMeansFactory,
                              VaultKeys vaultKeys, Clock clock) {
        this.clientSiteService = clientSiteService;
        this.accessMeansRepository = accessMeansRepository;
        this.userSiteAccessMeansRepository = userSiteAccessMeansRepository;
        this.formProviderRestClient = formProviderRestClient;
        this.providerRestClient = providerRestClient;
        this.siteManagementMetrics = siteManagementMetrics;
        this.userExternalIdRepository = userExternalIdRepository;
        this.authenticationMeansFactory = authenticationMeansFactory;
        this.encryptionKey = vaultKeys.getSymmetricKey(AES256_ENCRYPTION_KEY_NAME);
        this.clock = clock;
    }

    /**
     * This method will try do the following things:
     * - retrieve the means from the database
     * - check that the access means are valid, if they are not: renew the means and store them in the database
     * - return the means
     */
    public AccessMeansResult retrieveValidAccessMeans(
            @NonNull final ClientUserToken clientUserToken,
            @NonNull final PostgresUserSite userSite,
            @NonNull final Instant requestStartTime,
            @Nullable final String psuIpAddress
    ) {
        if (!isScrapingSite(userSite.getProvider())) {
            return manageForDirectConnectionProvider(clientUserToken, userSite, psuIpAddress, requestStartTime);
        } else {
            return manageForScrapingProvider(clientUserToken, userSite.getProvider(), userSite.getUserId(), requestStartTime);
        }
    }

    public AccessMeansHolder createUserForScrapingProvider(
            PostgresUserSite userSite,
            @NonNull ClientUserToken clientUserToken
    ) {
        FormCreateNewUserResponseDTO newUser;
        try {
            newUser = formProviderRestClient.createNewUser(userSite.getProvider(), new FormCreateNewUserRequestDTO(
                    userSite.getUserId(),
                    userSite.getSiteId(),
                    userSite.getClientId(),
                    SiteService.isSiteStubbedByYoltbank(userSite.getSiteId())
            ), clientUserToken);
        } catch (HttpException e) {
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(SCR_CREATE_USER, userSite.getProvider(), e);
            // No way to reasonably recover.
            throw new KnownProviderRestClientException(e);
        }

        // Store the newly acquired access means.
        AccessMeans accessMeans = dtoToAccessMeans(newUser.getAccessMeans(), userSite.getProvider(), encryptionKey);
        accessMeansRepository.save(accessMeans);

        // Store a record to link our user to the external-user in the systems of a provider.
        userExternalIdRepository.save(new UserExternalId(
                userSite.getUserId(),
                userSite.getProvider(),
                newUser.getExternalUserId()
        ));

        return AccessMeansHolder.fromAccessMeans(accessMeans, encryptionKey);
    }

    /**
     * Call this method for to create or overwrite {@link UserSiteAccessMeans}.  Either the user first gives
     * consent, or the user renews their consent.  In no other situation should this method be called.
     * <p>
     * This restriction is necessary because this method updates the {@link UserSiteAccessMeans#getCreated()} field.
     * This field is exposed to clients and functions as an indicator for how long the consent will remain valid.
     * <p>
     * Elsewhere in this class we call {@link UserSiteAccessMeansRepository#save(UserSiteAccessMeans)} directly
     * without altering the {@link UserSiteAccessMeans#getCreated()} field.  That is correct since the field
     * {@link UserSiteAccessMeans#getCreated()} must not be updated when we exchange a refresh token for an
     * access token.
     */
    public void upsertUserSiteAccessMeans(@NonNull AccessMeansDTO accessMeansDTO, PostgresUserSite userSite) {
        if (isScrapingSite(userSite.getProvider())) {
            throw new IllegalArgumentException("Trying to save UserSiteAccessMeans for a scraping provider.");
        }
        UserSiteAccessMeans userSiteAccessMeans = AccessMeansMapper.dtoToUserSiteAccessMeans(accessMeansDTO, userSite.getProvider(), userSite.getUserSiteId(), encryptionKey);
        userSiteAccessMeans.setCreated(Instant.now(clock));
        userSiteAccessMeansRepository.save(userSiteAccessMeans);
        // Keep a metric.
        siteManagementMetrics.incrementCounterConsentSuccess(userSite.getProvider());
    }

    /**
     * @param userSite usersite for which the AccessMeans must be deleted
     */
    public void deleteAccessMeansForUserSite(
            PostgresUserSite userSite,
            @NonNull ClientUserToken clientUserToken,
            boolean userHasOtherUserSitesWithSameProvider
    ) {
        //noinspection EnhancedSwitchMigration
        if (!isScrapingSite(userSite.getProvider())) {
            userSiteAccessMeansRepository.delete(userSite.getUserId(), userSite.getUserSiteId(), userSite.getProvider());
            return;
        } else {
            // Only delete AccessMeans if the IUserSite for which we are asked to delete them is the only
            // IUserSite that the user has with a given provider.  If the user has another IUserSite with the
            // same provider, the AccessMeans are shared, and should thus not be deleted.
            if (userHasOtherUserSitesWithSameProvider) {
                return;
            }

            // Retrieve the AccessMeans in order to inform the scraping provider that they can delete the user at their end.
            // Even though we are currently in the process of deleting the access means at a provider, this might cause
            // a renewal request to be sent to a scraping provider.
            AccessMeansResult accessMeansResult = manageForScrapingProvider(
                    clientUserToken,
                    userSite.getProvider(),
                    userSite.getUserId(),
                    Instant.now(clock)
            );
            switch (accessMeansResult.getResultCode()) {
                case OK:
                    try {
                        formProviderRestClient.deleteUser(
                                userSite.getProvider(),
                                new FormDeleteUser(
                                        accessMeansResult.accessMeans.toAccessMeansDTO(),
                                        userSite.getClientId()
                                ),
                                clientUserToken
                        );
                    } catch (HttpException e) {
                        siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(SCR_DELETE_USER, userSite.getProvider(), e);
                        log.warn("Failed to delete user at scraping provider {}", userSite.getProvider());
                        throw new KnownProviderRestClientException(e);
                    }
                    break;
                case ACCESS_MEANS_DO_NOT_EXIST:
                    log.warn("AccessMeans could not be found in the database.  Have they already been deleted?");
                    break;
                case UNKNOWN_ERROR:
                    log.error("An unknown error occurred while deleting accessMeans for a scraping provider.");
                    break;
                default:
                    throw new IllegalStateException();
            }
            // Once the means have been deleted at the provider, also delete the AccessMeans from our database.
            accessMeansRepository.delete(userSite.getUserId(), userSite.getProvider());
        }
    }

    private AccessMeansResult manageForDirectConnectionProvider(
            @NonNull ClientUserToken clientUserToken,
            PostgresUserSite userSite,
            @Nullable String psuIpAddress,
            Instant requestStartTime
    ) {
        ClientId clientId = new ClientId(clientUserToken.getClientIdClaim());
        UUID siteId = userSite.getSiteId();
        Optional<UserSiteAccessMeans> optionalUserSiteAccessMeans = userSiteAccessMeansRepository.get(
                userSite.getUserId(),
                userSite.getUserSiteId(),
                userSite.getProvider()
        );
        if (optionalUserSiteAccessMeans.isEmpty()) {
            return new AccessMeansResult(ACCESS_MEANS_DO_NOT_EXIST);
        }

        // Access means are present in the db.

        final UserSiteAccessMeans userSiteAccessMeans = optionalUserSiteAccessMeans.get();

        if (!isCloseToExpiry(userSiteAccessMeans.getExpireTime(), requestStartTime)) {
            // Happy path, we're done.
            return new AccessMeansResult(AccessMeansHolder.fromUserSiteAccessMeans(userSiteAccessMeans, encryptionKey));
        }

        // Try to renew the access means.

        final AccessMeansDTO renewedAccessMeansDTO;
        try {
            boolean forceExperimentalVersion = clientSiteService.isClientUsingExperimentalVersion(clientId, siteId);
            renewedAccessMeansDTO = providerRestClient.refreshAccessMeans(
                    userSite.getProvider(),
                    userSite.getSiteId(),
                    new RefreshAccessMeansDTO(
                            userSiteAccessMeansToDTO(userSiteAccessMeans, encryptionKey),
                            authenticationMeansFactory.createAuthMeans(clientUserToken, userSite.getRedirectUrlId()),
                            psuIpAddress,
                            userSiteAccessMeans.getCreated()
                    ),
                    clientUserToken,
                    forceExperimentalVersion
            );
        } catch (HttpException e) {
            if ("PR034".equals(e.getFunctionalErrorCode())) {
                // The user has to renew their consent.
                return new AccessMeansResult(AccessMeansResult.ResultCode.DIRECT_CONNECTION_PROVIDER_ERROR_COULD_NOT_RENEW_BECAUSE_CONSENT_EXPIRED);
            }
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(RENEW_ACCESS_MEANS, userSite.getProvider(), e);
            log.warn("POST /access-means/refresh failed (http)", e);
            return new AccessMeansResult(AccessMeansResult.ResultCode.UNKNOWN_ERROR);
        } catch (RuntimeException e) {
            log.error("POST /access-means/refresh failed (other)", e);
            return new AccessMeansResult(AccessMeansResult.ResultCode.UNKNOWN_ERROR);
        }

        // Renewing succeeded, persist the access means.

        final UserSiteAccessMeans renewedUserSiteAccessMeans = dtoToUserSiteAccessMeans(renewedAccessMeansDTO, userSite.getProvider(), userSite.getUserSiteId(), encryptionKey);
        if (!renewedUserSiteAccessMeans.getUserId().equals(userSiteAccessMeans.getUserId()) || !renewedUserSiteAccessMeans.getUserSiteId().equals(userSiteAccessMeans.getUserSiteId())) {
            throw new IllegalStateException("Renewed UserSiteAccessMeans have mismatching key.");
        }

        // Copy the "created" value that was present on the original UserSiteAccessMeans to retain it (it is null on
        // renewedUserSiteAccessMeans).  The semantics of this field are: the moment the UserSiteAccessMeans were
        // created, we are only renewing them now, but the "consent" is still the same.
        renewedUserSiteAccessMeans.setCreated(userSiteAccessMeans.getCreated());
        if (renewedUserSiteAccessMeans.getCreated() == null) {
            // 2021-02-21 Set a sentinel value, this field must be non-null before saving the AccessMeans to the database.
            // This if-statement can be dropped after 90 days have passed since this comment was added, since all AccessMeans
            // that are valid in the database will have 'created' set after that date.  Before definitively deleting this
            // if-statement, make sure that the below log message does not occur on any production environment.
            renewedUserSiteAccessMeans.setCreated(Instant.EPOCH);
            log.info("Setting UserSiteAccessMeans.created to EPOCH as a fallback.");
        }
        userSiteAccessMeansRepository.save(renewedUserSiteAccessMeans);

        return new AccessMeansResult(AccessMeansHolder.fromUserSiteAccessMeans(renewedUserSiteAccessMeans, encryptionKey));
    }

    /**
     * Retrieve the times at which a user has most recently given consent for their {@link IUserSite .
     *
     * @return a map of {@link IUserSite getUserSiteId()} -> {@link Instant}
     */
    public Map<UUID, Instant> retrieveAccessMeansCreatedAtForUser(@NonNull UUID userId) {
        return userSiteAccessMeansRepository.getForUser(userId).stream()
                .filter(userSiteAccessMeans -> userSiteAccessMeans.getCreated() != null)
                .collect(Collectors.toMap(UserSiteAccessMeans::getUserSiteId, UserSiteAccessMeans::getCreated));
    }

    public Optional<Instant> retrieveAccessMeansCreatedAtForUserSite(PostgresUserSite userSite) {
        return userSiteAccessMeansRepository.get(userSite.getUserId(), userSite.getUserSiteId(), userSite.getProvider())
                .map(UserSiteAccessMeans::getCreated);
    }

    public AccessMeansResult manageForScrapingProvider(@NonNull ClientUserToken clientUserToken, @NonNull String provider, @NonNull UUID userId, Instant requestStartTime) {
        Optional<AccessMeans> optionalAccessMeans = accessMeansRepository.get(
                userId,
                provider
        );
        if (optionalAccessMeans.isEmpty()) {
            return new AccessMeansResult(ACCESS_MEANS_DO_NOT_EXIST);
        }

        // Access means are present in db.

        final AccessMeans accessMeans = optionalAccessMeans.get();

        if (!isCloseToExpiry(accessMeans.getExpireTime(), requestStartTime)) {
            // Happy path, we're done.
            return new AccessMeansResult(AccessMeansHolder.fromAccessMeans(accessMeans, encryptionKey));
        }

        // Try to renew the access means.

        final AccessMeansDTO renewedAccessMeansDTO;
        try {
            renewedAccessMeansDTO = formProviderRestClient.accessMeansRefresh(
                    provider,
                    new FormRefreshAccessMeansDTO(
                            AccessMeansMapper.accessMeansToDTO(accessMeans, encryptionKey),
                            new ClientId(clientUserToken.getClientIdClaim())
                    ),
                    clientUserToken
            );
        } catch (HttpException e) {
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(RENEW_ACCESS_MEANS, provider, e);
            log.warn("POST /access-means/refresh failed (http)", e);
            return new AccessMeansResult(AccessMeansResult.ResultCode.UNKNOWN_ERROR);
        } catch (RuntimeException e) {
            log.error("POST /access-means/refresh failed (other)", e);
            return new AccessMeansResult(AccessMeansResult.ResultCode.UNKNOWN_ERROR);
        }
        // Renewing succeeded, persist the access means.

        final AccessMeans newAccessMeans = AccessMeansMapper.dtoToAccessMeans(renewedAccessMeansDTO, provider, encryptionKey);
        accessMeansRepository.save(newAccessMeans);

        return new AccessMeansResult(AccessMeansHolder.fromAccessMeans(newAccessMeans, encryptionKey));
    }

    /**
     * Returns true if the {@code expiryTime} is "in the near future".
     * We've chosen a somewhat arbitrary and (hopefully) safe value of 1 minute.
     */
    private boolean isCloseToExpiry(Date expiryTime, Instant requestStartTime) {
        return requestStartTime.plus(of(1, MINUTES)).isAfter(expiryTime.toInstant());
    }

    @Value
    public static class AccessMeansResult {

        public enum ResultCode {
            // Can occur for all types of providers.
            ACCESS_MEANS_DO_NOT_EXIST,
            UNKNOWN_ERROR,
            OK,
            // Direct connection providers only.
            DIRECT_CONNECTION_PROVIDER_ERROR_COULD_NOT_RENEW_BECAUSE_CONSENT_EXPIRED,
            ;
        }

        public AccessMeansResult(AccessMeansHolder accessMeans) {
            this.resultCode = ResultCode.OK;
            this.accessMeans = accessMeans;
        }

        public AccessMeansResult(ResultCode resultCode) {
            this.resultCode = resultCode;
            this.accessMeans = null;
        }

        ResultCode resultCode;
        AccessMeansHolder accessMeans;
    }

}
