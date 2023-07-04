package nl.ing.lovebird.sitemanagement.usersite;

import lombok.Data;
import nl.ing.lovebird.sitemanagement.site.LoginType;

import java.util.UUID;

@Data
public abstract class Login {

    private final UUID userId;

}
