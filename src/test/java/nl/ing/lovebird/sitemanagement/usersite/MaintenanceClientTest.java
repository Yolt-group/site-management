package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.SiteManagementApplication;
import nl.ing.lovebird.sitemanagement.maintenanceclient.MaintenanceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(MaintenanceClient.class)
@ContextConfiguration(classes = SiteManagementApplication.class)
class MaintenanceClientTest {

    final UUID userid = UUID.randomUUID();
    final UUID siteId = UUID.randomUUID();

    @Autowired
    MaintenanceClient maintenanceClient;
    @Autowired
    MockRestServiceServer server;

    @Test
    void shouldScheduleUserSiteDelete() {
        server.expect(requestTo("/maintenance/user-site/" + userid + "/" + siteId))
                .andRespond(withSuccess());
        assertDoesNotThrow(() -> maintenanceClient.scheduleUserSiteDelete(userid, siteId));
    }

    @AfterEach
    void verify() {
        server.verify();
    }
}