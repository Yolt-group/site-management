package nl.ing.lovebird.sitemanagement.usersite;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.ing.lovebird.sitemanagement.site.LoginType;

import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
public class UrlLogin extends Login {
    private final String redirectUrl;
    private final LoginType loginType = LoginType.URL;

    public UrlLogin(final UUID userId, final String redirectUrl) {
        super(userId);
        this.redirectUrl = redirectUrl;
    }
}
