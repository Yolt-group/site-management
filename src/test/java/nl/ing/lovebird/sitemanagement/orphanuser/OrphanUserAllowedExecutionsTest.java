package nl.ing.lovebird.sitemanagement.orphanuser;

import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class OrphanUserAllowedExecutionsTest {

    private static final UUID FAKE_BATCH_ID = UUID.randomUUID();
    private static final UUID REAL_BATCH_ID = UUID.fromString("191cd44d-7926-4ea1-b7fa-9ba25cda7084");

    @Autowired
    private OrphanUserAllowedExecutions allowedExecutions;

    @Test
    void testIsAllowed() {
        assertThat(allowedExecutions.isAllowed("AIB", FAKE_BATCH_ID)).isFalse();
        assertThat(allowedExecutions.isAllowed("YODLEE", FAKE_BATCH_ID)).isFalse();
        assertThat(allowedExecutions.isAllowed("AIB", REAL_BATCH_ID)).isFalse();
        assertThat(allowedExecutions.isAllowed("YODLEE", REAL_BATCH_ID)).isTrue();
    }
}
