package nl.ing.lovebird.sitemanagement.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.users.UserMessage.Payload.UserMessageType;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteRefreshService;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType.USER_REFRESH;

@Slf4j
@RequiredArgsConstructor
@Service
class UserConsumer {
    private static final String PSU_IP_ADDRESS = "psu-ip-address";

    private final UserService userService;
    private final UserSiteRefreshService userSiteRefreshService;
    private final UserSiteService userSiteService;

    @KafkaListener(
            topics = "${yolt.kafka.topics.users.topic-name}",
            concurrency = "${yolt.kafka.topics.users.listener-concurrency}"
    )
    void userUpdate(
            // A ClientToken in case of USER_CREATED
            // A ClientUserToken in case of USER_UPDATED, USER_DELETED
            @Header(value = CLIENT_TOKEN_HEADER_NAME) final ClientToken clientToken,
            @Header(value = PSU_IP_ADDRESS, required = false) final String psuIpAddress,
            @Payload final UserMessage value
    ) {

        log.debug("consumed message on users topic {}", value); //NOSHERIFF debug only.
        final var messageType = UserMessageType.fromValue(value.headers().messageType());
        final var lastLogin = value.payload().lastLogin();
        final var userId = value.payload().id();
        final var clientId = value.payload().clientId();
        final var newOneOffAisFlag = value.payload().oneOffAis();

        if (userId == null || clientId == null) {
            throw new IllegalArgumentException("Both userId and clientId should be provided, userId: " + userId
                    + ", clientId: " + clientId + " for " + messageType);
        }

        final var user = new User(userId, lastLogin == null ? null : lastLogin.toInstant(), clientId, StatusType.ACTIVE, newOneOffAisFlag);

        switch (messageType) {
            case USER_CREATED -> userService.saveUser(user);
            case USER_UPDATED -> {
                ClientUserToken clientUserToken = requireClientUserToken(clientToken);
                var userTransitionsToRegularUser = userTransitionsToRegularUser(userId, newOneOffAisFlag);

                userService.saveUser(user);

                if (userTransitionsToRegularUser) {
                    refreshUserSites(newOneOffAisFlag, clientUserToken, removeQuotesFromHeader(psuIpAddress));
                }
            }
            case USER_LOGIN -> throw new UnsupportedOperationException("User event USER_LOGIN should not be produceed by yts-users.");
            case USER_DELETED -> {
                requireClientUserToken(clientToken);
                userService.deleteUser(userId);
                log.info("User {} is deleted", userId);
            }
        }
    }

    private String removeQuotesFromHeader(String header) {
        if (header == null) {
            return null;
        }
        // Handle JSON encoded strings. Remove when all pods on > 13.0.25
        // See: https://yolt.atlassian.net/browse/CHAP-145
        if (header.length() > 1 && header.startsWith("\"") && header.endsWith("\"")) {
            return header.substring(1, header.length() - 1);
        }
        return header;
    }

    private static ClientUserToken requireClientUserToken(ClientToken clientToken) {
        if(!(clientToken instanceof ClientUserToken clientUserToken)) {
            throw new IllegalArgumentException("A ClientUserToken is required in case of USER_UPDATED, USER_DELETED");
        }
        return clientUserToken;
    }

    private void refreshUserSites(boolean oneOffAisUser, ClientUserToken clientUserToken, String psuIpAddress) {
        userSiteRefreshService.refreshUserSitesAsync(
                userSiteService.getNonDeletedUserSites(clientUserToken.getUserIdClaim()),
                oneOffAisUser,
                clientUserToken,
                USER_REFRESH,
                psuIpAddress,
                null
        );
    }

    private boolean userTransitionsToRegularUser(UUID userId, boolean newOneOffAisFlag) {
        return !newOneOffAisFlag && isOneOffAisUser(userId);
    }

    private boolean isOneOffAisUser(UUID userId) {
        return userService.getUser(userId)
                .map(User::isOneOffAis)
                .orElse(false);
    }
}
