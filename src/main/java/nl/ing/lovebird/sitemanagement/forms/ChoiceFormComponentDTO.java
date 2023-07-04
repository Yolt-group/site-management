package nl.ing.lovebird.sitemanagement.forms;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "CHOICE")
public class ChoiceFormComponentDTO implements FormContainerDTO {

    private String id;

    final List<FormComponentDTO> childComponents = new ArrayList<>();

    private String displayName;

    private boolean optional;

    @Override
    @Schema(type = "string", allowableValues = "CHOICE")
    public ContainerType getContainerType() {
        return ContainerType.CHOICE;
    }

    @Override
    public void addChildComponent(final FormComponentDTO formContainerDTO) {
        childComponents.add(formContainerDTO);
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    @Override
    @Schema(type = "string", allowableValues = "CONTAINER")
    public ComponentType getComponentType() {
        return ComponentType.CONTAINER;
    }

    @Override
    @Schema(allowableValues = "CHOICE")
    public String getType() {
        return "CHOICE";
    }

}

