package nl.ing.lovebird.sitemanagement.health.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.sitemanagement.health.controller.dto.AccountGroupDTO;
import nl.ing.lovebird.sitemanagement.health.controller.dto.UserHealthDTO;
import nl.ing.lovebird.sitemanagement.health.controller.dto.UserSiteHealthDTO;
import nl.ing.lovebird.sitemanagement.health.service.HealthService;
import nl.ing.lovebird.sitemanagement.health.service.domain.UserHealth;
import nl.ing.lovebird.sitemanagement.lib.documentation.External;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequiredArgsConstructor
@Tag(name = "health")
public class HealthController {

    private final HealthService healthService;

    @Deprecated
    @External
    @Operation(summary = "Get user health",
            description = "This endpoint can be used to poll on to see the status of an add-bank or refresh flow.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "An aggregation object of the total status of a user")
            }
    )
    @GetMapping(value = "/user-health/me", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<UserHealthDTO> getUserHealthMe(
            @VerifiedClientToken ClientUserToken clientUserToken
    ) {
        UserHealth userHealth = healthService.getUserHealth(clientUserToken);

        List<AccountGroupDTO> accountGroupDTOs = userHealth.getAccountGroups().stream()
                .map(AccountGroupDTO::fromAccountGroup)
                .sorted(comparing(AccountGroupDTO::getType))
                .collect(Collectors.toList());

        List<UserSiteHealthDTO> userSiteHealthDTOs = userHealth.getUserSites().stream()
                .map(UserSiteHealthDTO::fromUserSite)
                .sorted(comparing(UserSiteHealthDTO::getId))
                .collect(Collectors.toList());

        UserHealthDTO userHealthDTO = new UserHealthDTO(
                userHealth.getLovebirdHealthCode(),
                accountGroupDTOs,
                userSiteHealthDTOs,
                userHealth.getMigrationStatus());

        return ResponseEntity.ok(userHealthDTO);
    }
}
