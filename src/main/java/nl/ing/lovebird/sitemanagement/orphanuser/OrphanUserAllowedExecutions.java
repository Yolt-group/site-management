package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Configuration
@ConfigurationProperties(prefix = "yolt.orphan-user")
@Data
public class OrphanUserAllowedExecutions {

    private List<OrphanUserAllowedExecution> allowedExecutions;

    public boolean isAllowed(@NotNull final String provider, @NotNull final UUID batchId) {
        if (allowedExecutions == null || allowedExecutions.isEmpty()) {
            return false;
        }

        final OrphanUserAllowedExecution orphanUsersAllowedExecution = new OrphanUserAllowedExecution();
        orphanUsersAllowedExecution.setProvider(provider);
        orphanUsersAllowedExecution.setBatchId(batchId);
        return allowedExecutions.contains(orphanUsersAllowedExecution);
    }

    @Data
    static class OrphanUserAllowedExecution {
        private String provider;
        private UUID batchId;
    }
}
