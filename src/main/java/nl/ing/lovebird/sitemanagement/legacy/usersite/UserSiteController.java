package nl.ing.lovebird.sitemanagement.legacy.usersite;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.sitemanagement.exception.FunctionalityUnavailableForOneOffAISUsersException;
import nl.ing.lovebird.sitemanagement.forms.FormValidationException;
import nl.ing.lovebird.sitemanagement.legacy.HateoasUtils;
import nl.ing.lovebird.sitemanagement.legacy.sites.sitewithcountry.SiteWithCountryDTO;
import nl.ing.lovebird.sitemanagement.lib.documentation.External;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.lib.validation.IpAddress;
import nl.ing.lovebird.sitemanagement.site.*;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.users.User;
import nl.ing.lovebird.sitemanagement.users.UserService;
import nl.ing.lovebird.sitemanagement.usersite.*;
import nl.ing.lovebird.sitemanagement.usersitedelete.UserSiteDeleteService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static nl.ing.lovebird.sitemanagement.legacy.usersite.SiteLoginController.mapToFormStepObject;
import static nl.ing.lovebird.sitemanagement.legacy.usersite.SiteLoginController.toResponse;
import static nl.ing.lovebird.sitemanagement.lib.PsuIpAddress.PSU_IP_ADDRESS_HEADER_NAME;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType.USER_REFRESH;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
@Tag(name = "user-sites", description = "Use it to manage your user-sites.")
@Deprecated
public class UserSiteController {

    public static final String USER_ID_HEADER_KEY = "user-id";

    private static final String REDIRECT_URL_ID_HEADER_KEY = "redirect-url-id";

    private final SiteService siteService;
    private final UserService userService;
    private final UserSiteService userSiteService;
    private final LegacyUserSiteService legacyUserSiteService;
    private final CreateOrUpdateUserSiteService createOrUpdateUserSiteService;
    private final SiteDTOMapper siteDTOMapper;
    private final UserSiteDeleteService userSiteDeleteService;
    private final UserSiteRefreshService userSiteRefreshService;

    private static String getSelfPath(final UUID userSiteId) {
        final String path = HateoasUtils
                .createPath(methodOn(UserSiteController.class).getUserSite(userSiteId, null, Collections.emptyList()));
        log.debug("Path to self: {}", path);
        return path;
    }

