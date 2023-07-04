package nl.ing.lovebird.sitemanagement.legacy;

import nl.ing.lovebird.sitemanagement.legacy.HateoasUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class HateoasUtilsTest {

    @Test
    void testMakePath() {
        assertThat(HateoasUtils.makePath("http://localhost:8080/some/path")).isEqualTo("/some/path");
        assertThat(HateoasUtils.makePath("https://localhost:8080/some/path")).isEqualTo("/some/path");

        assertThat(HateoasUtils.makePath("http://localhost:8080/some/path?param=value")).isEqualTo("/some/path?param=value");
        assertThat(HateoasUtils.makePath("https://localhost:8080/some/path?param=value")).isEqualTo("/some/path?param=value");
    }

    @Test
    void testInvalidUri() {
        assertThrows(IllegalArgumentException.class, () -> HateoasUtils.makePath("/some/path"));
    }
}