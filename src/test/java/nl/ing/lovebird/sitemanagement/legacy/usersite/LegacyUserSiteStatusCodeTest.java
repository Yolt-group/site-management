package nl.ing.lovebird.sitemanagement.legacy.usersite;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LegacyUserSiteStatusCodeTest {

    @Test
    void testIsSuccessState() {
        assertThat(LegacyUserSiteStatusCode.INITIAL_PROCESSING.isSuccessState()).isTrue();
        assertThat(LegacyUserSiteStatusCode.LOGIN_SUCCEEDED.isSuccessState()).isTrue();
        assertThat(LegacyUserSiteStatusCode.REFRESH_FINISHED.isSuccessState()).isTrue();
        assertThat(LegacyUserSiteStatusCode.REFRESH_FAILED.isSuccessState()).isFalse();
        assertThat(LegacyUserSiteStatusCode.UNKNOWN.isSuccessState()).isFalse();
        assertThat(LegacyUserSiteStatusCode.LOGIN_FAILED.isSuccessState()).isFalse();
    }

}
