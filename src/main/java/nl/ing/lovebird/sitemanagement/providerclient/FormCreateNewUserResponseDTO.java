package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FormCreateNewUserResponseDTO {

    private final AccessMeansDTO accessMeans;
    private final String externalUserId;

}
