package nl.ing.lovebird.sitemanagement.users;

import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void testGetUser() {
        UUID userId = UUID.randomUUID();

        userService.getUser(userId);

        verify(userRepository).getUser(userId);
    }


    @Test
    void testDeleteUser_whenPresent_shouldBeRemoved_andCounterDecremented() {
        User user = user();
        when(userRepository.getUser(user.getUserId())).thenReturn(Optional.of(user));

        userService.deleteUser(user.getUserId());

        verify(userRepository).getUser(user.getUserId());
        verify(userRepository).deleteUser(user.getUserId());
    }

    @Test
    void testDeleteUser_whenNotPresent_shouldNotBeRemoved_andCounterNotDecremented() {
        User user = user();
        when(userRepository.getUser(user.getUserId())).thenReturn(Optional.empty());

        userService.deleteUser(user.getUserId());

        verify(userRepository).getUser(user.getUserId());
        verify(userRepository, never()).deleteUser(user.getUserId());
    }

    private static User user() {
        return new User(UUID.randomUUID(), Instant.now(systemUTC()), ClientId.random(), StatusType.ACTIVE, false);
    }
}
