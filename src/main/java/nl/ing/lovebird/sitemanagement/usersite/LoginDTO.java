package nl.ing.lovebird.sitemanagement.usersite;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import nl.ing.lovebird.sitemanagement.site.LoginType;

import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "loginType",
        defaultImpl = FormLoginDTO.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = UrlLoginDTO.class, name = "URL"),
        @JsonSubTypes.Type(value = FormLoginDTO.class, name = "FORM")
})
@Schema(name = "LoginDTO",
        description = "A container object with the redirect-url needed for login.",
        discriminatorProperty = "loginType",
        oneOf = {UrlLoginDTO.class, FormLoginDTO.class},
        discriminatorMapping = {
                @DiscriminatorMapping(value = "URL", schema = UrlLoginDTO.class),
                @DiscriminatorMapping(value = "FORM", schema = FormLoginDTO.class)
        })
public interface LoginDTO {

    LoginType getLoginType();

    Login toLogin(final UUID userId);

}
