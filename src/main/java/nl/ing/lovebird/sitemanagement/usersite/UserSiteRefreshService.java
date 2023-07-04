package nl.ing.lovebird.sitemanagement.usersite;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.*;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansHolder;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager;
import nl.ing.lovebird.sitemanagement.accessmeans.CustomExpiredConsentFlowService;
import nl.ing.lovebird.sitemanagement.accessmeans.UserSiteAccessMeans;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.AccountsAndTransactionsClient;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.UserSiteTransactionStatusSummary;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSiteService;
import nl.ing.lovebird.sitemanagement.configuration.ApplicationConfiguration;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.health.activities.ActivityService;
import nl.ing.lovebird.sitemanagement.legacy.aismigration.MigrationConstants;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.nonlicensedclients.AuthenticationMeansFactory;
import nl.ing.lovebird.sitemanagement.providerclient.*;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequest;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequestRepository;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static nl.ing.lovebird.sitemanagement.SiteManagementMetrics.ProvidersFunction.FETCH_DATA;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;

/**
 * This class can trigger a refresh ("data fetch") for one or more UserSites.
 * <p>
 * There are two methods that do exactly the same:
 * <ul>
 *     <li>{@link #refreshUserSitesAsync}: returns immediately</li>
 *     <li>{@link #refreshUserSitesBlocking}: will block execution because it performs http calls</li>
 * </ul>
 * <p>
 * The methods {@link #refreshUserSitesBlocking} is an "idiot proof" method that will do the following:
 * <ul>
 *     <li>Filter out UserSites that are not "eligible" to be refreshed, see {@link #isUserSiteEligibleForDataFetch}.</li>
 *     <li>Lock all the "eligible" user sites to prevent concurrent data fetches.</li>
 *     <li>Send an appropriate {@link UserSiteStartEvent} so that interested consumers know a data fetch is in progress</li>
 *     <li>Perform the actual data fetch for every user-site:
 *         <ul>
 *             <li>Make sure that valid {@link AccessMeansDTO} are present, see {@link AccessMeansManager}.</li>
 *             <li>Trigger a data fetch at providers, see {@link #triggerDataFetch}.</li>
 *         </ul>
 *     </li>
 * </ul>
 * <p>
 *     If anything goes wrong during the above and a {@link UserSiteStartEvent} has been sent, {@link #refreshUserSitesBlocking}
 *     will make an effort to notify the consumers of the UserSites for which no data fetch was triggered by sending the appropriate
 *     {@link EventType#REFRESHED_USER_SITE} event.
 *     In addition, the error handler will also update the connection status accordingly.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSiteRefreshService {

    private final Clock clock;
    private final ProviderRequestRepository providerRequestRepository;
    private final FormProviderRestClient formProviderRestClient;
    private final AccessMeansManager accessMeansManager;
    private final CustomExpiredConsentFlowService customExpiredConsentFlowService;
    private final UserService userService;
    private final UserSiteService userSiteService;
    private final SiteService siteService;
    private final ActivityService activityService;
    private final SiteManagementMetrics siteManagementMetrics;
    private final ProviderRestClient providerRestClient;
    private final AuthenticationMeansFactory authenticationMeansFactory;
    private final LastFetchedService lastFetchedService;
    private final ClientSiteService clientSiteService;
    private final AccountsAndTransactionsClient accountsAndTransactionsClient;

    static final Collection<UserSiteActionType> userSiteActionTypesForMultipleUserSites = Set.of(UserSiteActionType.FLYWHEEL_REFRESH, UserSiteActionType.USER_REFRESH);

    /**
     * {@link Async} variant of {@link #refreshUserSitesBlocking}.
     */
    @Async(ApplicationConfiguration.ASYNC_EXECUTOR)
    public void refreshUserSitesAsync(
            @NonNull Collection<PostgresUserSite> userSites,
            boolean oneOffAisUser,
            @NonNull ClientUserToken clientUserToken,
            @NonNull UserSiteActionType userSiteActionType,
            @Nullable String psuIpAddress,
            @Nullable UUID inProgressActivityId
    ) {
        refreshUserSitesBlocking(userSites, oneOffAisUser, clientUserToken, userSiteActionType, psuIpAddress, inProgressActivityId);
    }

    /**
     * If {@code inProgressActivityId} is not-null, this method assumes that all {@code userSites} are eligible to
     * be refreshed and have already been locked by the caller.
     * <p>
     * If this method returns a UUID then that UUID is the 'running' activityId.
     * If this method returns empty() it means no refresh has been started.
     */
    public Optional<UUID> refreshUserSitesBlocking(
            @NonNull Collection<PostgresUserSite> userSites,
            boolean oneOffAisUser,
            @NonNull ClientUserToken clientUserToken,
            @NonNull UserSiteActionType userSiteActionType,
            @Nullable String psuIpAddress,
            @Nullable UUID inProgressActivityId
    ) {
        return siteManagementMetrics.timeUserSitesRefresh(
                () -> internalRefreshUserSitesBlocking(userSites,
                        oneOffAisUser,
                        clientUserToken,
                        userSiteActionType,
                        psuIpAddress,
                        inProgressActivityId));
    }

    public Optional<UUID> refreshUserSitesBlocking(
            @NonNull Collection<PostgresUserSite> userSites,
            @NonNull ClientUserToken clientUserToken,
            @NonNull UserSiteActionType userSiteActionType,
            @Nullable String psuIpAddress,
            @Nullable UUID inProgressActivityId
    ) {
        final User user = userService.getUserOrThrow(clientUserToken.getUserIdClaim());

        return refreshUserSitesBlocking(userSites, user.isOneOffAis(), clientUserToken, userSiteActionType,
                psuIpAddress, inProgressActivityId);
    }

    private Optional<UUID> internalRefreshUserSitesBlocking(
            @NonNull Collection<PostgresUserSite> userSites,
            boolean oneOffAisUser,
            @NonNull ClientUserToken clientUserToken,
            @NonNull UserSiteActionType userSiteActionType,
            @Nullable String psuIpAddress,
            @Nullable UUID inProgressActivityId
    ) {
        final List<PostgresUserSite> userSitesToRefresh = userSites.stream()
                .filter(userSite -> !oneOffAisUser || userSite.getLastDataFetch() == null)
                .collect(toList());
        if (userSitesToRefresh.size() < userSites.size()) {
            log.warn("A second refresh was requested for one or more user-sites of a one-off AIS user. Not refreshing those user-sites.");
        }

        if (!validRefreshUserSitesRequest(userSitesToRefresh, userSiteActionType)) {
            return Optional.empty();
        }

        List<UserSiteTransactionStatusSummary> summaries = accountsAndTransactionsClient.retrieveUserSiteTransactionStatusSummary(clientUserToken);

        var lockedSites = lockUserSites(userSitesToRefresh, inProgressActivityId);
        var activityId = lockedSites.getKey();
        var lockedUserSites = lockedSites.getValue();

        if (lockedUserSites.isEmpty()) {
            log.info("No user-sites to refresh for in-progress activity-id [{}]. No user-sites are eligible or could not be locked at this time.", inProgressActivityId);
            return Optional.empty(); // Exit early if there's nothing to do.
        }

        // Proceed to trigger a data fetch for all the usersites for which we could acquire a lock.

        // We need to do some administration to make sure that if an exception occurs during the data fetch process
        // that we send the appropriate events so that listeners know a failure has occurred.
        boolean hasStartedActivity = false;
        // This collection keeps track of the UserSites for which a data fetch was triggered successfully, these
        // can be excluded from error handling.
        final Collection<UUID> dataFetchTriggeredForUserSiteIds = new ArrayList<>(lockedUserSites.size());
        try {
            // Send an activity event to signal the start of a data fetch.
            var event = createStartEvent(lockedUserSites, activityId, userSiteActionType, clientUserToken)
                    .orElseThrow(() -> new IllegalArgumentException("Don't know how to handle: " + userSiteActionType));

            activityService.startActivity(clientUserToken, event);

            // Keep some administration so the error handling code will know to send the appropriate messages in the event of a failure.
            hasStartedActivity = true;

            for (final PostgresUserSite userSite : lockedUserSites) {
                try (LogBaggage ignored = new LogBaggage(userSite)) {
                    Optional<AccessMeansHolder> accessMeans = getAccessMeans(userSite, activityId, clientUserToken, psuIpAddress);
                    if (accessMeans.isEmpty()) {
                        // The method getAccessMeans will have already called markAsFailed()
                        continue;
                    }
                    if (!triggerDataFetch(
                            clientUserToken,
                            userSite,
                            summaries,
                            activityId,
                            accessMeans.get(),
                            userSiteActionType,
                            psuIpAddress)) {
                        markAsFailed(userSite, clientUserToken, activityId, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR);
                        continue;
                    }
                    dataFetchTriggeredForUserSiteIds.add(userSite.getUserSiteId());
                }
            }
        } catch (Exception e1) {
            log.error("refresh: unexpected exception", e1);
            try {
                for (PostgresUserSite userSite : lockedUserSites) {
                    if (dataFetchTriggeredForUserSiteIds.contains(userSite.getUserSiteId())) {
                        // Everything went OK for this userSite.  No error handling required.
                        continue;
                    }
                    userSiteService.unlock(userSite);
                    userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR, null);
                    if (hasStartedActivity) {
                        // At this point we have a userSite that was included in the UserSiteStartEvent but for which
                        // no data fetch was triggered, we now need to send a message to make sure consumers will not
                        // wait around for data that will never come in.
                        activityService.handleFailedRefresh(clientUserToken, activityId, userSite, RefreshedUserSiteEvent.Status.FAILED);
                    } else {
                        // If we have not sent an activity to health we should not return an activity id.
                        return Optional.empty();
                    }
                }
            } catch (Exception e2) {
                // If this condition occurs we've presumably failed to send activityEvents for all the failed usersites and the
                // refresh is effectively "incomplete": a consumer might wait until a timeout, user-sites will remain locked, etc.
                log.error("refresh: unexpected exception while handling errors. activity {} is incomplete", activityId, e2);
            }
        }

        return Optional.of(activityId);
    }

    boolean validRefreshUserSitesRequest(@NonNull Collection<PostgresUserSite> userSites, @NonNull UserSiteActionType userSiteActionType) {
        if (userSites.isEmpty()) {
            log.warn("refreshUserSitesBlocking called with an empty list.");
            return false;
        }
        if (userSites.size() > 1 && !userSiteActionTypesForMultipleUserSites.contains(userSiteActionType)) {
            throw new IllegalArgumentException("refresh: fetching data for > 1 user-site can not be done with action " + userSiteActionType.name());
        }
        if (userSiteActionType == UserSiteActionType.CREATE_USER_SITE) {
            final PostgresUserSite userSite = userSites.iterator().next();
            if (isScrapingSite(userSite.getProvider())) {
                // This method should not be called for a scraping provider after a CREATE_USER_SITE.  Two reasons:
                // 1) There is no need for us to 'trigger' a data fetch at a scraping provider after having just created
                //    the usersite.  The reason is that scraping providers do not consider 'connecting' and 'fetching data'
                //    to be separate actions.  We send credentials their way and they will immediately connect and fetch
                //    data in one go.
                // 2) We want the CREATE_USER_SITE event to carry the meaning: "the user site has been connected to a site",
                //    and that is only the case if the credentials are correct.  We don't know that yet at this point.
                //    The CREATE_USER_SITE event will be sent immediately after receiving confirmation from the scraping
                //    provider that the credentials are correct, that is coincidentally the same moment that the first data
                //    is sent to us by the scraping provider.  See GenericDataProviderResponseProcessor.
                throw new IllegalArgumentException("refresh: method should not be called for CREATE_USER_SITE in case of a scraping user-site.");
            }
        }
        return true;
    }

    Pair<UUID, Collection<PostgresUserSite>> lockUserSites(@NonNull Collection<PostgresUserSite> userSites, @Nullable UUID inProgressActivityId) {
        return inProgressActivityId != null ?
                checkUserSitesLocksForActivity(userSites, inProgressActivityId) :
                lockUserSitesWithNewActivity(userSites);
    }

    private Pair<UUID, Collection<PostgresUserSite>> lockUserSitesWithNewActivity(@NonNull Collection<PostgresUserSite> userSites) {
        // This method is in control of the activity and will need to lock the user-sites.
        // Come up with a random activityId so the receivers of the activityEvents that we send as part of this refresh
        // can correlate those events to one another.
        var activityId = UUID.randomUUID();
        // Filter out non-eligible user-sites (there are various possible reasons why a usersite might not be eligible).
        var eligibleUserSites = userSites.stream()
                .filter(this::isUserSiteEligibleForDataFetch)
                .toList();

        // Subsequently lock everything that can be locked.
        var lockedUserSites = eligibleUserSites.stream()
                .filter(userSite -> userSiteService.attemptLock(userSite, activityId))
                .collect(toList());

        log.info("refresh: requested={}, eligible={}, locked={}. activityId={}", userSites.size(), eligibleUserSites.size(), lockedUserSites.size(), activityId);
        return Pair.of(activityId, lockedUserSites);
    }

    private Pair<UUID, Collection<PostgresUserSite>> checkUserSitesLocksForActivity(@NonNull Collection<PostgresUserSite> userSites, @Nullable UUID inProgressActivityId) {
        // The caller is in control of the activity and has locked the user-sites for us (we'll check).
        // Check that all user-sites are in fact locked by the provided inProgressActivityId.
        for (PostgresUserSite userSite : userSites) {
            Optional<PostgresUserSiteLock> optionalUserSiteLock = userSiteService.checkLock(userSite);
            if (optionalUserSiteLock.isEmpty()) {
                log.error("refresh, lock problem: called with inProgressActivityId={} but userSite={} is not locked.  Acquiring lock.", inProgressActivityId, userSite.getUserSiteId());
                if (!userSiteService.attemptLock(userSite, inProgressActivityId)) {
                    log.error("refresh, lock problem: acquiring lock for userSite={} failed.", userSite.getUserSiteId());
                    continue;
                }
                continue;
            }
            final PostgresUserSiteLock userSiteLock = optionalUserSiteLock.get();
            if (!userSiteLock.getActivityId().equals(inProgressActivityId)) {
                log.error("refresh, lock problem: called with inProgressActivityId={} but userSite={} is instead locked by activityId={}.", inProgressActivityId, userSite.getUserSiteId(), userSiteLock.getActivityId());
                return Pair.of(inProgressActivityId, emptyList());
            }
        }

        log.info("refresh: requested={}, eligible={}, locked={}. activityId={}", userSites.size(), userSites.size(), userSites.size(), inProgressActivityId);
        return Pair.of(inProgressActivityId, userSites);
    }

    Optional<? extends UserSiteStartEvent> createStartEvent(@NonNull Collection<PostgresUserSite> lockedUserSites,
                                                            @NonNull UUID activityId,
                                                            @NonNull UserSiteActionType userSiteActionType,
                                                            ClientUserToken clientUserToken) {
        switch (userSiteActionType) {
            case USER_REFRESH:
                return Optional.of(new RefreshUserSitesEvent(
                        clientUserToken.getUserIdClaim(),
                        activityId,
                        ZonedDateTime.now(clock),
                        toUserSiteIds(lockedUserSites)
                ));
            case FLYWHEEL_REFRESH:
                // intentional fall-through
            case PROVIDER_FLYWHEEL_REFRESH:
                return Optional.of(new RefreshUserSitesFlywheelEvent(
                        clientUserToken.getUserIdClaim(),
                        activityId,
                        ZonedDateTime.now(clock),
                        toUserSiteIds(lockedUserSites)
                ));
            case CREATE_USER_SITE:
                // There should always be exactly 1 IUserSite.
                assert lockedUserSites.size() == 1;
                return lockedUserSites.stream()
                        .map(createdUserSite -> new CreateUserSiteEvent(
                                clientUserToken.getUserIdClaim(),
                                createdUserSite.getSiteId(),
                                activityId,
                                siteService.getSiteName(createdUserSite.getSiteId()),
                                ZonedDateTime.now(clock),
                                createdUserSite.getUserSiteId()))
                        .findFirst();
            case UPDATE_USER_SITE:
                // There should always be exactly 1 IUserSite.
                assert lockedUserSites.size() == 1;
                return lockedUserSites.stream()
                        .map(updatedUserSite -> new UpdateUserSiteEvent(
                                clientUserToken.getUserIdClaim(),
                                updatedUserSite.getSiteId(),
                                activityId,
                                siteService.getSiteName(updatedUserSite.getSiteId()),
                                ZonedDateTime.now(clock),
                                updatedUserSite.getUserSiteId()))
                        .findFirst();
            default:
                log.error("Unable to handle user-site action-type: {}", userSiteActionType);
                return Optional.empty();
        }
    }

    Optional<AccessMeansHolder> getAccessMeans(PostgresUserSite userSite, UUID activityId, ClientUserToken clientUserToken, String psuIpAddress) {
        var accessMeansResult = accessMeansManager.retrieveValidAccessMeans(
                clientUserToken,
                userSite,
                Instant.now(clock),
                psuIpAddress
        );

        switch (accessMeansResult.getResultCode()) {
            case ACCESS_MEANS_DO_NOT_EXIST -> {
                log.error("Error while refreshing user-site {}: the user-site is eligible, however, there are no access means in the db.", userSite.getUserSiteId());
                markAsFailed(userSite, clientUserToken, activityId, ConnectionStatus.DISCONNECTED, FailureReason.AUTHENTICATION_FAILED);
                return Optional.empty();
            }
            case DIRECT_CONNECTION_PROVIDER_ERROR_COULD_NOT_RENEW_BECAUSE_CONSENT_EXPIRED -> {
                var connectionStatus = customExpiredConsentFlowService.shouldDisconnectOnConsentExpired(userSite) ? ConnectionStatus.DISCONNECTED : ConnectionStatus.CONNECTED;
                markAsFailed(userSite, clientUserToken, activityId, connectionStatus, FailureReason.AUTHENTICATION_FAILED);
                return Optional.empty();
            }
            case UNKNOWN_ERROR -> {
                markAsFailed(userSite, clientUserToken, activityId, ConnectionStatus.CONNECTED, FailureReason.TECHNICAL_ERROR);
                return Optional.empty();
            }
            case OK -> {
                // We now know that we have valid Access Means for this IUserSite, update the status accordingly.
                userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, null, null);
                return Optional.of(accessMeansResult.getAccessMeans());
            }
            default -> throw new IllegalStateException();
        }
    }

    private void markAsFailed(PostgresUserSite userSite, ClientUserToken clientUserToken, UUID activityId, ConnectionStatus connectionStatus, FailureReason failureReason) {
        userSiteService.unlock(userSite);
        userSiteService.updateUserSiteStatus(userSite, connectionStatus, failureReason, null);
        activityService.handleFailedRefresh(clientUserToken, activityId, userSite, RefreshedUserSiteEvent.Status.FAILED);
    }

    /**
     * Map a collection of UserSites to their id.
     */
    List<UUID> toUserSiteIds(Collection<PostgresUserSite> userSites) {
        return userSites.stream()
                .map(PostgresUserSite::getUserSiteId)
                .collect(toList());
    }

    /**
     * Determine if a UserSite is in a state that permits a data fetch.
     */
    boolean isUserSiteEligibleForDataFetch(PostgresUserSite userSite) {
        // If we're migrating a usersite, we can't fetch data.
        if (MigrationConstants.IN_MIGRATION_STATUSES.contains(userSite.getMigrationStatus())) {
            return false;
        }
        UserSiteNeededAction neededAction = userSite.determineUserSiteNeededAction();
        // If a usersite "needs action", and that action is not "try to fetch data", the user has to perform some action first.
        if (neededAction != null && neededAction != UserSiteNeededAction.TRIGGER_REFRESH) {
            return false;
        }

        return true;
    }

    /**
     * Call providers over http, the call should return immediately with 202 Accepted.
     * If the call does not return with 202 Accepted, this method returns false.
     */
    boolean triggerDataFetch(
            @NonNull final ClientUserToken clientUserToken,
            @NonNull final PostgresUserSite userSite,
            @NonNull List<UserSiteTransactionStatusSummary> summaries,
            @NonNull final UUID activityId,
            @NonNull final AccessMeansHolder accessMeans,
            @NonNull final UserSiteActionType userSiteActionType,
            @Nullable final String psuIpAddress
    ) {
        /*
         * Keep track of state before we hand off the request to providers.  Storing this
         * state allows us to correlate the asynchronous responses later received by
         * {@link ProviderServiceResponseConsumer}
         */
        UUID providerRequestId = UUID.randomUUID();
        final ProviderRequest providerRequest = new ProviderRequest(
                providerRequestId,
                activityId,
                userSite.getUserId(),
                userSite.getUserSiteId(),
                userSiteActionType
        );
        providerRequestRepository.saveValidated(providerRequest);

        final List<AccountType> whiteListedAccountTypes = siteService.getSiteWhiteListedAccountType(userSite.getSiteId());

        var transactionsDerivedLowerBound = summaries.stream()
                .filter(s -> userSite.getUserSiteId().equals(s.getUserSiteId()))
                .findFirst()
                .flatMap(UserSiteTransactionStatusSummary::getTransactionRetrievalLowerBoundTimestamp);

        var userSiteAccessMeansCreated = Optional.ofNullable(accessMeans.getUserSiteAccessMeans())
                .map(UserSiteAccessMeans::getCreated);

        final Instant fetchTransactionsStartInstant = lastFetchedService
                .determineTransactionRetrievalLowerBoundTimestamp(new ClientId(clientUserToken.getClientIdClaim()),
                        Optional.ofNullable(userSite.getLastDataFetch()), userSiteAccessMeansCreated, transactionsDerivedLowerBound,
                        Optional.of(userSite.getProvider()));

        if (isScrapingSite(userSite.getProvider())) {
            try {
                final String userSiteExternalId = userSite.getExternalId();
                List<String> migratedAccountExternalIds = Collections.emptyList();
                formProviderRestClient.triggerRefreshAndFetchData(
                        userSite.getProvider(),
                        new FormTriggerRefreshAndFetchDataDTO(
                                userSiteExternalId,
                                new FormUserSiteDTO(
                                        userSite.getUserId(),
                                        userSite.getUserSiteId(),
                                        userSiteExternalId
                                ),
                                accessMeans.toAccessMeansDTO(),
                                fetchTransactionsStartInstant,
                                providerRequestId,
                                userSite.getClientId(),
                                new UserSiteDataFetchInformation(
                                        userSiteExternalId,
                                        userSite.getUserSiteId(),
                                        userSite.getSiteId(),
                                        migratedAccountExternalIds,
                                        whiteListedAccountTypes
                                ),
                                activityId,
                                userSite.getSiteId()
                        ),
                        clientUserToken
                );
            } catch (HttpException e) {
                siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(FETCH_DATA, userSite.getProvider(), e);
                log.warn("POST /trigger-refresh-and-fetch-data failed (http)", e);
                return false;
            } catch (RuntimeException e) {
                log.error("POST /trigger-refresh-and-fetch-data failed (other)", e);
                return false;
            }
        } else {
            try {
                ClientId clientId = new ClientId(clientUserToken.getClientIdClaim());
                UUID siteId = userSite.getSiteId();
                boolean forceExperimentalVersion = clientSiteService.isClientUsingExperimentalVersion(clientId, siteId);
                providerRestClient.fetchData(
                        userSite.getProvider(),
                        userSite.getSiteId(),
                        new ApiFetchDataDTO(
                                userSite.getUserId(),
                                userSite.getUserSiteId(),
                                fetchTransactionsStartInstant,
                                accessMeans.toAccessMeansDTO(),
                                authenticationMeansFactory.createAuthMeans(clientUserToken,
                                        userSite.getRedirectUrlId()
                                ),
                                providerRequestId,
                                activityId,
                                userSite.getSiteId(),
                                psuIpAddress,
                                new UserSiteDataFetchInformation(
                                        null,
                                        userSite.getUserSiteId(),
                                        userSite.getSiteId(),
                                        emptyList(),
                                        whiteListedAccountTypes
                                )
                        ),
                        clientUserToken,
                        forceExperimentalVersion
                );
            } catch (HttpException e) {
                siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(FETCH_DATA, userSite.getProvider(), e);
                log.error("POST /fetch-data failed (http)", e);
                return false;
            } catch (RuntimeException e) {
                log.error("POST /fetch-data failed (other)", e);
                return false;
            }
        }

        siteManagementMetrics.incrementCounterFetchDataStart(userSiteActionType, userSite);
        siteManagementMetrics.registerNumberOfDaysFetched(userSite.getProvider(), fetchTransactionsStartInstant);
        return true;
    }



}
