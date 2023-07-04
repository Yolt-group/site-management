package nl.ing.lovebird.sitemanagement.lib.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = HaveRedirectUrlForUrlProviderValidator.class)
@Target( { ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface HaveRedirectUrlForUrlProvider {
    String message() default "A redirectUrl must only be specified for provider type URL.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
