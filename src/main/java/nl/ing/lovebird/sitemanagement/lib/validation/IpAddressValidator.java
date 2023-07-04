package nl.ing.lovebird.sitemanagement.lib.validation;

import com.google.common.net.InetAddresses;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Slf4j
public class IpAddressValidator implements ConstraintValidator<IpAddress, String> {

    @Override
    public boolean isValid(final String ip, final ConstraintValidatorContext context) {
        if (ip == null) {
            return true;
        }
        try {
            // We deliberately don't use the jdk InetAddress class because it may do a DNS lookup.  The Guava method used deliberately avoids DNS calls.
            InetAddresses.forString(ip);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid ip address: {}", ip);
            return false;
        }
        return true;
    }

}
