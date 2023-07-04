package nl.ing.lovebird.sitemanagement.legacy.sites.sitewithcountry;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.sitemanagement.lib.documentation.External;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Deprecated
@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
public class SiteWithCountryController {

    /**
     * @deprecated use /clients/v2/sites/{siteid} instead
     */
    @Deprecated
    @External
    @GetMapping(value = "/sites/{siteId}", produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> getSite(
            @Parameter(description = "The unique site identifier", required = true) @PathVariable final UUID siteId,
            @Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken
    ) {
        // Only kept around so that we can still generate valid HATEOAS links.
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    @Deprecated
    @External
    @GetMapping(value = {"/sites", "/sites/by-group"}, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> goneEndpoints(
            @Parameter(hidden = true) @VerifiedClientToken final ClientToken clientToken) {

        // Only kept around so that the root HATEOAS links in `client-proxy` remain working. Can be removed once story
        // YCL-3333 has been picked up by the Clients team.
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
