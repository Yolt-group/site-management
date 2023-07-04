package nl.ing.lovebird.sitemanagement.usersite;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.accessmeans.AccessMeansManager;
import nl.ing.lovebird.sitemanagement.exception.UserSiteNotFoundException;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository.UserSiteConnectionInfo;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSiteRepository.UserSiteTotalsInfo;
import nl.ing.lovebird.sitemanagement.usersiteevent.UserSiteEventService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.*;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteService.GeneralizedConnectionStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "siteManagementUserSiteCache")
public class UserSiteService {

    private final Clock clock;
    private final PostgresUserSiteRepository postgresUserSiteRepository;
    private final PostgresUserSiteLockRepository userSiteLockRepository;
    private final UserSiteEventService userSiteEventService;
    private final AccessMeansManager accessMeansManager;
    private final SiteManagementMetrics siteManagementMetrics;
    private final SitesProvider sitesProvider;

    public List<PostgresUserSite> getNonDeletedUserSites(final UUID userId) {
        return postgresUserSiteRepository.getUserSites(userId).stream()
                .filter(userSite -> !userSite.isDeleted())
                .collect(Collectors.toList());
    }

    /**
     * Be really carefull using this method. This returns *all* usersites, also the ones that are scheduled for deletion.
     * If you want a list of usersites, you probably want {@link #getNonDeletedUserSites(UUID)} Hence the weird name of this method.
     */
    public List<PostgresUserSite> getAllUserSitesIncludingDeletedOnes(final UUID userId) {
        return postgresUserSiteRepository.getUserSites(userId);
    }

    /**
     * @deprecated Don't use this anymore. This assumes users <--> site is a 1..1 relationship, while we could have 1..*
     */
    @Deprecated
    public Optional<PostgresUserSite> findExistingUserSite(final UUID userId, final UUID siteId) {
        final List<PostgresUserSite> userSites = getNonDeletedUserSites(userId);
        return userSites.stream().filter(us -> us.getSiteId().equals(siteId)).findFirst();
    }

    public PostgresUserSite getUserSite(final UUID userId, final UUID userSiteId) {
        final PostgresUserSite userSite = postgresUserSiteRepository.getUserSite(userId, userSiteId)
                .orElse(null);

        if (userSite == null) {
            final String message = format("User site %s does not exist, or is not owned by user %s.", userSiteId, userId);
            throw new UserSiteNotFoundException(message);
        } else if (userSite.isDeleted()) {
            final String message = format("User site %s is marked for deletion.", userSiteId);
            throw new UserSiteNotFoundException(message);
        }
        return userSite;
    }

    public PostgresUserSite getUserSiteByClientId(final ClientId clientId, final UUID userSiteId) {
        final Optional<PostgresUserSite> userSite = postgresUserSiteRepository.getUserSiteByClientId(clientId, userSiteId);

        if (userSite.isEmpty()) {
            final String message = format("User site %s does not exist, or is not owned by client %s.", userSiteId, clientId);
            throw new UserSiteNotFoundException(message);
        } else if (userSite.get().isDeleted()) {
            final String message = format("User site %s is marked for deletion.", userSiteId);
            throw new UserSiteNotFoundException(message);
        }
        return userSite.get();
    }

    /**
     * Change the status of a {@link PostgresUserSite}.
     *
     * @param userSite         the user site whose status will be changed
     * @param connectionStatus the new status to set
     * @param failureReason    the new reason to set
     * @param statusTimeout    the timeout of the STEP, for scraping sites only
     */
    public void updateUserSiteStatus(
            final PostgresUserSite userSite,
            @NonNull final ConnectionStatus connectionStatus,
            @Nullable final FailureReason failureReason,
            @Nullable final Instant statusTimeout
    ) {
        if (statusTimeout != null && connectionStatus != ConnectionStatus.STEP_NEEDED) {
            throw new IllegalArgumentException("Misuse: cannot set statusTimeout without also setting STEP_NEEDED.");
        }

        if (userSite.getConnectionStatus() == connectionStatus && userSite.getFailureReason() == failureReason) {
            // Equal status and reason mean calling this function is a no-op.
            // Assumption: statusTimeout is never updated by itself.  We therefore don't check that field.
            return;
        }

        // Track status updates with a metric.  We assume that a reason is only ever updated if the status is also updated.
        siteManagementMetrics.incrementUserSiteStatusUpdate(
                userSite.getProvider(),
                userSite.getConnectionStatus(), // Current status
                userSite.getFailureReason(), // Current reason
                connectionStatus, // New status
                failureReason // New reason
        );

        // Change object in-memory (status fields).
        userSite.setConnectionStatus(connectionStatus);
        userSite.setFailureReason(failureReason);
        userSite.setStatusTimeoutTime(statusTimeout);
        userSite.setUpdated(Instant.now(clock));

        // Change IUserSite in the database.
        postgresUserSiteRepository.save(userSite);

        // Send out an update over Kafka if the status has changed (after we have persisted it ourselves).
        userSiteEventService.publishUserSiteUpdate(userSite.getUserId(), userSite.getUserSiteId(), userSite.getSiteId());
    }

