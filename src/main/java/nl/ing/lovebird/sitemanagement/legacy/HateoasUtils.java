package nl.ing.lovebird.sitemanagement.legacy;

import lombok.experimental.UtilityClass;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

/**
 * Yolt has decided to drop all use of hateoas.  This is still used in several legacy andpoints which is why we
 * have to keep it around until those endpoints disappear.
 */
@UtilityClass
@Deprecated
public class HateoasUtils {
    public static String makePath(final String absolutePath) {
        if (!absolutePath.matches("^(http://|https://).*")) {
            throw new IllegalArgumentException(String.format("Href '%s' does not appear to be a full http(s):// URI", absolutePath));
        }
        final int index = absolutePath.indexOf("/", absolutePath.indexOf("://") + 3);

        return absolutePath.substring(index);
    }

    public static String createPath(final Object invocationValue) {
        return makePath(create(invocationValue));
    }

    private static String create(final Object invocationValue) {
        return WebMvcLinkBuilder.linkTo(invocationValue).toString();
    }
}
