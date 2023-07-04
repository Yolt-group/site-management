package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.exception.UserSiteNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class UserSiteServiceTest {

    @Mock
    private PostgresUserSiteRepository postgresUserSiteRepository;

    @InjectMocks
    private UserSiteService userSiteService;

    @Test
    void testUserSiteMarkedForDelete() {
        assertThatThrownBy(() -> {
            final UUID randomUserId = UUID.randomUUID();
            final UUID randomUserSiteId = UUID.randomUUID();
            final PostgresUserSite randomUserSite = new PostgresUserSite();
            randomUserSite.setUserId(randomUserId);
            randomUserSite.setUserSiteId(randomUserSiteId);
            randomUserSite.setDeleted(true);
            when(postgresUserSiteRepository.getUserSite(randomUserId, randomUserSiteId)).thenReturn(Optional.of(randomUserSite));
            userSiteService.getUserSite(randomUserId, randomUserSiteId);
        }).isInstanceOf(UserSiteNotFoundException.class);
    }
}
