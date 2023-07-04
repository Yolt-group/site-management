package nl.ing.lovebird.sitemanagement.usersite.encryption;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import nl.ing.lovebird.sitemanagement.usersite.FormStepEncryptionDetailsDTO;

@Schema(name = "NoFormStepEncryptionDetails")
@EqualsAndHashCode
public class NoFormStepEncryptionDetailsDTO implements FormStepEncryptionDetailsDTO {
    public static final String TYPE = "NONE";

    @Override
    @Schema(type = "string", allowableValues = TYPE)
    public Type getType() {
        return Type.NONE;
    }
}
