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
@Schema(name = "TEXT")
public class TextFieldDTO extends AbstractFieldDTO {

    private Integer length;

    private Integer maxLength;

    public TextFieldDTO(final String id, final String displayName, final Integer length, final Integer maxLength,
                        final Boolean optional) {
        super(id, displayName, optional);
        this.length = length;
        this.maxLength = maxLength;
    }

    @Override
    @Schema(type = "string", allowableValues = "TEXT")
    public FieldType getFieldType() {
        return FieldType.TEXT;
    }

    @Override
    @Schema(allowableValues = "TEXT")
    public String getType() {
        return "TEXT";
    }
}
