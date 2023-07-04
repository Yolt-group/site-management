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
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "RADIO")
public class RadioFieldDTO extends AbstractFieldDTO {

    private List<SelectOptionValueDTO> selectOptionValues = new ArrayList<>();

    /**
     * The preselected option. Is optional. Could be provided to be used as a default.
     */
    private SelectOptionValue defaultValue;

    public RadioFieldDTO(final String id, final String displayName, final Boolean optional, final SelectOptionValue defaultValue) {
        super(id, displayName, optional);
        this.defaultValue = defaultValue;
    }

    public void addSelectOptionValue(final SelectOptionValueDTO selectOptionValue) {
        selectOptionValues.add(selectOptionValue);
    }

    @Override
    @Schema(type = "string", allowableValues = "RADIO")
    public FieldType getFieldType() {
        return FieldType.RADIO;
    }

    @Override
    @Schema(allowableValues = "RADIO")
    public String getType() {
        return "RADIO";
    }
}

