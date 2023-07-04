package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.Date;
import java.util.UUID;

@Value
@AllArgsConstructor
public class AccessMeansDTO {

    @NonNull
    UUID userId;
    @NonNull
    String accessMeansBlob;
    @NonNull
    Date updated;
    @NonNull
    Date expireTime;

}
