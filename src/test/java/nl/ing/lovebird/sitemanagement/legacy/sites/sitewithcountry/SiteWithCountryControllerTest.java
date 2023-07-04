package nl.ing.lovebird.sitemanagement.legacy.sites.sitewithcountry;


import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.configuration.SiteManagementDebugProperties;
import nl.ing.lovebird.sitemanagement.exception.ExceptionHandlers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SiteWithCountryController.class)
@ActiveProfiles("test")
@Import({
        ExceptionHandlers.class,
        SiteManagementDebugProperties.class,
})
class SiteWithCountryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestClientTokens testClientTokens;

    @Test
    void getNonExistingSite() throws Exception {
        var clientToken = testClientTokens.createClientToken(randomUUID(), randomUUID());

        var headers = new HttpHeaders();
        headers.put("user-id", singletonList(randomUUID().toString()));
        headers.put("cbms-profile-id", singletonList("yolt"));
        headers.add("client-token", clientToken.getSerialized());

        this.mockMvc.perform(get("/sites/" + randomUUID())
                .headers(headers))
                .andExpect(status().isGone());
    }
}