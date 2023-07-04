package nl.ing.lovebird.sitemanagement.usersite;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.annotations.AIS;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.providershared.form.ExplanationField;
import nl.ing.lovebird.providershared.form.FormComponent;
import nl.ing.lovebird.sitemanagement.forms.ExplanationFieldDTO;
import nl.ing.lovebird.sitemanagement.forms.FormComponentDTO;
import nl.ing.lovebird.sitemanagement.forms.FormDTOMapper;
import nl.ing.lovebird.sitemanagement.forms.FormValidationException;
import nl.ing.lovebird.sitemanagement.legacy.usersite.UserSiteController;
import nl.ing.lovebird.sitemanagement.lib.documentation.External;
import nl.ing.lovebird.sitemanagement.lib.validation.IpAddress;
import nl.ing.lovebird.sitemanagement.exception.UserIdMismatchException;
import nl.ing.lovebird.springdoc.annotations.ExternalApi;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.lib.PsuIpAddress.PSU_IP_ADDRESS_HEADER_NAME;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "transaction information")
public class UserSiteControllerV1 {
    private static final String USER_ID = "userId";
    private static final String REDIRECT_URL_ID = "redirectUrlId";

    private final CreateOrUpdateUserSiteService createOrUpdateUserSiteService;
    private final SiteLoginService siteLoginService;
    private final UserSiteController userSiteController;
    private final UserSiteService userSiteService;

