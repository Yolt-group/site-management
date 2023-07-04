package nl.ing.lovebird.sitemanagement.health.repository.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import nl.ing.lovebird.sitemanagement.health.service.domain.LovebirdHealthCode;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Account implements LovebirdHealth {

    @NonNull
    private final UUID id;

    @NonNull
    @Deprecated //This field is deprecated and will be removed soon 13-12-2016
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ", timezone = "UTC")
    private final Date lastRefreshed;

    @NonNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ", timezone = "UTC")
    private final Date updated;

    @NonNull
    private final UUID userSiteId;

    private final LovebirdHealthCode lovebirdHealthCode = LovebirdHealthCode.UP_TO_DATE;
}
