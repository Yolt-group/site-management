package nl.ing.lovebird.sitemanagement.usersite;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(name = "FilledInFormValue", description = "Contains the unique field identifier and its filled-in value.")
public class FilledInFormValueDTO {

    @NotBlank
    @Size(max = 1024)
    private String fieldId;

    @NotBlank
    @Size(max = 10240)
    private String value;

}
