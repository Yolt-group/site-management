package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import nl.ing.lovebird.sitemanagement.site.LoginType;
import org.hibernate.validator.constraints.URL;

import java.util.UUID;

@EqualsAndHashCode
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "URL", description = "A container object with the redirect-url needed for login.")
public class UrlLoginDTO implements LoginDTO {

    @URL
    private String redirectUrl;

    @Override
    public Login toLogin(final UUID userId) {
        return new UrlLogin(userId, getRedirectUrl());
    }

    @Override
    @Schema(type = "string", allowableValues = "URL")
    public LoginType getLoginType() {
        return LoginType.URL;
    }

}
