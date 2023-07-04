package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.sitemanagement.SiteManagementMetrics;
import nl.ing.lovebird.sitemanagement.configuration.ApplicationConfiguration;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.exception.KnownProviderRestClientException;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.providercallback.UserExternalId;
import nl.ing.lovebird.sitemanagement.providercallback.UserExternalIdRepository;
import nl.ing.lovebird.sitemanagement.providerclient.FormProviderRestClient;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static nl.ing.lovebird.sitemanagement.SiteManagementMetrics.ProvidersFunction.SCR_DELETE_ORPHAN_USER;
import static nl.ing.lovebird.sitemanagement.SiteManagementMetrics.ProvidersFunction.SCR_GET_EXT_USER_ID;

@Service
@AllArgsConstructor
@Slf4j
public class OrphanUserService {

    private static final int ORPHAN_USER_EXTERNAL_ID_FETCH_SIZE = 1000;
    private static final int ORPHAN_USER_FETCH_SIZE = 1000;

    private final Clock clock;
    private final OrphanUserBatchRepository orphanUserBatchRepository;
    private final OrphanUserRepository orphanUserRepository;
    private final OrphanUserExternalIdRepository orphanUserExternalIdRepository;
    private final FormProviderRestClient formProviderRestClient;
    private final UserExternalIdRepository userExternalIdRepository;
    private final UserSiteService userSiteService;
    private final OrphanUserAllowedExecutions allowedExecutions;
    private final SiteManagementMetrics siteManagementMetrics;

    public UUID startPreparingBatch(@NotNull final ClientToken clientToken, @NotNull final String provider) {
        final UUID batchId;
        try {
            batchId = formProviderRestClient.fetchProviderExternalUserIds(provider, clientToken);
        } catch (HttpException e) {
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(SCR_GET_EXT_USER_ID, provider, e);
            log.warn("Failed to get external-user-ids for provider {}", provider);
            throw new KnownProviderRestClientException(e);
        }
        savePrepareInitiatedBatch(new ClientId(clientToken.getClientIdClaim()), provider, batchId);
        return batchId;
    }

    private void savePrepareInitiatedBatch(final ClientId clientId, final String provider, final UUID batchId) {
        if (orphanUserBatchRepository.get(clientId, provider, batchId).isPresent()) {
            throw new OrphanUserBatchAlreadyExistsException(batchId, provider);
        }

        final Instant now = Instant.now(clock);
        orphanUserBatchRepository.save(new OrphanUserBatch(clientId, provider, batchId, now, now, OrphanUserBatch.Status.PREPARE_INITIATED));
    }

    public void findAndSaveOrphanUsers(@NotNull final ClientId clientId, @NotNull final String provider, @NotNull final UUID batchId) {
        final List<OrphanUserExternalId> externalIds = orphanUserExternalIdRepository
                .getForBatchAndProvider(clientId, provider, batchId, ORPHAN_USER_EXTERNAL_ID_FETCH_SIZE);
        externalIds.stream()
                .filter(orphanUserExternalId -> isUserOrphaned(orphanUserExternalId.getProvider(), orphanUserExternalId.getExternalUserId()))
                .forEach(orphanUserExternalId -> orphanUserRepository.save(toOrphanUser(orphanUserExternalId, OrphanUser.Status.INITIAL, Instant.now(clock))));
    }

    /**
     * A user is considered to be orphaned if he/she still exists at provider but does not have any user-sites within Yolt anymore.
     *
     * @param provider       - a provider to check for orphaned users
     * @param externalUserId - user id at provider
     * @return - true if user is orphaned, otherwise false
     */
    private boolean isUserOrphaned(final String provider, final String externalUserId) {
        final Optional<UserExternalId> externalIdUser = userExternalIdRepository.findByProviderAndExternalUserId(provider, externalUserId);
        if (externalIdUser.isEmpty()) {
            log.warn("External user id {} for provider {} is not found in database, this may indicate that we deleted " +
                    "some entries from external_id_user table that we should not do in any case, skipping this user " +
                    "from preparing orphaned users batch", externalUserId, provider);
            return false;
        }

        final List<PostgresUserSite> userSites = userSiteService.getNonDeletedUserSites(externalIdUser.get().getUserId()).stream()
                .filter(userSite -> provider.equals(userSite.getProvider())).toList();
        return userSites.isEmpty();
    }

