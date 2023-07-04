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
@Schema(name = "IBAN")
public class IbanFieldDTO extends AbstractFieldDTO {

    /**
     * Optional pre-filled / default value.
     */
    private String defaultValue;

    public IbanFieldDTO(final String id, final String displayName,
                        final Boolean optional, final String defaultValue) {
        super(id, displayName, optional);
        this.defaultValue = defaultValue;
    }

    @Override
    @Schema(type = "string", allowableValues = "IBAN")
    public FieldType getFieldType() {
        return FieldType.IBAN;
    }

    @Override
    @Schema(allowableValues = "IBAN")
    public String getType() {
        return "IBAN";
    }
}
