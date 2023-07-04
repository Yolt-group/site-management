package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.exception.UserIdMismatchException;
import nl.ing.lovebird.sitemanagement.legacy.usersite.UserSiteController;
import nl.ing.lovebird.sitemanagement.consentsession.ConsentSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class UserSiteControllerV1Test {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID USER_SITE_ID = UUID.randomUUID();
    private static final String PSU_IP_ADDRESS = "192.168.0.1";
    private static final RedirectStep REDIRECT_STEP = new RedirectStep("", "", "", UUID.randomUUID());

    @Mock
    private UserSiteController userSiteController;

    @Mock
    private CreateOrUpdateUserSiteService createOrUpdateUserSiteService;

    @Mock
    private ConsentSessionService userSiteSessionService;

    @Mock
    private ClientUserToken clientToken;

    @InjectMocks
    private UserSiteControllerV1 subject;

    @BeforeEach
    void beforeEach() {
        when(clientToken.getUserIdClaim()).thenReturn(USER_ID);
    }

    @Test
    void renewAccess() throws Exception {
        UUID redirectUrl = UUID.randomUUID();
        when(createOrUpdateUserSiteService.createLoginStepToRenewAccess(clientToken, USER_SITE_ID, redirectUrl, PSU_IP_ADDRESS)).thenReturn(REDIRECT_STEP);
        final ResponseEntity<LoginStepV1DTO> actual = subject.renewAccess(USER_ID, USER_SITE_ID, clientToken, redirectUrl, PSU_IP_ADDRESS);
        assertThat(actual.getBody().getRedirect()).isNotNull();
    }

    @Test
    void refreshAllUserSites_shouldDelegateToOldController() throws Exception {
        subject.refreshAllUserSites(USER_ID, clientToken, PSU_IP_ADDRESS);
        verify(userSiteController).refreshAllUserSites(USER_ID, clientToken, PSU_IP_ADDRESS);
    }

    @Test
    void refreshUserSite_shouldDelegateToOldController() throws Exception {
        subject.refreshUserSite(USER_SITE_ID, USER_ID, clientToken, PSU_IP_ADDRESS);
        verify(userSiteController).refreshUserSite(USER_SITE_ID, USER_ID, clientToken, PSU_IP_ADDRESS);
    }

    @Test
    void deleteUserSite_shouldDelegateToOldController() throws UserIdMismatchException {
        final ResponseEntity<Void> actual = subject.deleteUserSite(USER_ID, USER_SITE_ID, PSU_IP_ADDRESS, clientToken);

        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(userSiteController).deleteUserSite(USER_SITE_ID, PSU_IP_ADDRESS, clientToken);
    }
}
