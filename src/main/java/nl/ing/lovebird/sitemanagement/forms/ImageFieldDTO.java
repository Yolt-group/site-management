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
@Schema(name = "IMAGE")
public class ImageFieldDTO extends AbstractFieldDTO {

    private String base64Image;
    private String mimeType;

    public ImageFieldDTO(String id, String displayName, String base64Image, String mimeType) {
        super(id, displayName, true);
        this.base64Image = base64Image;
        this.mimeType = mimeType;
    }

    @Override
    @Schema(type = "string", allowableValues = "IMAGE")
    public FieldType getFieldType() {
        return FieldType.IMAGE;
    }

    @Override
    @Schema(type = "string", allowableValues = "FIELD")
    public ComponentType getComponentType() {
        return ComponentType.FIELD;
    }

    @Override
    @Schema(allowableValues = "IMAGE")
    public String getType() {
        return "IMAGE";
    }
}
