package nl.ing.lovebird.sitemanagement.usersite;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.ing.lovebird.providershared.form.FilledInUserSiteFormValues;
import nl.ing.lovebird.sitemanagement.site.LoginType;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode
@Data
@Schema(name = "FORM", description = "A container object with the filled-in form values needed for login.")
public class FormLoginDTO implements LoginDTO {

    @NotEmpty
    @Schema(description = "The state-id that is used to determine which process to relate this form login to. Think of it like a session.")
    private String stateId;

    @ArraySchema(arraySchema = @Schema(description = "The list of filled-in form values."))
    @Size(max = 256)
    private List<@Valid FilledInFormValueDTO> filledInFormValues = new ArrayList<>();

    @Override
    public Login toLogin(final UUID userId) {
        final FilledInUserSiteFormValues filledInUserSiteFormValues = new FilledInUserSiteFormValues();
        for (FilledInFormValueDTO filledInFormValueDTO : getFilledInFormValues()) {
            filledInUserSiteFormValues.add(filledInFormValueDTO.getFieldId(), filledInFormValueDTO.getValue());
        }
        String stateId = getStateId();
        return new FormLogin(userId, filledInUserSiteFormValues, stateId != null ? UUID.fromString(stateId) : null);
    }

    @Override
    @Schema(type = "string", allowableValues = "FORM")
    public LoginType getLoginType() {
        return LoginType.FORM;
    }

}