    private static OrphanUser toOrphanUser(OrphanUserExternalId orphanUserExternalId, OrphanUser.Status status, Instant created) {
        return new OrphanUser(orphanUserExternalId.getClientId(), orphanUserExternalId.getProvider(), orphanUserExternalId.getOrphanUserBatchId(),
                orphanUserExternalId.getExternalUserId(), created, created, status);
    }

    @Async(ApplicationConfiguration.BATCH_JOB_EXECUTOR)
    public void executeOrphanUserBatch(@NotNull final ClientToken clientToken, @NotNull final String provider, @NotNull final UUID batchId) {
        final Optional<OrphanUserBatch> batchOptional = orphanUserBatchRepository.get(new ClientId(clientToken.getClientIdClaim()), provider, batchId);
        if (batchOptional.isEmpty()) {
            throw new OrphanUserBatchNotFoundException(provider, batchId);
        }

        final OrphanUserBatch orphanUserBatch = batchOptional.get();
        if (OrphanUserBatch.Status.PREPARE_PROCESSING_FINISHED != orphanUserBatch.getStatus()) {
            throw new OrphanUserBatchInvalidStateException(OrphanUserBatch.Status.PREPARE_PROCESSING_FINISHED, orphanUserBatch.getStatus());
        }

        final List<OrphanUser> orphanUsers = orphanUserRepository.listOrphanUsers(new ClientId(clientToken.getClientIdClaim()), provider, batchId, ORPHAN_USER_FETCH_SIZE);
        if (orphanUsers.isEmpty()) {
            log.info("No orphaned users found for provider {} within batch {}", provider, batchId);
            orphanUserBatchRepository.updateStatus(new ClientId(clientToken.getClientIdClaim()), provider, batchId, OrphanUserBatch.Status.EXECUTE_EMPTY);
            return;
        }

        final boolean isSuccess = orphanUsers.stream()
                .map(orphanUser -> deleteOrphanUserAtProvider(clientToken, orphanUser))
                .reduce(true, (first, second) -> first && second);

        if (isSuccess) {
            orphanUserBatchRepository.updateStatus(new ClientId(clientToken.getClientIdClaim()), provider, batchId, OrphanUserBatch.Status.EXECUTE_FINISHED_SUCCESS);
        } else {
            orphanUserBatchRepository.updateStatus(new ClientId(clientToken.getClientIdClaim()), provider, batchId, OrphanUserBatch.Status.EXECUTE_FINISHED_WITH_ERRORS);
        }
    }

    private boolean deleteOrphanUserAtProvider(final ClientToken clientToken, final OrphanUser orphanUser) {
        log.info("Sending request for deleting orphan user with external user id {} to provider {}", orphanUser.getExternalUserId(), orphanUser.getProvider()); //NOSHERIFF
        try {
            formProviderRestClient.deleteOrphanUserAtProvider(orphanUser.getProvider(), orphanUser.getExternalUserId(), clientToken);
            orphanUserRepository.updateStatus(new ClientId(clientToken.getClientIdClaim()), orphanUser.getProvider(), orphanUser.getOrphanUserBatchId(),
                    orphanUser.getExternalUserId(), OrphanUser.Status.DELETED);
            return true;
        } catch (HttpException e) {
            siteManagementMetrics.incrementCounterUnhandledProvidersHttpError(SCR_DELETE_ORPHAN_USER, orphanUser.getProvider(), e);
            log.warn("Error occurred when deleting orphan user with external user id {} at provider {}",
                    orphanUser.getExternalUserId(), orphanUser.getProvider(), e); //NOSHERIFF
            orphanUserRepository.updateStatus(new ClientId(clientToken.getClientIdClaim()), orphanUser.getProvider(), orphanUser.getOrphanUserBatchId(),
                    orphanUser.getExternalUserId(), OrphanUser.Status.ERROR);
            return false;
        } catch (Exception e) {
            log.error("Error occurred when deleting orphan user with external user id {} at provider {}",
                    orphanUser.getExternalUserId(), orphanUser.getProvider(), e); //NOSHERIFF
            orphanUserRepository.updateStatus(new ClientId(clientToken.getClientIdClaim()), orphanUser.getProvider(), orphanUser.getOrphanUserBatchId(),
                    orphanUser.getExternalUserId(), OrphanUser.Status.ERROR);
            return false;
        }
    }

