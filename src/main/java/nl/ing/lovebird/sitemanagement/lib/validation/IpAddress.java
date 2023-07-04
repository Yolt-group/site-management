package nl.ing.lovebird.sitemanagement.lib.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = IpAddressValidator.class)
@Target( { ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface IpAddress {
    String message() default "Invalid ip address";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
