package nl.ing.lovebird.sitemanagement.exception;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import nl.ing.lovebird.errorhandling.ErrorDTO;

import java.util.ArrayList;
import java.util.List;

@Schema(
        name = "Error",
        description = "Describes an error in the system"
)
@Getter
public class InputErrorDTO extends ErrorDTO {
    @ArraySchema(arraySchema = @Schema(description = "A list of field errors."))
    private List<String> fieldErrors = new ArrayList<>();
    @ArraySchema(arraySchema = @Schema(description = "A list of global errors."))
    private List<String> globalErrors = new ArrayList<>();

    public InputErrorDTO(String code, String message) {
        super(code, message);
    }

    public void addFieldError(String error) {
        fieldErrors.add(error);
    }
    public void addGlobalError(String error) {
        globalErrors.add(error);
    }

}
