package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FormStep.class, name = "FORM"),
        @JsonSubTypes.Type(value = RedirectStep.class, name = "REDIRECT_URL")
})
@RequiredArgsConstructor
@Getter
@Setter
@Schema(name = "Step", subTypes = {FormStep.class, RedirectStep.class})
public abstract class Step {
    /**
     * Returned from providers. Ony stored internally and provided back to providers when submitting the next step.
     */
    private final String providerState;

    /**
     * A stateId. A 'form' is presented when the user adds or updates a user-site. The stateId should come from the 'usersite session'.
     * (i.e. the session where he/she adds the user-site)
     * This stateId should be send back on POST /user-sites, so we know what we were doing by getting the correct user site session.
     */
    private UUID stateId;
}



