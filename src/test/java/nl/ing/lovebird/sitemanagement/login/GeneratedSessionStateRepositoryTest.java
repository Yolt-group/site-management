package nl.ing.lovebird.sitemanagement.login;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.consentsession.GeneratedSessionState;
import nl.ing.lovebird.sitemanagement.consentsession.GeneratedSessionStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class GeneratedSessionStateRepositoryTest {

    @Autowired
    private Session session;
    private GeneratedSessionStateRepository generatedSessionStateRepository;

    @BeforeEach
    void setUp() {
        generatedSessionStateRepository = new GeneratedSessionStateRepository(session);
    }

    @Test
    void test() {
        UUID userId = UUID.randomUUID();
        GeneratedSessionState submittedState = new GeneratedSessionState(userId, UUID.randomUUID().toString(), new Date(), false, UUID.randomUUID());
        GeneratedSessionState generatedSessionState2 = new GeneratedSessionState(userId, UUID.randomUUID().toString(), new Date(), false, UUID.randomUUID());
        generatedSessionStateRepository.store(submittedState);
        generatedSessionStateRepository.store(generatedSessionState2);

        generatedSessionStateRepository.markAsSubmitted(submittedState.getUserId(), submittedState.getStateId());

        List<GeneratedSessionState> generatedSessionStates = generatedSessionStateRepository.get(userId);
        assertThat(generatedSessionStates.size()).isEqualTo(2);
        List<GeneratedSessionState> submittedStates = generatedSessionStates.stream().filter(it -> it.getStateId().equals(submittedState.getStateId()))
                .collect(Collectors.toList());
        assertThat(submittedStates.size()).isEqualTo(1);

        GeneratedSessionState nonSubmittedState = generatedSessionStates.stream().filter(it -> it.getStateId().equals(generatedSessionState2.getStateId())).findFirst().get();
        assertThat(nonSubmittedState).isEqualTo(generatedSessionState2);
    }

}
