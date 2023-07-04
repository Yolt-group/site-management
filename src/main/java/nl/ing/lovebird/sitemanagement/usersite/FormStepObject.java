package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;
import nl.ing.lovebird.sitemanagement.forms.ExplanationFieldDTO;
import nl.ing.lovebird.sitemanagement.forms.FormComponentDTO;

import java.util.List;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "FormStepObject", description = "A form step in the login flow. This form needs to be rendered, filled in, and sent back.")
public class FormStepObject {
    @ArraySchema(arraySchema = @Schema(description = "Array with form components", required = true), schema = @Schema(ref = "FormComponentDTO"))
    List<FormComponentDTO> formComponents;

    ExplanationFieldDTO explanationField;

    @Schema(required = true)
    FormStepEncryptionDetailsDTO encryption;

    @Schema(description = "The stateId that must be submitted along the form when posting it back.", required = true)
    String stateId;
}
