package nl.ing.lovebird.sitemanagement.usersite;


import lombok.*;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.annotations.VerifiedClientToken;
import nl.ing.lovebird.sitemanagement.lib.documentation.Internal;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static nl.ing.lovebird.sitemanagement.lib.documentation.Internal.Service.managementPortals;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserSiteStatisticsController {

    private final UserSiteService userSiteService;

    @ResponseBody
    @Internal(managementPortals)
    @GetMapping(value = "/internal/clients/self/user-sites/-/statistics")
    public UserSiteStatistics getUserSiteStatistics(final @VerifiedClientToken ClientToken clientToken) {
        List<UserSiteService.UserSiteStatistics> userSiteStatistics = userSiteService.getUserSiteStatistics(new ClientId(clientToken.getClientIdClaim()));
        return UserSiteStatistics.builder()
                .statistics(userSiteStatistics)
                .build();
    }

    @Builder
    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class UserSiteStatistics {
        @NonNull
        public final List<UserSiteService.UserSiteStatistics> statistics;
    }
}
