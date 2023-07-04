package nl.ing.lovebird.sitemanagement.flywheel;

import nl.ing.lovebird.errorhandling.ExceptionHandlingService;
import nl.ing.lovebird.sitemanagement.exception.ExceptionHandlers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class InternalFlywheelControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InternalFlywheelService internalFlyWheelService;

    @BeforeEach
    void setUp() {
        final InternalFlywheelController controller = new InternalFlywheelController(Clock.systemUTC(), internalFlyWheelService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ExceptionHandlers(new ExceptionHandlingService("SM"), "SM")).build();
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(internalFlyWheelService);
    }

    @Test
    void testRefreshAllUserSitesSuccess() throws Exception {
        mockMvc.perform(post("/flywheel/internal"))
                .andExpect(status().isAccepted());
        verify(internalFlyWheelService).refreshUserSitesAsync(any());
    }

    @Test
    void refreshAllUserSitesForASpecificUser() throws Exception {
        final UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/flywheel/internal/users/" + userId))
                .andExpect(status().isAccepted());

        verify(internalFlyWheelService).forceRefreshUserSitesForSpecificUserAsync(eq(userId));
    }
}