    /**
     * @deprecated use /v1/users variant: {@link UserSiteControllerV1#postLogin}
     */
    @Deprecated
    @External
    @Operation(summary = "Create or update a new user-site",
            description = "Create or update a user-site by posting a filled in form or redirect url. Any redirect url or filled in form can be " +
                    "posted to this endpoint. Whether you are updating or creating a new user site depends on whether the flow was initiated " +
                    "by 'sites/<>/initiate-user-site' or 'user-sites/<>/renew-access' " +
                    "If more steps are required in order to complete updating/creating a user site, the usersite status will be 'STEP_NEEDED'. " +
                    "The step can be retrieved on 'user-sites/<>/step', and this completed step can then also be posted on this endpoint.",
            responses = {
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation errors",
                            content = {@Content(schema = @Schema(implementation = ErrorDTO.class))}
                    ),
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful"
                    )
            })
    @PostMapping(value = "/user-sites", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<LoginResponseDTO> postLogin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "An object containing a redirect-url or a filled-in form. You need to indicate it by setting the 'loginType' value accordingly.")
            @RequestBody @Valid final LoginDTO loginDTO,
            @RequestHeader(name = USER_ID_HEADER_KEY) final UUID userId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action. If not, it can be left empty.")
            @Nullable @IpAddress @RequestHeader(name = PSU_IP_ADDRESS_HEADER_NAME, required = false) String psuIpAddress
    ) throws FormValidationException {
        if (StringUtils.isBlank(psuIpAddress)) {
            log.info("Client {} does not provide PSU_IP_ADDRESS on {}, login type : {}", clientUserToken.getClientIdClaim(), "POST /user-sites", loginDTO.getClass().getName());
        }

        final ProcessedStepResult processedStepResult = createOrUpdateUserSiteService.processPostedLogin(
                loginDTO.toLogin(clientUserToken.getUserIdClaim()), clientUserToken, psuIpAddress
        );
        PostgresUserSite userSite = userSiteService.getUserSite(userId, processedStepResult.getUserSiteId());
        final LegacyUserSiteDTO legacyUserSiteDtoWithoutLinks = legacyUserSiteService.createUserSiteDTO(userSite);

        final LoginResponseDTO loginResponseDTO = createLoginResponseDtoIncludingLinks(legacyUserSiteDtoWithoutLinks);

        if (processedStepResult.getActivityId() != null) {
            loginResponseDTO.setActivityId(processedStepResult.getActivityId());
        } else if (processedStepResult.getStep() != null) {
            Step step = processedStepResult.getStep();
            if (step instanceof RedirectStep redirectStep) {
                loginResponseDTO.setStep(new LoginResponseDTO.LegacyStepDTO(null, new RedirectStepObject(redirectStep.getRedirectUrl(), redirectStep.getStateId().toString())));
            } else if (step instanceof FormStep) {
                FormStepObject formStepDTO = mapToFormStepObject((FormStep) step);
                loginResponseDTO.setStep(new LoginResponseDTO.LegacyStepDTO(formStepDTO, null));
            } else {
                throw new UnsupportedOperationException("Not implemented for loginStep of type " + step.getClass());
            }
        }

        log.info("Returning consent step on POST /user-sites for user site {} on activity: {}, with form: {}, with redirect: {}",
                loginResponseDTO.getUserSiteId(),
                loginResponseDTO.getActivityId(),
                loginResponseDTO.getStep() != null && loginResponseDTO.getStep().getForm() != null,
                loginResponseDTO.getStep() != null && loginResponseDTO.getStep().getRedirect() != null); //NOSHERIFF (should not fail?)
        return ResponseEntity.ok(loginResponseDTO);
    }

    private LoginResponseDTO createLoginResponseDtoIncludingLinks(LegacyUserSiteDTO legacyUserSiteDTO) {
        final LoginResponseDTO loginResponseDTO = new LoginResponseDTO();

        final UUID userSiteId = legacyUserSiteDTO.getId();
        loginResponseDTO.setUserSite(HateoasHelper.enrichWithHateoasLinks(legacyUserSiteDTO));

        loginResponseDTO.setUserSiteId(userSiteId);
        loginResponseDTO.setLinks(new LoginResponseDTO.LinksDTO(
                getSelfPath(userSiteId)
        ));

        return loginResponseDTO;
    }

    /**
     * @deprecated use /v1/users variant: {@link UserSiteControllerV1#getNextStep}
     */
    @Deprecated
    @External
    @Operation(summary = "Retrieve the next step in the flow to create/update a user-site.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = {@Content(schema = @Schema(implementation = ErrorDTO.class))}
            )
    })
    @GetMapping(value = "/user-sites/{userSiteId}/step", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginStepDTO> getNextStep(@Parameter(description = "The user-site id. This value is returned when you initiated the flow via POST /user-sites.", required = true)
                                                    @PathVariable final UUID userSiteId,
                                                    @RequestHeader(name = USER_ID_HEADER_KEY) final UUID userId) {
        Step nextStep = createOrUpdateUserSiteService.getNextStepFromSession(userId, userSiteId);
        LoginStepDTO loginStepDTO = toResponse(nextStep, userSiteId);
        log.info("Returning consent step on GET /step for user site {} with form: {}, redirect: {}",
                loginStepDTO.getUserSiteId(),
                loginStepDTO.getForm() != null,
                loginStepDTO.getRedirect() != null); //NOSHERIFF
        return ResponseEntity.ok(loginStepDTO);
    }

    /**
     * @deprecated use /v1/users variant: {@link UserSiteControllerV1#renewAccess}
     */
    @Deprecated
    @External
    @Operation(
            summary = "Renew the access for a user-site",
            description = "Starts a new consent flow for a client-user. This is needed when the credentials are expired or invalid.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User site not found",
                            content = {@Content(schema = @Schema(implementation = ErrorDTO.class))}
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Technical Error or the authentication means for given provider are missing.",
                            content = {@Content(schema = @Schema(implementation = ErrorDTO.class))}
                    )
            })
    @GetMapping(value = "/user-sites/{userSiteId}/renew-access", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginStepDTO> renewAccess(
            @PathVariable final UUID userSiteId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "The redirectUrl to use. Required for sites with a DIRECT_CONNECTION. " +
                    "Might be left empty in case of a SCRAPING site where there's no redirect. ")
            @RequestHeader(name = REDIRECT_URL_ID_HEADER_KEY, required = false) final UUID redirectUrlId,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action. If not, it can be left empty.")
            @Nullable @IpAddress @RequestHeader(name = PSU_IP_ADDRESS_HEADER_NAME, required = false) String psuIpAddress
    ) {
        if (StringUtils.isBlank(psuIpAddress)) {
            log.info("Client {} does not provide PSU_IP_ADDRESS on {} for user-site {}", clientUserToken.getClientIdClaim(), "/user-sites/{userSiteId}/renew-access", userSiteId);
        }
        Step loginStep = createOrUpdateUserSiteService.createLoginStepToRenewAccess(
                clientUserToken, userSiteId, redirectUrlId, psuIpAddress
        );
        LoginStepDTO loginStepDTO = toResponse(loginStep, userSiteId);

        log.info("Returning consent step on GET /renew-access-means for user site {} with form: {}, redirect: {}",
                loginStepDTO.getUserSiteId(),
                loginStepDTO.getForm() != null,
                loginStepDTO.getRedirect() != null); //NOSHERIFF

        return ResponseEntity.ok(loginStepDTO);
    }

    /**
     * @deprecated use /v1/users variant: {@link UserSiteControllerV1#getUserSites}
     */
    @Deprecated
    @External
    @Operation(summary = "Retrieve a list of user-sites", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            )
    })
    @GetMapping(value = "/user-sites/me", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<LegacyUserSiteDTO>> getUserSites(
            @RequestHeader(name = USER_ID_HEADER_KEY) final UUID userId,
            @Parameter(description = "A list that contains objects that should also be retrieved. Currently supported: 'site'." +
                    "If set, this will populate the site field inside the response.")
            @Valid @Size(max = 1)
            @RequestParam(value = "fetchObject", required = false) final List<@Valid @Size(max = 256) String> fetchObjects) {


        final List<LegacyUserSiteDTO> legacyUserSiteDTOS = userSiteService.getNonDeletedUserSites(userId)
                .stream()
                .map(u -> createUserSiteDTOWithSiteInformationIfAvailable(fetchObjects, u))
                .collect(Collectors.toList());

        return ResponseEntity.ok(legacyUserSiteDTOS);
    }

    private LegacyUserSiteDTO createUserSiteDTOWithSiteInformationIfAvailable(List<String> fetchObjects, PostgresUserSite userSite) {
        final LegacyUserSiteDTO legacyUserSiteDTO = legacyUserSiteService.createUserSiteDTO(userSite);
        HateoasHelper.enrichWithHateoasLinks(legacyUserSiteDTO);

        if (fetchObjects != null && fetchObjects.contains("site")) {
            UUID siteId = userSite.getSiteId();
            final Site site = siteService.getSite(siteId);
            SiteWithCountryDTO siteWithCountryDTO = new SiteWithCountryDTO(
                    site.getId(),
                    site.getName(),
                    SiteDTOMapper.constructPrimaryLabel(site.getAccountTypeWhitelist()),
                    site.getAccountTypeWhitelist(),
                    isScrapingSite(site.getProvider()) ? LoginType.FORM : LoginType.URL,
                    isScrapingSite(site.getProvider()) ? ConnectionType.SCRAPER : ConnectionType.DIRECT_CONNECTION,
                    site.getServices(),
                    siteDTOMapper.getSiteLinksDTO(siteId),
                    site.getGroupingBy(),
                    site.getUsesStepTypes(),
                    site.getAvailableInCountries(),
                    SiteConnectionHealthStatus.SITE_CONNECTION_HEALH_STATUS_NOT_AVAILABLE,
                    false);

            legacyUserSiteDTO.setSite(siteWithCountryDTO);
        }
        return legacyUserSiteDTO;
    }

    /**
     * @deprecated use /v1/users variant: {@link UserSiteControllerV1#getUserSite}
     */
    @Deprecated
    @External
    @Operation(summary = "Retrieve a user-site", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            )
    })
    @GetMapping(value = "/user-sites/{userSiteId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<LegacyUserSiteDTO>
    getUserSite(@Parameter(description = "The unique user-site identifier", required = true) @PathVariable final UUID userSiteId,
                @RequestHeader(name = USER_ID_HEADER_KEY) final UUID userId,
                @Parameter(description = "A list that contains objects that should also be retrieved. Currently supported: 'site'.")
                @Valid @Size(max = 256)
                @RequestParam(value = "fetchObject", required = false) final List<@Valid @Size(max = 1024) String> fetchObjects) {

        log.debug("Request to get user site with id {} for user id {}.", userSiteId, userId);

        final PostgresUserSite userSite = userSiteService.getUserSite(userId, userSiteId);
        final LegacyUserSiteDTO legacyUserSiteDTO = createUserSiteDTOWithSiteInformationIfAvailable(fetchObjects, userSite);

        return ResponseEntity.ok(legacyUserSiteDTO);

    }

    /**
     * @deprecated use /v1/users variant {@link UserSiteControllerV1#refreshAllUserSites}
     */
    @Deprecated
    @External
    @Operation(summary = "Trigger a refresh for all user-sites of a regular user. Note: this endpoint cannot be called " +
            "for one-off AIS users.", responses = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Successfully triggered the refresh"
            )
    })
    @PutMapping(value = "/user-sites/me/refresh", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<RefreshResponseDTO> refreshAllUserSites(
            @RequestHeader(name = USER_ID_HEADER_KEY) final UUID userId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action. If not, it can be left empty.")
            @Nullable @IpAddress @RequestHeader(name = PSU_IP_ADDRESS_HEADER_NAME, required = false) String psuIpAddress
    ) {
        if (StringUtils.isBlank(psuIpAddress)) {
            log.info("Client {} does not provide PSU_IP_ADDRESS on {}", clientUserToken.getClientIdClaim(), "/user-sites/me/refresh");
        }

        log.debug("Request to refresh user-sites for user id {}.", clientUserToken.getUserIdClaim());

        final User user = userService.getUserOrThrow(clientUserToken.getUserIdClaim());
        if (user.isOneOffAis()) {
            throw new FunctionalityUnavailableForOneOffAISUsersException();
        }

        final Optional<UUID> activityId = userSiteRefreshService.refreshUserSitesBlocking(
                userSiteService.getNonDeletedUserSites(userId),
                user.isOneOffAis(),
                clientUserToken,
                USER_REFRESH,
                psuIpAddress,
                null
        );

        // Nothing to refresh: return HTTP 200 with activityId = null
        // >= 1 usersites to refresh, return HTTP 202 with the activityId
        return activityId
                .map(uuid -> ResponseEntity.accepted().body(new RefreshResponseDTO(uuid)))
                .orElseGet(() -> ResponseEntity.ok().body(new RefreshResponseDTO(null)));
    }

    /**
     * @deprecated use /v1/users variant {@link UserSiteControllerV1#refreshUserSite}
     */
    @Deprecated
    @External
    @Operation(summary = "Trigger a refresh for a user-site of a regular user. Note: this endpoint cannot be called " +
            "for one-off AIS users.", responses = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Successfully triggered the refresh"
            )
    })
    @PutMapping(value = "/user-sites/{userSiteId}/refresh", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<RefreshResponseDTO> refreshUserSite(
            @PathVariable final UUID userSiteId,
            @RequestHeader(name = USER_ID_HEADER_KEY) final UUID userId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action. If not, it can be left empty.")
            @Nullable @IpAddress @RequestHeader(name = PSU_IP_ADDRESS_HEADER_NAME, required = false) String psuIpAddress
    ) {
        if (StringUtils.isBlank(psuIpAddress)) {
            log.info("Client {} does not provide PSU_IP_ADDRESS on {} for user {} and user-site {}", clientUserToken.getClientIdClaim(), "/user-sites/{userSiteId}/refresh", userId, userSiteId);
        }

        log.debug("Request to refresh user-site {} for user id {}.", userSiteId, userId);

        final User user = userService.getUserOrThrow(clientUserToken.getUserIdClaim());
        if (user.isOneOffAis()) {
            throw new FunctionalityUnavailableForOneOffAISUsersException();
        }

        final PostgresUserSite userSite = userSiteService.getUserSite(userId, userSiteId);
        Optional<UUID> activityId = userSiteRefreshService.refreshUserSitesBlocking(
                Collections.singleton(userSite),
                user.isOneOffAis(),
                clientUserToken,
                USER_REFRESH,
                psuIpAddress,
                null
        );

        // Nothing to refresh: return HTTP 200 with activityId = null
        // >= 1 usersites to refresh, return HTTP 202 with the activityId.
        return activityId
                .map(uuid -> ResponseEntity.accepted().body(new RefreshResponseDTO(uuid)))
                .orElseGet(() -> ResponseEntity.ok().body(new RefreshResponseDTO(null)));

    }

    /**
     * @deprecated use /v1/users variant {@link UserSiteControllerV1#deleteUserSite}
     */
    @Deprecated
    @External
    @Operation(
            summary = "Remove all data for a user-site",
            description = "This will remove all data linked to the user-site, such as accounts and transactions"
    )
    @DeleteMapping(value = "/user-sites/{userSiteId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteUserSite(
            @PathVariable final UUID userSiteId,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action. If not, it can be left empty.")
            @Nullable @IpAddress @RequestHeader(name = PSU_IP_ADDRESS_HEADER_NAME, required = false) String psuIpAddress,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken
    ) {
        if (StringUtils.isBlank(psuIpAddress)) {
            log.info("Client {} does not provide PSU_IP_ADDRESS on {} for user-site: {}", new ClientId(clientUserToken.getClientIdClaim()), "DELETE /user-sites/{userSiteId}", userSiteId); //NOSHERIFF
        }
        userSiteDeleteService.deleteExternallyAndMarkForInternalDeletion(userSiteId, psuIpAddress, clientUserToken);

        return ResponseEntity.ok().build();

    }

}
