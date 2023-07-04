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
@Schema(name = "TEXT_AREA")
public class TextAreaFieldDTO extends AbstractFieldDTO {

    private Integer maxLength;
    /**
     * Purely rendering advice. The amount of 'multilines' or 'rows' the field should/could occupy.
     */
    private Integer rows;

    public TextAreaFieldDTO(final String id, final String displayName, final Integer maxLength,
                            final Boolean optional, final Integer rows) {
        super(id, displayName, optional);
        this.rows = rows;
        this.maxLength = maxLength;
    }

    @Override
    @Schema(type = "string", allowableValues = "TEXT_AREA")
    public FieldType getFieldType() {
        return FieldType.TEXT_AREA;
    }

    @Override
    @Schema(allowableValues = "TEXT_AREA")
    public String getType() {
        return "TEXT_AREA";
    }
}
