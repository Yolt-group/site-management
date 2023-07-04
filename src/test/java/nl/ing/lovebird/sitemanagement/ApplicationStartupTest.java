package nl.ing.lovebird.sitemanagement;

import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestContext
public class ApplicationStartupTest {

    @LocalManagementPort
    private int managementPort;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private HealthEndpointGroups healthEndpointGroups;


    @Test
    void applicationStartsUp() {
        HttpStatus statusCode = restTemplate.getForEntity("http://localhost:" + managementPort + "/actuator/info", String.class).getStatusCode();
        assertThat(statusCode).isEqualTo(HttpStatus.OK);
    }

    @Test
    void when_appStartsUp_then_healthEndpointGroupsAreConfiguredCorrectly() {
        assertThat(healthEndpointGroups.getNames()).contains("readiness");

    }

    @Test
    void when_IRequestSwaggerThroughTheAPI_then_swaggerShouldBeReturned() throws Exception {
        this.mockMvc.perform(get("/v3/api-docs?group=site-management"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.paths./v1/users/{userId}/user-sites/{userSiteId}/refresh").exists());
    }
}
