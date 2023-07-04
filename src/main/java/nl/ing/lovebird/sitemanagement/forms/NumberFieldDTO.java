package nl.ing.lovebird.sitemanagement.forms;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "NUMBER")
public class NumberFieldDTO extends AbstractFieldDTO {

    /**
     * The minimum boundary of the number.
     */
    private BigDecimal min;

    /**
     * The maximum boundary of the number.
     */
    private BigDecimal max;

    /**
     * The 'stepsize' (equal to html input type="number" step="0.1")
     * This tells you something about the allowed precision. For example step = 0.01 means that the precision is 2 decimals.
     */
    private BigDecimal stepSize;

    /**
     * Optional pre-filled / default value.
     */
    private BigDecimal defaultValue;

    public NumberFieldDTO(final String id, final String displayName,
                          final Boolean optional, final BigDecimal min, final BigDecimal max,
                          final BigDecimal stepSize, final BigDecimal defaultValue) {
        super(id, displayName, optional);
        this.min = min;
        this.max = max;
        this.stepSize = stepSize;
        this.defaultValue = defaultValue;
    }

    @Override
    @Schema(type = "string", allowableValues = "NUMBER")
    public FieldType getFieldType() {
        return FieldType.NUMBER;
    }

    @Override
    @Schema(allowableValues = "NUMBER")
    public String getType() {
        return "NUMBER";
    }
}
