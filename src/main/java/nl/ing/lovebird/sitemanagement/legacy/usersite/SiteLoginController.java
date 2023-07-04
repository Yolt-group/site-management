package nl.ing.lovebird.sitemanagement.legacy.usersite;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.providershared.form.ExplanationField;
import nl.ing.lovebird.providershared.form.FormComponent;
import nl.ing.lovebird.sitemanagement.forms.ExplanationFieldDTO;
import nl.ing.lovebird.sitemanagement.forms.FormComponentDTO;
import nl.ing.lovebird.sitemanagement.forms.FormDTOMapper;
import nl.ing.lovebird.sitemanagement.forms.FormValidationException;
import nl.ing.lovebird.sitemanagement.legacy.HateoasUtils;
import nl.ing.lovebird.sitemanagement.lib.documentation.External;
import nl.ing.lovebird.sitemanagement.lib.validation.IpAddress;
import nl.ing.lovebird.sitemanagement.usersite.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.lib.PsuIpAddress.PSU_IP_ADDRESS_HEADER_NAME;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Deprecated
@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
@Tag(name = "sites", description = "Use it to retrieve login forms for sites.")
public class SiteLoginController {

    public static final String REDIRECT_URL_ID_HEADER_NAME = "redirect-url-id";

    private final SiteLoginService siteLoginService;

    /**
     * @deprecated use {@link UserSiteControllerV1#connectUserSite} instead
     */
    @Deprecated
    @External
    @Operation(summary = "Initiate user-site", description = "Get a form or url that the user needs to fill in or be redirected to respectively, " +
                    "in order to create a connection (user-site) to a site.", responses = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful"
            )
    })
    @GetMapping(value = "/sites/{siteId}/initiate-user-site", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginStepDTO> initiateUserSite(
            @PathVariable final UUID siteId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientUserToken clientUserToken,
            @Parameter(description = "The redirectUrl to use. Required for sites with a DIRECT_CONNECTION. " +
                    "Might be left empty in case of a SCRAPING site where there's no redirect. ")
            @RequestHeader(name = REDIRECT_URL_ID_HEADER_NAME, required = false) final UUID redirectUrlId,
            @Parameter(description = "The ipv4 or ipv6 address of the client-user performing an action. " +
                    "Fill this in when a user initiates this action. If not, it can be left empty.")
            @Nullable @IpAddress @RequestHeader(name = PSU_IP_ADDRESS_HEADER_NAME, required = false) String psuIpAddress
    ) {
        if (StringUtils.isBlank(psuIpAddress)) {
            log.info("Client {} does not provide PSU_IP_ADDRESS on {} for site {}", clientUserToken.getClientIdClaim(), "/sites/{siteId}/initiate-user-site", siteId);
        }

        Pair<Step, UUID> p = siteLoginService.createLoginStepForNewUserSite(clientUserToken, siteId, redirectUrlId, psuIpAddress);

        LoginStepDTO loginStepDTO = toResponse(p.getLeft(), p.getRight());
        log.info("Returning consent step on GET /initiate-user-site for reserved user site {}, with form: {}, redirect: {}",
                loginStepDTO.getUserSiteId(),
                loginStepDTO.getForm() != null,
                loginStepDTO.getRedirect() != null); //NOSHERIFF (should not fail?)
        return ResponseEntity.ok(loginStepDTO);
    }

    static LoginStepDTO toResponse(Step loginStep, UUID userSiteId) {
        if (loginStep instanceof RedirectStep redirectStep) {
            String redirectUrl = redirectStep.getRedirectUrl();
            return new LoginStepDTO(makeHateoasPostLoginStepPath(), redirectUrl, userSiteId, redirectStep.getStateId().toString());
        } else if (loginStep instanceof FormStep formStep) {
            FormStepObject formStepObject = mapToFormStepObject(formStep);
            return new LoginStepDTO(makeHateoasPostLoginStepPath(), formStepObject, userSiteId);
        } else {
            throw new UnsupportedOperationException("Not implemented for loginStep of type " + loginStep.getClass());
        }
    }

    static FormStepObject mapToFormStepObject(FormStep formStep) {
        final FormStepEncryptionDetailsDTO formStepEncryptionDetailsDTO = FormStepEncryptionDetailsDTO.from(formStep.getEncryptionDetails());
        final List<FormComponent> formComponents = formStep.getForm().getFormComponents();
        final List<FormComponentDTO> formComponentDTOs =
                FormDTOMapper.convertToFormComponentDTOs(formComponents);

        final ExplanationField explanation = formStep.getForm().getExplanationField();
        final ExplanationFieldDTO explanationFieldDTO = explanation != null ?
                (ExplanationFieldDTO) FormDTOMapper.convertToFormComponentDTO(explanation) : null;
        return new FormStepObject(formComponentDTOs, explanationFieldDTO, formStepEncryptionDetailsDTO, formStep.getStateId().toString());
    }

    static String makeHateoasPostLoginStepPath() {
        try {
            return HateoasUtils.createPath(methodOn(UserSiteController.class).postLogin(
                     null, null, null, null
            ));
        } catch (FormValidationException e) {
            throw new RuntimeException(e); // should never happen as the method shouldn't be even called here.
        }
    }

}
