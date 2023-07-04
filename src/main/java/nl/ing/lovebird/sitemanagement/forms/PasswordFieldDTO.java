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
@Schema(name = "PASSWORD")
public class PasswordFieldDTO extends AbstractFieldDTO {

    private Integer length;

    private Integer maxLength;

    public PasswordFieldDTO(final String id, final String displayName, final Integer length, final Integer maxLength,
                            final Boolean optional) {
        super(id, displayName, optional);
        this.length = length;
        this.maxLength = maxLength;
    }

    @Override
    @Schema(type = "string", allowableValues = "PASSWORD")
    public FieldType getFieldType() {
        return FieldType.PASSWORD;
    }

    @Override
    @Schema(allowableValues = "PASSWORD")
    public String getType() {
        return "PASSWORD";
    }
}