    @External
    @ExternalApi
    @Operation(
            summary = "Create or update a new user-site",
            description = "Create or update a user-site by posting a filled in form or redirect url. Any redirect url or filled in form can be " +
                    "posted to this endpoint. Whether you are updating or creating a new user site depends on whether the flow was initiated " +
                    "by '/v1/users/<>/connect' or '/v1/users/<>/user-sites/<>/renew-access'. ",
            responses = {
                @ApiResponse(
                        responseCode = "400",
                        description = "Validation errors",
                        content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "The wrong userId provided.",
                        content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
                ),
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful"
                )
    })
    @PostMapping(value = "/v1/users/{userId}/user-sites", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    @AIS
    public ResponseEntity<LoginResponseDTO> postLogin(
            @PathVariable(name = USER_ID) final UUID userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "An object containing a redirect-url or a filled-in form. You need to indicate which of the two you are sending by setting setting the field loginType to value URL or FORM respectively.")
            @RequestBody @Valid final LoginDTO loginDTO,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action.", required = true)
            @IpAddress @RequestHeader(PSU_IP_ADDRESS_HEADER_NAME) String psuIpAddress
    ) throws FormValidationException, UserIdMismatchException {
        final UUID secureUserId = clientUserToken.getUserIdClaim();
        if (!Objects.equals(secureUserId, userId)) {
            throw new UserIdMismatchException(secureUserId, userId);
        }

        if (loginDTO instanceof FormLoginDTO && StringUtils.isBlank(((FormLoginDTO) loginDTO).getStateId())) {
            throw new IllegalArgumentException("StateId in the form is empty. This is required.");
        }

        final ProcessedStepResult processedStepResult = createOrUpdateUserSiteService.processPostedLogin(
                loginDTO.toLogin(secureUserId), clientUserToken, psuIpAddress
        );

        PostgresUserSite userSite = userSiteService.getUserSite(secureUserId, processedStepResult.getUserSiteId());
        UserSiteDTO userSiteDTOV2 = userSiteService.toUserSiteDTO(userSite);

        LoginResponseDTO loginResponseDTO = new LoginResponseDTO(null, null, userSite.getUserSiteId(), userSiteDTOV2);
        if (processedStepResult.getActivityId() != null) {
            loginResponseDTO.setActivityId(processedStepResult.getActivityId());
        } else if (processedStepResult.getStep() != null) {
            Step step = processedStepResult.getStep();
            if (step instanceof RedirectStep redirectStep) {
                String redirectUrl = redirectStep.getRedirectUrl();
                loginResponseDTO.setStep(new LoginResponseDTO.StepDTO(null, new RedirectStepObject(redirectUrl, redirectStep.getStateId().toString())));
            } else if (step instanceof FormStep formStep) {
                FormStepObject formStepDTO = mapToFormStepObject(formStep);
                loginResponseDTO.setStep(new LoginResponseDTO.StepDTO(formStepDTO, null));
            } else {
                throw new UnsupportedOperationException("Not implemented for loginStep of type " + step.getClass());
            }
        }

        log.info("Returning consent step on POST /v1/users/{}/user-sites for user site {} with activity: {}, with form: {}, with redirect: {}",
                secureUserId,
                loginResponseDTO.getUserSiteId(),
                loginResponseDTO.getActivityId(),
                loginResponseDTO.getStep() != null && loginResponseDTO.getStep().getForm() != null,
                loginResponseDTO.getStep() != null && loginResponseDTO.getStep().getRedirect() != null); //NOSHERIFF (should not fail?)

        return ResponseEntity.ok(loginResponseDTO);
    }

    @External
    @ExternalApi
    @Operation(summary = "Retrieve the next step in the flow to create/update a user-site.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            )
    })
    @GetMapping(value = "/v1/users/{userId}/user-sites/{userSiteId}/step", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginStepV1DTO>
    getNextStep(@Parameter(description = "The user-site id. This value is returned when you initiated the flow via POST /user-sites.", required = true)
                @PathVariable final UUID userSiteId,
                @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
                @PathVariable(name = USER_ID) final UUID userId) throws UserIdMismatchException {
        final UUID secureUserId = clientUserToken.getUserIdClaim();
        if (!Objects.equals(secureUserId, userId)) {
            throw new UserIdMismatchException(secureUserId, userId);
        }
        Step nextStep = createOrUpdateUserSiteService.getNextStepFromSession(userId, userSiteId);
        LoginStepV1DTO loginStepDTO = toResponse(nextStep, userSiteId);
        log.info("Returning consent step on GET /step for user site {} with form: {}, redirect: {}",
                loginStepDTO.getUserSiteId(),
                loginStepDTO.getForm() != null,
                loginStepDTO.getRedirect() != null); //NOSHERIFF
        return ResponseEntity.ok(loginStepDTO);
    }

    @External
    @ExternalApi
    @Operation(
            summary = "Renew the access for a user-site",
            description = "Starts a new consent flow for a client-user. This is needed when the credentials are expired or invalid.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful"
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "The wrong userId provided.",
                        content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
                ),
                @ApiResponse(
                        responseCode = "404",
                        description = "User site not found",
                        content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
                ),
                @ApiResponse(
                        responseCode = "500",
                        description = "Technical Error or the authentication means for given provider are missing.",
                        content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
                )
    })
    @PostMapping(value = "/v1/users/{userId}/user-sites/{userSiteId}/renew-access", produces = APPLICATION_JSON_VALUE)
    @AIS
    public ResponseEntity<LoginStepV1DTO> renewAccess(
            @PathVariable(name = USER_ID) final UUID userId,
            @PathVariable final UUID userSiteId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "The redirectUrl to use. Required for sites with a DIRECT_CONNECTION. " +
                    "Might be left empty in case of a SCRAPING site where there's no redirect. ")
            @RequestParam(name = REDIRECT_URL_ID, required = false) final UUID redirectUrlId,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action.", required = true)
            @IpAddress @RequestHeader(PSU_IP_ADDRESS_HEADER_NAME) String psuIpAddress
    ) throws UserIdMismatchException {
        final UUID secureUserId = clientUserToken.getUserIdClaim();
        if (!Objects.equals(secureUserId, userId)) {
            throw new UserIdMismatchException(secureUserId, userId);
        }

        Step loginStep = createOrUpdateUserSiteService.createLoginStepToRenewAccess(
                clientUserToken, userSiteId, redirectUrlId, psuIpAddress
        );
        LoginStepV1DTO loginStepDTO = toResponse(loginStep, userSiteId);

        log.info("Returning consent step on GET /renew-access-means for user site {} with form: {}, redirect: {}",
                loginStepDTO.getUserSiteId(),
                loginStepDTO.getForm() != null,
                loginStepDTO.getRedirect() != null); //NOSHERIFF

        return ResponseEntity.ok(loginStepDTO);
    }

    @External
    @ExternalApi
    @Operation(summary = "Retrieve a list of user-sites", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            )
    })
    @GetMapping(value = "/v1/users/{userId}/user-sites", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<UserSiteDTO>> getUserSites(
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @PathVariable(name = USER_ID) final UUID userId) throws UserIdMismatchException {
        final UUID secureUserId = clientUserToken.getUserIdClaim();
        if (!Objects.equals(secureUserId, userId)) {
            throw new UserIdMismatchException(secureUserId, userId);
        }

        List<PostgresUserSite> nonDeletedUserSites = userSiteService.getNonDeletedUserSites(secureUserId);

        return ResponseEntity.ok(userSiteService.toUserSiteDTOs(nonDeletedUserSites));
    }

    @External
    @ExternalApi
    @Operation(summary = "Retrieve a user-site", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            )
    })
    @GetMapping(value = "/v1/users/{userId}/user-sites/{userSiteId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<UserSiteDTO>
    getUserSite(@Parameter(description = "The unique user-site identifier", required = true) @PathVariable final UUID userSiteId,
                @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
                @PathVariable(name = USER_ID) final UUID userId) throws UserIdMismatchException {
        final UUID secureUserId = clientUserToken.getUserIdClaim();
        if (!Objects.equals(secureUserId, userId)) {
            throw new UserIdMismatchException(secureUserId, userId);
        }

        PostgresUserSite existingUserSite = userSiteService.getUserSite(secureUserId, userSiteId);
        UserSiteDTO userSiteDTOV2 = userSiteService.toUserSiteDTO(existingUserSite);
        return ResponseEntity.ok(userSiteDTOV2);
    }

    @External
    @ExternalApi
    @Operation(summary = "Trigger a refresh for all user-sites",
            description = "This endpoint cannot be called for one-off AIS users.",
            responses = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Successfully triggered the refresh"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "The wrong userId provided.",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            )
    })
    @PutMapping(value = "/v1/users/{userId}/user-sites/refresh", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    // Required, because Swagger will otherwise always add a default 200 OK response.
    @AIS
    public ResponseEntity<RefreshResponseDTO> refreshAllUserSites(
            @PathVariable(name = USER_ID) final UUID userId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action.", required = true)
            @IpAddress @RequestHeader(PSU_IP_ADDRESS_HEADER_NAME) String psuIpAddress
    ) throws UserIdMismatchException {
        final UUID secureUserId = clientUserToken.getUserIdClaim();
        if (!Objects.equals(secureUserId, userId)) {
            throw new UserIdMismatchException(secureUserId, userId);
        }
        return userSiteController.refreshAllUserSites(secureUserId, clientUserToken, psuIpAddress);
    }

    @External
    @ExternalApi
    @Operation(summary = "Trigger a refresh for a user-site",
            description = "This endpoint cannot be called for one-off AIS users.",
            responses = {
            @ApiResponse(
                    responseCode = "202",
                    description = "Successfully triggered the refresh"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "The wrong userId provided.",
                    content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
            )
    })
    @PutMapping(value = "/v1/users/{userId}/user-sites/{userSiteId}/refresh", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    // Required, because swagger will otherwise always add a default 200 OK response.
    @AIS
    public ResponseEntity<RefreshResponseDTO> refreshUserSite(
            @PathVariable final UUID userSiteId,
            @PathVariable(name = USER_ID) final UUID userId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action.", required = true)
            @IpAddress @RequestHeader(PSU_IP_ADDRESS_HEADER_NAME) String psuIpAddress
    ) throws UserIdMismatchException {
        final UUID secureUserId = clientUserToken.getUserIdClaim();
        if (!Objects.equals(secureUserId, userId)) {
            throw new UserIdMismatchException(secureUserId, userId);
        }
        return userSiteController.refreshUserSite(userSiteId, secureUserId, clientUserToken, psuIpAddress);
    }

    @External
    @ExternalApi
    @Operation(summary = "Remove user-site",
            description = "Removes the user-site including its accounts and transactions")
    @DeleteMapping(value = "/v1/users/{userId}/user-sites/{userSiteId}", produces = APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @AIS
    public ResponseEntity<Void> deleteUserSite(
            @Parameter(description = "The id of the user.", required = true)
            @PathVariable(name = USER_ID) final UUID userId,
            @Parameter(description = "The id of the user-site", required = true)
            @PathVariable final UUID userSiteId,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action.")
            @IpAddress @RequestHeader(PSU_IP_ADDRESS_HEADER_NAME) String psuIpAddress,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken
    ) throws UserIdMismatchException {
        final UUID secureUserId = clientUserToken.getUserIdClaim();
        if (!Objects.equals(secureUserId, userId)) {
            throw new UserIdMismatchException(secureUserId, userId);
        }
        userSiteController.deleteUserSite(userSiteId, psuIpAddress, clientUserToken);
        return ResponseEntity.accepted().build();
    }

    @External
    @ExternalApi
    @Operation(
            summary = "Connect a user-site",
            description = "Get a form or url that the user needs to fill in or be redirected to respectively, " +
                    "in order to create a connection (user-site) to a site.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successful"
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "The wrong userId provided.",
                        content = { @Content(schema = @Schema(implementation = ErrorDTO.class)) }
                )
    })
    @PostMapping(value = "/v1/users/{userId}/connect", produces = APPLICATION_JSON_VALUE)
    @AIS
    public ResponseEntity<LoginStepV1DTO> connectUserSite(
            @PathVariable @NotNull final UUID userId,
            @RequestParam("site") final UUID siteId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "The redirectUrl to use. Required for sites with a DIRECT_CONNECTION. " +
                    "Might be left empty in case of a SCRAPING site where there's no redirect. ")
            @RequestParam(name = REDIRECT_URL_ID, required = false) final UUID redirectUrlId,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action.", required = true)
            @IpAddress @RequestHeader(PSU_IP_ADDRESS_HEADER_NAME) String psuIpAddress
    ) throws UserIdMismatchException {
        final UUID secureUserId = clientUserToken.getUserIdClaim();
        if (!Objects.equals(secureUserId, userId)) {
            throw new UserIdMismatchException(secureUserId, userId);
        }

        Pair<Step, UUID> p = siteLoginService.createLoginStepForNewUserSite(clientUserToken, siteId, redirectUrlId, psuIpAddress);

        LoginStepV1DTO loginStepDTO = toResponse(p.getLeft(), p.getRight());
        log.info("Returning consent step on POST /connect for reserved user site {}, with form: {}, redirect: {}",
                loginStepDTO.getUserSiteId(),
                loginStepDTO.getForm() != null,
                loginStepDTO.getRedirect() != null); //NOSHERIFF
        return ResponseEntity.ok(loginStepDTO);
    }

    private static LoginStepV1DTO toResponse(Step loginStep, UUID userSiteId) {
        if (loginStep instanceof RedirectStep redirectStep) {
            String redirectUrl = redirectStep.getRedirectUrl();
            var state = redirectStep.getStateId();
            return new LoginStepV1DTO(redirectUrl, state.toString(), userSiteId);
        } else if (loginStep instanceof FormStep formStep) {
            FormStepObject formStepObject = mapToFormStepObject(formStep);
            return new LoginStepV1DTO(formStepObject, userSiteId);
        } else {
            throw new UnsupportedOperationException("Not implemented for loginStep of type " + loginStep.getClass());
        }
    }

    private static FormStepObject mapToFormStepObject(FormStep formStep) {
        final FormStepEncryptionDetailsDTO formStepEncryptionDetailsDTO = FormStepEncryptionDetailsDTO.from(formStep.getEncryptionDetails());
        final List<FormComponent> formComponents = formStep.getForm().getFormComponents();
        final List<FormComponentDTO> formComponentDTOs =
                FormDTOMapper.convertToFormComponentDTOs(formComponents);

        final ExplanationField explanation = formStep.getForm().getExplanationField();
        final ExplanationFieldDTO explanationFieldDTO = explanation != null ?
                (ExplanationFieldDTO) FormDTOMapper.convertToFormComponentDTO(explanation) : null;
        return new FormStepObject(formComponentDTOs, explanationFieldDTO, formStepEncryptionDetailsDTO, formStep.getStateId().toString());
    }
}
