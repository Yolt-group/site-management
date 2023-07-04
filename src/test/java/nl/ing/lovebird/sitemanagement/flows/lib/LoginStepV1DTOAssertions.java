package nl.ing.lovebird.sitemanagement.flows.lib;

import nl.ing.lovebird.sitemanagement.usersite.FormStepEncryptionDetailsDTO;
import nl.ing.lovebird.sitemanagement.usersite.FormStepEncryptionDetailsDTO.Type;
import nl.ing.lovebird.sitemanagement.usersite.LoginStepV1DTO;

import static nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI.parse;
import static org.assertj.core.api.Assertions.assertThat;

public class LoginStepV1DTOAssertions {

    /**
     * Validate a {@link LoginStepV1DTO} if a redirect step is expected.
     */
    public static void assertIsRedirectStep(LoginStepV1DTO dto) {
        assertInvariants(dto);
        assertThat(dto.getRedirect()).isNotNull();

        // We should have been given an URL
        var redirectToBankUrl = parse(dto.getRedirect().getUrl());
        assertThat(redirectToBankUrl.isValid()).isTrue();

        // There should not be a Form.
        assertThat(dto.getForm()).isNull();
    }

    /**
     * Validate a {@link LoginStepV1DTO} and check that a FormStep is present.
     */
    public static void assertIsFormStep(LoginStepV1DTO dto) {
        assertInvariants(dto);

        // There should be a form
        assertThat(dto.getForm()).isNotNull();
        // Check that the encryptio object is present, but make sure the type is set to NONE.  Types other than NONE
        // are only used for SCRAPING connections.
        assertThat(dto.getForm().getEncryption()).isNotNull();
        assertThat(dto.getForm().getEncryption().getType()).isEqualTo(Type.NONE);
        // There should be at least a single form component, otherwise the form is nonsense.
        assertThat(dto.getForm().getFormComponents()).hasSizeGreaterThan(0);
        // Make sure that the stateId is set.
        assertThat(dto.getForm().getStateId()).isNotNull();

        // There should not be a redirectUrl
        assertThat(dto.getRedirect()).isNull();
    }

    private static void assertInvariants(LoginStepV1DTO dto) {
        assertThat(dto).isNotNull();

        // We expect the userSiteId and redirect object to be present
        assertThat(dto.getUserSiteId()).isNotNull();
    }

}
