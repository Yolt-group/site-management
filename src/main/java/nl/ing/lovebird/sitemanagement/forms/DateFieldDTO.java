package nl.ing.lovebird.sitemanagement.forms;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.Nullable;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "DATE")
public class DateFieldDTO extends AbstractFieldDTO {

    /**
     * Optional boundary of a minimum date.
     */
    private LocalDate minDate;
    /**
     * Optional boundary of a maximum date.
     */
    private LocalDate maxDate;

    /**
     * Optional pre-filled defaultvalue
     */
    private LocalDate defaultValue;

    private String dateFormat;

    public DateFieldDTO(final String id, final String displayName, final Boolean optional, @Nullable final LocalDate minDate,
                        @Nullable final LocalDate maxDate, @Nullable final LocalDate defaultValue,
                        @Nullable final String dateFormat) {
        super(id, displayName, optional);
        this.minDate = minDate;
        this.maxDate = maxDate;
        this.defaultValue = defaultValue;
        this.dateFormat = dateFormat;
    }

    @Override
    @Schema(type = "string", allowableValues = "DATE")
    public FieldType getFieldType() {
        return FieldType.DATE;
    }

    @Override
    @Schema(allowableValues = "DATE")
    public String getType() {
        return "DATE";
    }
}
