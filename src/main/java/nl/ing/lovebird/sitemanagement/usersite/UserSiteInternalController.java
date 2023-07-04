package nl.ing.lovebird.sitemanagement.usersite;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.sitemanagement.exception.UserSiteNotFoundException;
import nl.ing.lovebird.sitemanagement.lib.documentation.Internal;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.lib.validation.IpAddress;
import nl.ing.lovebird.sitemanagement.usersitedelete.UserSiteDeleteService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static nl.ing.lovebird.sitemanagement.lib.PsuIpAddress.PSU_IP_ADDRESS_HEADER_NAME;
import static nl.ing.lovebird.sitemanagement.lib.documentation.Internal.Service.*;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Internal operations, these are not available to clients.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class UserSiteInternalController {
    public static final String USER_ID_HEADER_KEY = "user-id";
    private final UserSiteService userSiteService;
    private final UserSiteDeleteService userSiteDeleteService;
    private final WebClient.Builder webClientBuilder;
    private final Clock clock;
    private final ClientTokenRequesterService clientTokenRequesterService;

    /**
     * Move an account from one usersite to another.  If this operation completes successfully, the account pointed to
     * by {@link MoveAccountDTO#accountId} will have been deleted from {@link MoveAccountDTO#fromUserSiteId} and added
     * to {@link MoveAccountDTO#toUserSiteId} from the perspective of a user.
     * <p>
     * The two usersites (from and to) must have the same Provider and they must belong to the same user,
     * identified by {@link MoveAccountDTO#userId}.
     * <p>
     * This functionality was builtat the start of 2021 to help move accounts across usersites for
     * some customers.  This functionality is not regularly used.
     */
    @Internal(batchTrigger)
    @PostMapping(value = "/internal/user-sites/-/move-account")
    public ResponseEntity<Void> moveAccount(@Valid @RequestBody MoveAccountDTO moveAccount) {
        boolean moveToExistingUserSite = moveAccount.toUserSiteId != null;
        final PostgresUserSite userSiteFrom, userSiteTo;
        try {
            userSiteFrom = userSiteService.getUserSite(moveAccount.userId, moveAccount.fromUserSiteId);
        } catch (UserSiteNotFoundException e) {
            log.error("moveAccount failed, cannot find usersite={}", moveAccount.fromUserSiteId, e);
            return ResponseEntity.badRequest().build();
        }
        if (moveToExistingUserSite) {
            // We are moving an account between two existing user-sites.
            try {
                userSiteTo = userSiteService.getUserSite(moveAccount.userId, moveAccount.toUserSiteId);
            } catch (UserSiteNotFoundException e) {
                log.error("moveAccount failed, cannot find usersite={}.", moveAccount.toUserSiteId, e);
                return ResponseEntity.badRequest().build();
            }
            if (!userSiteFrom.getSiteId().equals(userSiteTo.getSiteId())) {
                log.error("moveAccount failed, cannot move account, sites are different: from={} and to={}.",
                        userSiteFrom.getSiteId(), userSiteTo.getSiteId());
                return ResponseEntity.badRequest().build();
            }
        } else {
            // We are splitting an existing user-site and will have to create a new user-site on-the-fly.
            userSiteTo = new PostgresUserSite(
                    userSiteFrom.getUserId(), UUID.randomUUID(), userSiteFrom.getSiteId(), null, ConnectionStatus.DISCONNECTED, FailureReason.CONSENT_EXPIRED, null, Instant.now(clock), Instant.now(clock), null, userSiteFrom.getClientId(), userSiteFrom.getProvider(), null, userSiteFrom.getRedirectUrlId(), userSiteFrom.getPersistedFormStepAnswers(), false, null);
            // Will throw if insert fails.
            userSiteService.createNew(userSiteTo);
            log.info("moveAccount created userSiteTo with UserSite.id={}", userSiteTo.getUserSiteId());
        }

        var webClient = webClientBuilder.build();

        // Grab a clientUserToken.
        var clientUserToken = clientTokenRequesterService.getClientUserToken(userSiteFrom.getClientId().unwrap(), moveAccount.userId);

        // Make sure that the account exists.
        var accounts = webClient.get()
                .uri("https://accounts-and-transactions/accounts-and-transactions/v1/users/{userId}/accounts?userSiteId={userSiteId}",
                        clientUserToken.getUserIdClaim(),
                        moveAccount.fromUserSiteId
                )
                .header(CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .retrieve()
                .toEntityList(Map.class)
                .block();

        boolean accountExistsInUserSite = accounts.getBody().stream()
                .map(o -> UUID.fromString((String) o.get("id")))
                .anyMatch(moveAccount.accountId::equals);
        if (!accountExistsInUserSite) {
            log.error("moveAccount failed, account={} is not part of usersite={}, request={}", moveAccount.accountId, moveAccount.fromUserSiteId, moveAccount.toString()); //NOSHERIFF
            return ResponseEntity.badRequest().build();
        }

        // We have checked that the user sites belong to the same user, that the providers are equal for both user sites.
        // Additionally, we have verified that the provided accountId exists and belongs to the fromUserSite.  We can
        // now proceed to move the account between UserSites.  To do this, we will call a PATCH endpoint in
        // accounts-and-transactions that does the actual work.  Since A&T is the source of truth of account data,
        // the A&T service is responsible for propagating the call to wherever it needs to go.

        var resp = webClient.patch()
                .uri("https://accounts-and-transactions/accounts-and-transactions/internal/users/{userId}/account/{accountId}",
                        clientUserToken.getUserIdClaim(),
                        moveAccount.accountId
                )
                .header(CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(ALL)
                .bodyValue(new PatchAccountDTO(userSiteTo.getUserSiteId()))
                .retrieve()
                .toBodilessEntity()
                .block();

        if (resp == null || resp.getStatusCodeValue() != 200) {
            log.error("moveAccount failed, received http {} from accounts-and-transactions, request={}.", resp.getStatusCodeValue(), moveAccount.toString()); //NOSHERIFF
            return ResponseEntity.status(500).build();
        }

        // Everything succeeded.
        if (moveToExistingUserSite) {
            // Moved the account to an existing usersite.
            log.info("moveAccount request OK, request={}.", moveAccount.toString()); //NOSHERIFF
        } else {
            // Moved the account to a newly created usersite.
            log.info("moveAccount request OK, moved account to ad-hoc created userSiteTo={}, request={}.", userSiteTo, moveAccount.toString()); //NOSHERIFF
        }

        return ResponseEntity.ok().build();
    }

    /**
     * A request to move {@link #accountId} from {@link #fromUserSiteId} to {@link #toUserSiteId} for {@link #userId}.
     */
    public record MoveAccountDTO(@NotNull UUID userId, @NotNull UUID fromUserSiteId,
                                 @Nullable UUID toUserSiteId, @NotNull UUID accountId) {
    }

    @Value
    public static class PatchAccountDTO {
        @NonNull UUID userSiteId;
    }


    /**
     * Retrieves an external user-site id
     */
    @Internal(providers)
    @GetMapping(value = "/user-sites/{userSiteId}/external", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ExternalUserSiteIdDTO> getExternalId(@PathVariable final UUID userSiteId,
                                                               @RequestHeader(name = USER_ID_HEADER_KEY) final UUID userId) {

        log.debug("Request to get external user site id for site with id {} for user id {}.", userSiteId, userId);

        final PostgresUserSite userSite = userSiteService.getUserSite(userId, userSiteId);

        final ExternalUserSiteIdDTO externalUserSiteIdDTO = new ExternalUserSiteIdDTO(userSite.getExternalId());

        return ResponseEntity.ok(externalUserSiteIdDTO);

    }

    /**
     * Sets an external user-site id
     */
    @Internal(providers)
    @PutMapping(value = "/user-sites/{userSiteId}/external", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> putExternalId(@PathVariable final UUID userSiteId,
                                              @RequestHeader(name = USER_ID_HEADER_KEY) final UUID userId,
                                              @Valid @RequestBody final ExternalUserSiteIdDTO externalUserSiteIdDTO) {

        final String externalUserSiteId = externalUserSiteIdDTO.getExternalUserSiteId();
        log.info("Request to set external user site id for user site {} and user id {} to value {}.", userSiteId, userId, externalUserSiteId); //NOSHERIFF

        userSiteService.updateExternalId(userId, userSiteId, externalUserSiteId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Removes all data for a user-site from site-management and the provider
     */
    @Internal(maintenance)
    @DeleteMapping(value = "/user-sites/{userId}/{userSiteId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteUserSiteInternal(
            @PathVariable @NotNull final UUID userId,
            @PathVariable @NotNull final UUID userSiteId,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action. If not, it can be left empty.")
            @Nullable @IpAddress @RequestHeader(name = PSU_IP_ADDRESS_HEADER_NAME, required = false) String psuIpAddress
    ) {
        userSiteDeleteService.deleteUserSite(userId, userSiteId, psuIpAddress);
        return ResponseEntity.ok().build();
    }

    /**
     * Resets the last fetch date for a usersite, so that the next time it will be refreshed, a longer period will be fetched.
     */
    @Internal(managementPortals)
    @PatchMapping(value = "/user-sites/{userId}/{userSiteId}/reset-last-data-fetch", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resetLastFetchDate(@PathVariable final UUID userId, @PathVariable final UUID userSiteId) {
        log.debug("Request to refresh user-site {} for user id {}.", userSiteId, userId);

        userSiteService.resetLastDataFetch(userId, userSiteId);

        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves an user-id.
     */
    @Internal(managementPortals)
    @GetMapping(value = "/user-sites/{userSiteId}/user", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<UserIdDTO> getUserId(@PathVariable final UUID userSiteId,
                                               @VerifiedClientToken(restrictedTo = {"assistance-portal-yts", "dev-portal"}) ClientToken clientToken) {

        log.debug("Request to get user id for user site with id {} for client id {}.", userSiteId, new ClientId(clientToken.getClientIdClaim()));

        final PostgresUserSite userSite = userSiteService.getUserSiteByClientId(new ClientId(clientToken.getClientIdClaim()), userSiteId);

        final UserIdDTO userIdDTO = new UserIdDTO(userSite.getUserId());

        return ResponseEntity.ok(userIdDTO);

    }
}
