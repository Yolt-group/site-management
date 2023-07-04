package nl.ing.lovebird.sitemanagement.forms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class AbstractFieldDTO implements FormFieldDTO {

    private String id;

    private String displayName;

    private boolean optional;

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    @Schema(type = "string", allowableValues = "FIELD")
    public FormComponentDTO.ComponentType getComponentType() {
        return FormComponentDTO.ComponentType.FIELD;
    }

}
