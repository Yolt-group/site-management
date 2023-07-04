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
@Schema(name = "MULTI")
public class MultiFormComponentDTO implements FormContainerDTO {

    final List<FormComponentDTO> childComponents = new ArrayList<>();

    private String id;

    private String displayName;

    private boolean optional;


    @Override
    @Schema(type = "string", allowableValues = "MULTI")
    public ContainerType getContainerType() {
        return ContainerType.MULTI;
    }

    @Override
    public void addChildComponent(final FormComponentDTO formContainerDTO) {
        childComponents.add(formContainerDTO);
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Override
    @Schema(type = "string", allowableValues = "CONTAINER")
    public ComponentType getComponentType() {
        return ComponentType.CONTAINER;
    }

    @Override
    @Schema(allowableValues = "MULTI")
    public String getType() {
        return "MULTI";
    }
}
