package nl.ing.lovebird.sitemanagement.lib;

import nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI;
import org.junit.jupiter.api.Test;

import static nl.ing.lovebird.sitemanagement.lib.OAuth2RedirectionURI.matchUpWithRFC6749ErrorCodeConvention;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The term "valid" in this class means "it's a redirection URI for a successful authorization request",
 * conversely, "not valid" means the redirection URI is one that wasn't the result of a successful authorization
 * request.
 */
public class OAuth2RedirectionURITest {

    @Test
    void edgeCases() {
        assertThat(OAuth2RedirectionURI.parse(null)).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.invalid("null_uri", null));
        assertThat(OAuth2RedirectionURI.parse("https://")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.invalid("invalid_uri", null));
    }

    @Test
    void garbage() {
        assertThat(OAuth2RedirectionURI.parse("https://garbage@#$")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.invalid("invalid_uri", null));
    }

    @Test
    void httpsOnly() {
        assertThat(OAuth2RedirectionURI.parse("ftp://www.example.com")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.invalid("invalid_uri", null));
    }

    @Test
    void given_redirectionURIWithCodeAndState_when_validating_then_isValid() {
        // query
        assertThat(validate("?code=1&state=2")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("2"));
        assertThat(validate("?state=2&code=1")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("2"));
        assertThat(validate("?someotherstuff=notinteresting&state=2&code=1")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("2"));
        // fragment
        assertThat(validate("#code=1&state=2")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("2"));
        assertThat(validate("#state=2&code=1")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("2"));
        assertThat(validate("#someotherstuff=notinteresting&state=2&code=1")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("2"));
        // mixed
        assertThat(validate("?code=1#state=2")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("2"));
        assertThat(validate("?state=2#code=1")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("2"));
    }

    @Test
    void given_redirectionURIWithErrorParameter_when_validating_then_isInvalid() {
        assertThat(validate("?state=1&error=access_denied")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.invalid("access_denied", "1"));
        assertThat(validate("?error=access_denied")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.invalid("access_denied", null));
        assertThat(validate("?state=1&error=nonexistingerror")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.invalid("invalid_uri", "1"));
        assertThat(validate("?error=nonexistingerror")).isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.invalid("invalid_uri", null));
    }

    @Test
    void migratedTestsFromUrlParser() {
        assertThat(OAuth2RedirectionURI.parse("http://localhost/?code=some_code&siteId=site_x&state=some_state"))
                .isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("some_state"));

        assertThat(OAuth2RedirectionURI.parse("https://www.yolt.com/callback/d28b4598-efcf-41c8-8522-08b2744e551a?state=06f53fb4-b0b6-453f-90e5-07f3efd636e1&error=access_denied#."))
                .isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.invalid("access_denied", "06f53fb4-b0b6-453f-90e5-07f3efd636e1"));

        assertThat(OAuth2RedirectionURI.parse("https://callback?code=some_code&siteId=site_x&state=some_state"))
                .isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("some_state"));

        assertThat(OAuth2RedirectionURI.parse("http://localhost#code=some_code&siteId=site_x&state=some_state"))
                .isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("some_state"));

        assertThat(OAuth2RedirectionURI.parse("https://callback#code=some_code&siteId=site_x&state=some_state"))
                .isEqualTo(OAuth2RedirectionURI.RedirectionURIValidationResult.valid("some_state"));
    }

    @Test
    void shouldMatchUpWithRFC6749ErrorCodeConventionValid() {
        assertThat(matchUpWithRFC6749ErrorCodeConvention("server_error")).isEqualTo("server_error");
    }

    @Test
    void shouldMatchUpWithRFC6749ErrorCodeConventionWithHyphen() {
        assertThat(matchUpWithRFC6749ErrorCodeConvention("server-error")).isEqualTo("server_error");
    }

    @Test
    void shouldMatchUpWithRFC6749ErrorCodeConventionWithMixedCase() {
        assertThat(matchUpWithRFC6749ErrorCodeConvention("Server_Error")).isEqualTo("server_error");
    }

    @Test
    void shouldMatchUpWithRFC6749ErrorCodeConventionUnmatched() {
        assertThat(matchUpWithRFC6749ErrorCodeConvention("invalid")).isEqualTo("invalid");
    }

    @Test
    void shouldMatchUpWithRFC6749ErrorCodeConventionWithNull() {
        assertThat(matchUpWithRFC6749ErrorCodeConvention(null)).isNull();
    }

    private OAuth2RedirectionURI.RedirectionURIValidationResult validate(String v) {
        return OAuth2RedirectionURI.parse("https://www.example.com" + v);
    }

}