package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class OrphanUserResponseDTO {

    @NonNull Integer actualListSize;
    @NonNull List<OrphanUserDTO> orphanUserList;
}
