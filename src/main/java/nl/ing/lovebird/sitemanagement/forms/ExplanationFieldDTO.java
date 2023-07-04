package nl.ing.lovebird.sitemanagement.forms;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "EXPLANATION", description = "Form field with a descriptive message")
public class ExplanationFieldDTO implements FormFieldDTO {

    private final String id;
    private final String displayName;
    private final String explanation;

    public ExplanationFieldDTO(String id, String displayName, String explanation) {
        this.id = id;
        this.displayName = displayName;
        this.explanation = explanation;
    }

    @Override
    @Schema(type = "string", allowableValues = "EXPLANATION")
    public FieldType getFieldType() {
        return FieldType.EXPLANATION;
    }

    @Override
    public boolean isOptional() {
        return true;
    }

    @Override
    @Schema(type = "string", allowableValues = "FIELD")
    public ComponentType getComponentType() {
        return ComponentType.FIELD;
    }

    @Override
    @Schema(allowableValues = "EXPLANATION")
    public String getType() {
        return "EXPLANATION";
    }
}
