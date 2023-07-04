package nl.ing.lovebird.sitemanagement.health.service.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(enumAsRef = true)
public enum LovebirdHealthCode {

    @JsonProperty("PROCESSING")
    PROCESSING("Data is being processed, new data expected soon. Keep polling."),
    @JsonProperty("UP_TO_DATE")
    UP_TO_DATE("All is fine."),
    @JsonProperty("ERROR")
    ERROR("An error occurred."),
    @JsonProperty("UNKNOWN")
    UNKNOWN("We could not determine the state.");

    private final String description;


    LovebirdHealthCode(final String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("%s : %s", name(), description);
    }
}
