package nl.ing.lovebird.sitemanagement.usersite;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "UserIdDTO", description = "The id of the user.")
public class UserIdDTO {

    @NotNull
    private UUID userId;
}
