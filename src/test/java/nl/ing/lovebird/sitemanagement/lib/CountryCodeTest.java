package nl.ing.lovebird.sitemanagement.lib;

import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.exception.UnknownCountryCodeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CountryCodeTest {

    @Test
    void whenCountryCodeIsValid_shouldReturnEnumValue() {
        CountryCode countryCode = CountryCode.fromName("GB");

        assertThat(countryCode).isEqualTo(CountryCode.GB);
    }

    @Test
    void whenCountryCodeIsInvalid_shouldThrowException() {
        assertThatThrownBy(() -> CountryCode.fromName("XX")).isInstanceOf(UnknownCountryCodeException.class);
    }
}
