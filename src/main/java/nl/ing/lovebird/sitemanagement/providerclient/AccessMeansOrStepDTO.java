package nl.ing.lovebird.sitemanagement.providerclient;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import nl.ing.lovebird.sitemanagement.usersite.Step;

@Value
public class AccessMeansOrStepDTO {

    AccessMeansDTO accessMeans;
    Step step;

    public AccessMeansOrStepDTO(Step step) {
        this.step = step;
        this.accessMeans = null;
    }


    public AccessMeansOrStepDTO(AccessMeansDTO accessMeans) {
        this.accessMeans = accessMeans;
        this.step = null;
    }

    /**
     * Just for jackson.. You can use the overloaded variants so you don't need to instantiate something with 'null'.
     */
    @JsonCreator
    public AccessMeansOrStepDTO(@JsonProperty("accessMeans") AccessMeansDTO accessMeans, @JsonProperty("step") Step step) {
        this.accessMeans = accessMeans;
        this.step = step;
        if (step == null && accessMeans == null) {
            throw new IllegalArgumentException("Either the step or accessMeans should be not-null");
        }
    }


}