    public List<OrphanUserBatchDTO> listOrphanUserBatches(@NotNull final ClientToken clientToken, @NotNull final String provider) {
        return orphanUserBatchRepository.list(new ClientId(clientToken.getClientIdClaim()), provider).stream()
                .map(this::toOrphanUserBatchDTO)
                .sorted()
                .collect(Collectors.toList());
    }

    public OrphanUserBatchDTO getOrphanUserBatch(@NotNull final ClientToken clientToken, @NotNull final String provider, @NotNull final UUID orphanUserBatchId) {
        OrphanUserBatch orphanUserBatch = orphanUserBatchRepository.get(new ClientId(clientToken.getClientIdClaim()), provider, orphanUserBatchId)
                .orElseThrow(() -> new OrphanUserBatchNotFoundException(provider, orphanUserBatchId));
        return toOrphanUserBatchDTO(orphanUserBatch);
    }

    public List<OrphanUserDTO> listOrphanUsers(@NotNull final ClientToken clientToken, @NotNull final String provider, @NotNull final UUID orphanUserBatchId) {
        final List<OrphanUser> orphanUserList = orphanUserRepository.listOrphanUsers(new ClientId(clientToken.getClientIdClaim()), provider, orphanUserBatchId, ORPHAN_USER_FETCH_SIZE);
        return orphanUserList.stream()
                .map(OrphanUserService::toOrphanUserDTO)
                .collect(Collectors.toList());
    }

    private OrphanUserBatchDTO toOrphanUserBatchDTO(final OrphanUserBatch orphanUserBatch) {
        return new OrphanUserBatchDTO(
                orphanUserBatch.getOrphanUserBatchId(),
                orphanUserBatch.getProvider(),
                orphanUserBatch.getCreatedTimestamp(),
                orphanUserBatch.getUpdatedTimestamp(),
                orphanUserBatch.getStatus().name(),
                allowedExecutions.isAllowed(orphanUserBatch.getProvider(), orphanUserBatch.getOrphanUserBatchId()));
    }

    private static OrphanUserDTO toOrphanUserDTO(final OrphanUser orphanUser) {
        return new OrphanUserDTO(
                orphanUser.getOrphanUserBatchId(),
                orphanUser.getProvider(),
                orphanUser.getExternalUserId(),
                orphanUser.getCreatedTimestamp(),
                orphanUser.getUpdatedTimestamp(),
                orphanUser.getStatus().name()
        );
    }

    public void deleteBatchData(@NotNull final ClientToken clientToken, @NotNull final String provider, @NotNull final UUID orphanUserBatchId) {
        orphanUserBatchRepository.delete(new ClientId(clientToken.getClientIdClaim()), provider, orphanUserBatchId);
        orphanUserRepository.delete(new ClientId(clientToken.getClientIdClaim()), provider, orphanUserBatchId);
        orphanUserExternalIdRepository.delete(new ClientId(clientToken.getClientIdClaim()), provider, orphanUserBatchId);
    }
}
