package nl.ing.lovebird.sitemanagement.batch;

import nl.ing.lovebird.clienttokens.requester.service.ClientTokenRequesterService;
import nl.ing.lovebird.sitemanagement.providerclient.ProviderRestClient;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteMaintenanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PeriodicBatchController.class)
@ActiveProfiles("test")
class PeriodicBatchControllerTest {

    @MockBean
    private ClientTokenRequesterService clientTokenRequesterService;
    @MockBean
    private ProviderRestClient providerRestClient;
    @MockBean
    private DiagnosticLoggingService diagnosticLoggingService;
    @MockBean
    private UserSiteMaintenanceService userSiteMaintenanceService;
    @MockBean
    private DisconnectUnusableUserSitesService disconnectUnusableUserSitesService;
    @MockBean
    private BatchUserSiteDeleteService batchUserSiteDeleteService;
    @MockBean
    private ConsentTestingService consentTestingService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testConstraintViolationOnDaysInPastForUniqueRefreshes() throws Exception {
        this.mockMvc.perform(get("/batch/unique-refreshes")
                        .queryParam("days-in-past", "6"))
                .andExpect(status().isAccepted());
        verify(diagnosticLoggingService).logNumberOfUniqueUserRefreshes(6);
    }
}
