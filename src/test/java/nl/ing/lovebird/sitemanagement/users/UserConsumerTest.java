package nl.ing.lovebird.sitemanagement.users;

import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteRefreshService;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static nl.ing.lovebird.clienttokens.test.TestJwtClaims.createClientClaims;
import static nl.ing.lovebird.clienttokens.test.TestJwtClaims.createClientUserClaims;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class UserConsumerTest {

    private static final Instant USER_LAST_LOGIN_DATE = Instant.now(systemUTC());
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CLIENT_GROUP_ID = UUID.randomUUID();
    private static final ClientId CLIENT_ID = ClientId.random();
    private static final User USER = new User(USER_ID, USER_LAST_LOGIN_DATE, CLIENT_ID, StatusType.ACTIVE, false);
    private static final ClientToken CLIENT_TOKEN = new ClientToken("client-token", createClientClaims("junit", CLIENT_GROUP_ID, CLIENT_ID.unwrap()));
    private static final ClientUserToken CLIENT_USER_TOKEN = new ClientUserToken("client-user-token", createClientUserClaims("junit", CLIENT_GROUP_ID, CLIENT_ID.unwrap(), USER_ID));

    @Mock
    private UserService userService;
    @Mock
    private UserSiteService userSiteService;
    @Mock
    private UserSiteRefreshService userSiteRefreshService;

    @InjectMocks
    private UserConsumer subject;

    @Test
    void whenNoUserId_shouldThrowException() {
        final var userMessage = prepareUserMessage(null, CLIENT_ID, UserMessage.Payload.UserMessageType.USER_UPDATED, false);

        assertThatThrownBy(() -> subject.userUpdate(CLIENT_TOKEN, null, userMessage)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void whenNoClientId_shouldThrowException() {
        final var userMessage = prepareUserMessage(USER_ID, null, UserMessage.Payload.UserMessageType.USER_UPDATED, false);

        assertThatThrownBy(() -> subject.userUpdate(CLIENT_TOKEN, null, userMessage)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testIncomingUserMessage_RegularUserCreated() {
        final var userMessage = prepareUserMessage(UserMessage.Payload.UserMessageType.USER_CREATED);

        subject.userUpdate(null, null, userMessage);
        verify(userService).saveUser(USER);
    }

    @Test
    void testIncomingUserMessage_OneOffAisUserCreated() {
        final var userMessage = prepareUserMessage(USER_ID, CLIENT_ID, UserMessage.Payload.UserMessageType.USER_CREATED, true);
        subject.userUpdate(null, null, userMessage);
        verify(userService).saveUser(new User(USER_ID, USER_LAST_LOGIN_DATE, CLIENT_ID, StatusType.ACTIVE, true));
    }

    @Test
    void testIncomingUserMessage_RegularUserUpdated() {
        final var userMessage = prepareUserMessage(UserMessage.Payload.UserMessageType.USER_UPDATED);

        subject.userUpdate(CLIENT_USER_TOKEN, null, userMessage);
        verify(userService).saveUser(USER);
    }

    @Test
    void testIncomingUserMessage_OneOffAisUserUpdated() {
        final var userMessage = prepareUserMessage(USER_ID, CLIENT_ID, UserMessage.Payload.UserMessageType.USER_UPDATED, true);

        subject.userUpdate(CLIENT_USER_TOKEN, null, userMessage);
        verify(userService).saveUser(new User(USER_ID, USER_LAST_LOGIN_DATE, CLIENT_ID, StatusType.ACTIVE, true));
    }

    @Test
    void testIncomingUserMessage_OneOffAisUserBecomingRegularUser() {
        final var userMessage = prepareUserMessage(USER_ID, CLIENT_ID, UserMessage.Payload.UserMessageType.USER_UPDATED, false);
        final var userSites = Collections.singletonList(PostgresUserSite.builder()
                .userId(USER_ID).userSiteId(UUID.randomUUID()).build());

        when(userService.getUser(USER_ID)).thenReturn(Optional.of(new User(USER_ID, null, CLIENT_ID, StatusType.ACTIVE, true)));
        when(userSiteService.getNonDeletedUserSites(USER_ID)).thenReturn(userSites);

        subject.userUpdate(CLIENT_USER_TOKEN, null, userMessage);

        verify(userService).saveUser(new User(USER_ID, USER_LAST_LOGIN_DATE, CLIENT_ID, StatusType.ACTIVE, false));
    }

    @Test
    void testIncomingUserMessage_UserDeleted() {
        final var userMessage = prepareUserMessage(UserMessage.Payload.UserMessageType.USER_DELETED);

        subject.userUpdate(CLIENT_USER_TOKEN, null, userMessage);
        verify(userService).deleteUser(USER_ID);
    }

    private static UserMessage prepareUserMessage(UserMessage.Payload.UserMessageType userMessageType) {
        final var headers = prepareHeaders(userMessageType);
        final var payload = preparePayload(USER_ID, CLIENT_ID, USER.isOneOffAis());

        return new UserMessage(headers, payload);
    }

    private static UserMessage prepareUserMessage(UUID userId, ClientId clientId, UserMessage.Payload.UserMessageType userMessageType, boolean oneOffAis) {
        final var headers = prepareHeaders(userMessageType);
        final var payload = preparePayload(userId, clientId, oneOffAis);

        return new UserMessage(headers, payload);
    }

    private static UserMessage.Headers prepareHeaders(UserMessage.Payload.UserMessageType userMessageType) {
        return new UserMessage.Headers(userMessageType.name());
    }

    private static UserMessage.Payload preparePayload(UUID userId, ClientId clientId, boolean oneOffAis) {
        final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(USER_LAST_LOGIN_DATE, ZoneId.of("UTC"));

        return new UserMessage.Payload(userId, clientId, null, oneOffAis, zonedDateTime);
    }

}
