package nl.ing.lovebird.sitemanagement.lib.validation;

import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HaveRedirectUrlForUrlProviderValidatorTest {

    PostgresUserSite userSite;

    HaveRedirectUrlForUrlProviderValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HaveRedirectUrlForUrlProviderValidator();
    }

    @Test
    void isValidYodlee() {
        userSite = new PostgresUserSite();
        userSite.setProvider("YODLEE");

        boolean valid = validator.isValid(userSite, null);

        assertThat(valid).isTrue();
    }

    @Test
    void isValidUrl() {
        userSite = new PostgresUserSite();
        userSite.setProvider("STARLINGBANK");
        userSite.setRedirectUrlId(UUID.randomUUID());

        boolean valid = validator.isValid(userSite, null);

        assertThat(valid).isTrue();
    }

    @Test
    void isValidException() {
        userSite = mock(PostgresUserSite.class);

        boolean valid = validator.isValid(userSite, null);

        assertThat(valid).isFalse();
    }
}
