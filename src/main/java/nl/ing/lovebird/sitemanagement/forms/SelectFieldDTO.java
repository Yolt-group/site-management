package nl.ing.lovebird.sitemanagement.forms;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.ing.lovebird.providershared.form.SelectOptionValue;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "SELECT")
public class SelectFieldDTO extends AbstractFieldDTO {

    private List<SelectOptionValueDTO> selectOptionValues = new ArrayList<>();

    /**
     * The visual length of the field
     */
    private Integer length;

    /**
     * Maximum of the characters put into a field.
     * This is probably not really useful on a selectField, but is kept for backwards compatibility (it was property of the parent 'Field'
     * before)
     */
    private Integer maxLength;

    /**
     * The preselected option. Is optional. Could be provided to be used as a default.
     */
    private SelectOptionValue defaultValue;

    public SelectFieldDTO(final String id, final String displayName, final Integer length, final Integer maxLength,
                          final Boolean optional, final SelectOptionValue defaultValue) {
        super(id, displayName, optional);
        this.length = length;
        this.maxLength = maxLength;
        this.defaultValue = defaultValue;
    }

    public void addSelectOptionValue(final SelectOptionValueDTO selectOptionValue) {
        selectOptionValues.add(selectOptionValue);
    }

    @Override
    @Schema(type = "string", allowableValues = "SELECT")
    public FieldType getFieldType() {
        return FieldType.SELECT;
    }

    @Override
    @Schema(allowableValues = "SELECT")
    public String getType() {
        return "SELECT";
    }
}

