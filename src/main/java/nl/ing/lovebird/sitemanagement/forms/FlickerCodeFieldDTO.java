package nl.ing.lovebird.sitemanagement.forms;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "FLICKER_CODE")
public class FlickerCodeFieldDTO extends AbstractFieldDTO {

    private final String code;

    public FlickerCodeFieldDTO(String id, String displayName, String code) {
        super(id, displayName, true);
        this.code = code;
    }

    @Override
    @Schema(type = "string", allowableValues = "FLICKER_CODE")
    public FieldType getFieldType() {
        return FieldType.FLICKER_CODE;
    }

    @Override
    @Schema(type = "string", allowableValues = "FIELD")
    public ComponentType getComponentType() {
        return ComponentType.FIELD;
    }

    @Override
    @Schema(allowableValues = "FLICKER_CODE")
    public String getType() {
        return "FLICKER_CODE";
    }
}
