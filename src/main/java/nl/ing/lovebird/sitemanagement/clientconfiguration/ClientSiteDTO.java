package nl.ing.lovebird.sitemanagement.clientconfiguration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ClientSiteDTO {

    private UUID id;
    private String name;
    private Services services;
    private boolean useExperimentalVersion;

    @Value
    public static class Services {
        // Conditional: either ais or pis is not-null
        @Nullable
        AIS ais;

        @Value
        @Schema(description = "AIS, Account Information Service.")
        public static class AIS {
            @NotNull
            Onboarded onboarded;
        }

        @Value
        @Schema(description = "Describes whether the client is onboarded for this particular service on this site.")
        public static class Onboarded {
            List<UUID> redirectUrlIds;
        }
    }
}