    public void updateLastDataFetch(PostgresUserSite userSite, Instant lastDataFetch) {
        userSite.setLastDataFetch(lastDataFetch);
        userSite.setUpdated(Instant.now(clock));
        postgresUserSiteRepository.save(userSite);
    }

    public void updateUserSitePersistedFields(PostgresUserSite userSite, Map<String, String> persistedFormStepAnswers) {
        userSite.setPersistedFormStepAnswers(persistedFormStepAnswers);
        userSite.setUpdated(Instant.now(clock));
        postgresUserSiteRepository.save(userSite);
    }

    public void markUserSitesConnected(@NonNull UUID userId, @NonNull List<UUID> userSiteIds) {
        userSiteIds.forEach(userSiteId -> {
            try {
                final PostgresUserSite userSite = getUserSite(userId, userSiteId);
                updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, null, null);
            } catch (UserSiteNotFoundException e) {
                log.info("Could not find user site " + userSiteId + ". This was probably already deleted.", e);
            }
        });
    }

    List<PostgresUserSite> getUserSitesWithStepNeeded(int limit) {
        return postgresUserSiteRepository.getUserSitesWithStatus(ConnectionStatus.STEP_NEEDED, limit);
    }

    /**
     * Tries to lock the {@link PostgresUserSite}
     *
     * @return true if the user site has been locked, false if it was already locked.
     */
    public boolean attemptLock(final PostgresUserSite userSite, final UUID activityId) {
        return userSiteLockRepository.attemptLock(userSite.getUserSiteId(), activityId);
    }

    /**
     * Unlock a {@link PostgresUserSite}
     *
     * @param userSite the {@link PostgresUserSite} to lock
     */
    public void unlock(final PostgresUserSite userSite) {
        userSiteLockRepository.unlockUserSite(userSite.getUserSiteId());
    }

    /**
     * Fetches the lock, without attempting to put a lock on the {@link PostgresUserSite}.
     */
    public Optional<PostgresUserSiteLock> checkLock(final PostgresUserSite userSite) {
        return userSiteLockRepository.get(userSite.getUserSiteId());
    }

    @Transactional
    void updateExternalId(UUID userId, UUID userSiteId, String externalUserSiteId) {
        PostgresUserSite userSite = postgresUserSiteRepository.getUserSiteWithWriteLock(userId, userSiteId)
                .orElseThrow(() -> new UserSiteNotFoundException(format("The user-site(%s) for user(%s) could not be found", userSiteId, userId)));
        userSite.setExternalId(externalUserSiteId);
        userSite.setUpdated(Instant.now(clock));
        postgresUserSiteRepository.save(userSite); // not strictly needed because of @Transactional
    }

    void createNew(PostgresUserSite userSite) {
        postgresUserSiteRepository.save(userSite);
        userSiteEventService.publishCreatedUserSite(userSite);
    }

    /**
     * Map one {@link PostgresUserSite} to a {@link UserSiteDTO}.
     * <p>
     * Use {@link #toUserSiteDTOs(List)} when mapping more than 1 {@link PostgresUserSite}
     */
    public UserSiteDTO toUserSiteDTO(PostgresUserSite userSite) {
        Optional<Instant> consentValidFrom = accessMeansManager.retrieveAccessMeansCreatedAtForUserSite(userSite);
        return createUserSiteDTOV2(userSite, consentValidFrom.orElse(null));
    }

    /**
     * Map one or more {@link PostgresUserSite} to a {@link UserSiteDTO}.
     *
     * <p>Note: This method operates on a list because we need additional information from the database to perform
     * the mapping to a {@link UserSiteDTO}.  We prefer to execute 1 query and to then map multiple user sites at once.
     */
    public List<UserSiteDTO> toUserSiteDTOs(List<PostgresUserSite> userSites) {
        if (userSites.isEmpty()) {
            return Collections.emptyList();
        }
        final UUID userId = userSites.get(0).getUserId();

        // Retrieve additional information needed to map the UserSites.
        Map<UUID, Instant> userSiteIdToConsentValidFromMap = accessMeansManager.retrieveAccessMeansCreatedAtForUser(userId);

        return userSites.stream()
                .map(userSite -> createUserSiteDTOV2(userSite, userSiteIdToConsentValidFromMap.getOrDefault(userSite.getUserSiteId(), null)))
                .collect(Collectors.toList());
    }

    /**
     * Compile statistics for all user-sites for a give client. Statistics contain:
     * <ul>
     * <li>number of unique users</li>
     * <li>number of unique connections</li>
     * <li>number of occurrences per generalized status code</li>
     * </ul>
     *
     * @param clientId the client to for which to retrieve the statistics
     * @return a list of {@link UserSiteStatistics}
     */
    @Cacheable(unless = "#result.size()==0")
    @Transactional(readOnly = true)
    public List<UserSiteStatistics> getUserSiteStatistics(final @NonNull ClientId clientId) {

        Map<UUID, UserSiteTotalsInfo> connectionTotals
                = postgresUserSiteRepository.getUserSiteTotalsInfo(clientId);

        Map<UUID, List<UserSiteConnectionInfo>> connectionStatuses
                = postgresUserSiteRepository.getConnectionStatusBySite(clientId);

        // convert into a {@link GeneralizedConnectionStatus}
        Map<UUID, Map<GeneralizedConnectionStatus, Integer>> collect = connectionStatuses.entrySet().stream()
                .map(connectionStatusInfos -> {
                    UUID siteId = connectionStatusInfos.getKey();
                    Map<GeneralizedConnectionStatus, Integer> generalized = connectionStatusInfos.getValue()
                            .stream()
                            .map(userSiteConnectionInfo -> Pair.of(mapToGeneralizedConnectionStatus(userSiteConnectionInfo.connectionStatus, userSiteConnectionInfo.failureReason), userSiteConnectionInfo.count))
                            .collect(groupingBy(Pair::getKey, mapping(Pair::getValue, reducing(0, Integer::sum))));
                    return Pair.of(siteId, putDefaultZeroIfGeneralizedStatusIsAbsent(generalized));
                }).collect(toMap(Pair::getKey, Pair::getValue));

        return collect.entrySet().stream()
                .map(siteIdAndStatuses -> {
                    // find connections totals
                    Optional<UserSiteTotalsInfo> maybeUserSiteTotalsInfo
                            = Optional.ofNullable(connectionTotals.get(siteIdAndStatuses.getKey()));

                    return UserSiteStatistics.builder()
                            .siteId(siteIdAndStatuses.getKey())
                            .siteName(sitesProvider.findByIdOrThrow(siteIdAndStatuses.getKey()).getName())
                            .nrOfUniqueUsers(maybeUserSiteTotalsInfo.map(userSiteTotalsInfo -> userSiteTotalsInfo.nrOfUniqueUsers).orElse(0))
                            .nrOfUniqueConnections(maybeUserSiteTotalsInfo.map(userSiteTotalsInfo -> userSiteTotalsInfo.nrOfUniqueConnections).orElse(0))
                            .connectionStatuses(siteIdAndStatuses.getValue())
                            .compiledAt(ZonedDateTime.now(clock))
                            .build();
                }).collect(toList());
    }

    private GeneralizedConnectionStatus mapToGeneralizedConnectionStatus(final ConnectionStatus connectionStatus, final FailureReason failureReason) {
        if (connectionStatus == ConnectionStatus.DISCONNECTED || failureReason == FailureReason.CONSENT_EXPIRED) {
            return UNABLE_TO_LOGIN;
        } else if (connectionStatus == ConnectionStatus.CONNECTED) {
            if (failureReason != null) {
                return ERROR;
            }

            return ACTIVE;
        }

        return OTHER;
    }

    public Map<GeneralizedConnectionStatus, Integer> putDefaultZeroIfGeneralizedStatusIsAbsent(final Map<GeneralizedConnectionStatus, Integer> statuses) {
        for (GeneralizedConnectionStatus status : GeneralizedConnectionStatus.values()) {
            statuses.putIfAbsent(status, 0);
        }
        return statuses;
    }

    @CacheEvict(allEntries = true)
    @Scheduled(fixedDelay = 60_000) /* 1 minute in milliseconds */
    public void cacheEvict() {
        log.debug("Evicting `usersite` cache.");
    }

    /**
     * Mark a {@link PostgresUserSite} as deleted by setting the {@link PostgresUserSite#setDeleted(boolean)}
     * TODO: throw exception instead of suppressing the exception
     *
     * @param userId     the user-id
     * @param userSiteId the user-site to mark as deleted
     */
    @Transactional
    public void markAsDeleted(UUID userId, final UUID userSiteId) {
        postgresUserSiteRepository.getUserSiteWithWriteLock(userId, userSiteId)
                .ifPresentOrElse(
                        postgresUserSite -> {
                            postgresUserSite.markAsDeleted(clock);
                            postgresUserSiteRepository.save(postgresUserSite);
                            log.info("marked {} for deletion", postgresUserSite.getUserSiteId());
                        },
                        () -> log.error("Failed to mark user-site {} for deletion. User-site does not exist.", userSiteId));
    }

    /**
     * Reset the last data fetch for the given <code>userId</code> and <code>userSiteId</code>
     *
     * @param userId     the user id
     * @param userSiteId the user-site id
     */
    @Transactional
    public void resetLastDataFetch(final UUID userId, final UUID userSiteId) {
        PostgresUserSite userSite = postgresUserSiteRepository.getUserSiteWithWriteLock(userId, userSiteId)
                .orElseThrow(() -> new UserSiteNotFoundException(format("The user-site(%s) for user(%s) could not be found", userSiteId, userId)));

        userSite.resetLastDataFetch();
        userSite.setUpdated(Instant.now(clock));
        postgresUserSiteRepository.save(userSite); // not strictly needed because of @Transactional
    }

    public void updateRedirectUrlId(PostgresUserSite userSite, UUID redirectUrlId) {
        userSite.setRedirectUrlId(redirectUrlId);
        userSite.setUpdated(Instant.now(clock));
        postgresUserSiteRepository.save(userSite);
    }

    @Builder
    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class UserSiteStatistics {

        @NonNull
        public final UUID siteId;
        @NonNull
        public final String siteName;
        @NonNull
        public final Integer nrOfUniqueConnections;
        @NonNull
        public final Integer nrOfUniqueUsers;
        @NonNull
        public final Map<GeneralizedConnectionStatus, Integer> connectionStatuses;
        @NonNull
        private final ZonedDateTime compiledAt;
    }

    public enum GeneralizedConnectionStatus {

        /* The user site is created, it has a token and it is not in errorStatuses: REFRESH_FINISHED, LOGIN_SUCCEEDED*/
        ACTIVE,
        /* Stuck because of a login problem or a problem when (re)adding the bankStatuses: LOGIN_FAILED */
        UNABLE_TO_LOGIN,
        /* ERROR - the user site is in errorStatuses: REFRESH_FAILED, REFRESH_TIMED_OUT, STEP_FAILED */
        ERROR,
        /* OTHER - other statuses Statuses: MFA_NEEDED, INITIAL_PROCESSING, STEP_NEEDED, STEP_TIMED_OUT, UNKNOWN */
        OTHER
    }

    private UserSiteDTO createUserSiteDTOV2(PostgresUserSite userSite, Instant consentValidFrom) {
        Site site = sitesProvider.findByIdOrThrow(userSite.getSiteId());

        return new UserSiteDTO(userSite.getUserSiteId(),
                userSite.getConnectionStatus(),
                userSite.getFailureReason(),
                new UserSiteDTO.SiteDTO(
                        site.getId(),
                        site.getName(),
                        site.getAccountTypeWhitelist()
                ),
                userSite.getLastDataFetch(),
                Optional.ofNullable(consentValidFrom).orElse(Instant.EPOCH),
                determineUserSiteMetadata(userSite)
        );

    }

    /**
     * Exposes a map of metadata fields about a {@link PostgresUserSite} that might be useful to a client.
     */
    private static Map<String, String> determineUserSiteMetadata(PostgresUserSite userSite) {
        if (userSite.getPersistedFormStepAnswers() == null) {
            return Collections.emptyMap();
        }
        Set<String> whiteListedPersistedFormStepAnswerKeys = Set.of(
                "region" /* Used by some French banks, a user must indicate his/her "region" in France. */
        );
        return userSite.getPersistedFormStepAnswers().entrySet().stream()
                .filter(e -> whiteListedPersistedFormStepAnswerKeys.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
