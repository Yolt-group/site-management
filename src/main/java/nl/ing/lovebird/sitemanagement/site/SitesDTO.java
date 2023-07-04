package nl.ing.lovebird.sitemanagement.site;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Sites", description = "The container for a list of sites")
public class SitesDTO {
    @ArraySchema(arraySchema = @Schema(required = true, allowableValues = "[{}]"))
    private List<SiteDTO> sites;
}
