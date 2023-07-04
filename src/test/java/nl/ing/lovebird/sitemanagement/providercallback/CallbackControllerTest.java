package nl.ing.lovebird.sitemanagement.providercallback;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.ing.lovebird.errorhandling.BaseErrorConstants;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.sitemanagement.exception.ExceptionHandlers;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockitoSettings(strictness = Strictness.WARN)
@WebMvcTest(controllers = CallbackController.class)
@Import({
        ExceptionHandlers.class
})
class CallbackControllerTest {

    @MockBean
    private ProviderCallbackAsyncService providerCallbackService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCallbackOK() throws Exception {
        this.mockMvc.perform(post("/callbacks/BUDGET_INSIGHT")
                        .content("something"))
                .andExpect(status().isOk());

        verify(providerCallbackService).processCallbackDataAsync(eq("BUDGET_INSIGHT"), isNull(), anyString());
        verifyNoMoreInteractions(providerCallbackService);
    }

    @Test
    void callbackBodyCannotBeNull() throws Exception {
        ErrorDTO expectedErrorResponse = new ErrorDTO("SM" + BaseErrorConstants.REQUEST_BODY_2_DTO_ERROR.getCode(), BaseErrorConstants.REQUEST_BODY_2_DTO_ERROR.getMessage());

        mockMvc.perform(post("/callbacks/BUDGET_INSIGHT")
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(expectedErrorResponse)))
                .andReturn();
    }

    @Test
    void callbackBodyCannotBeEmpty() throws Exception {
        ErrorDTO expectedErrorResponse = new ErrorDTO("SM" + BaseErrorConstants.REQUEST_BODY_2_DTO_ERROR.getCode(), BaseErrorConstants.REQUEST_BODY_2_DTO_ERROR.getMessage());

        mockMvc.perform(post("/callbacks/BUDGET_INSIGHT")
                        .content("")
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(new ObjectMapper().writeValueAsString(expectedErrorResponse)))
                .andReturn();
    }
}
