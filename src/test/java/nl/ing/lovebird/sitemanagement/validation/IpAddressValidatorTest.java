package nl.ing.lovebird.sitemanagement.validation;

import nl.ing.lovebird.sitemanagement.lib.validation.IpAddressValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class IpAddressValidatorTest {

    final IpAddressValidator ipAddressValidator = new IpAddressValidator();

    @Test
    void testIpAddressValidator() {
        assertThat(ipAddressValidator.isValid("127.0.0.1", null)).isTrue();
        assertThat(ipAddressValidator.isValid("219.78.118", null)).isFalse();
        assertThat(ipAddressValidator.isValid("::1", null)).isTrue();
        assertThat(ipAddressValidator.isValid("::fffff", null)).isFalse();
        assertThat(ipAddressValidator.isValid("no-hostnames-allowed.ing.com", null)).isFalse();
        assertThat(ipAddressValidator.isValid(null, null)).isTrue();
    }

}