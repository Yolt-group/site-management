package nl.ing.lovebird.sitemanagement.usersite;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "ExternalUserSiteId", description = "External ID of a UserSite used by provider")
public class ExternalUserSiteIdDTO {

    @NotBlank
    private String externalUserSiteId;
}
