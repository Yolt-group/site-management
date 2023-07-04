package nl.ing.lovebird.sitemanagement.usersiteevent;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.AbstractClientToken;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Slf4j
@Service
public class UserSiteEventService {
    private final Clock clock;
    private final KafkaTemplate<String, UserSiteEventAbstract> kafkaTemplate;
    private final String topic;

    public UserSiteEventService(KafkaTemplate<String, UserSiteEventAbstract> kafkaTemplate,
                                @Value("${yolt.kafka.topics.user-site-events.topic-name}") String topic,
                                Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.clock = clock;
    }

    /**
     * Publish a {@link UserSiteEventUpdate} informing subscribers of a change in {@link PostgresUserSite} status.
     * @param userId     the user-id
     * @param userSiteId the user-site id
     * @param siteId     the site-id
     */
    public void publishUserSiteUpdate(final UUID userId, final UUID userSiteId, final UUID siteId) {
        UserSiteEventUpdate updateEvent
                = new UserSiteEventUpdate(userSiteId, userId, siteId, ZonedDateTime.now(clock));
        publishEvent(updateEvent, null);
    }

    public void publishCreatedUserSite(final PostgresUserSite userSite) {
        UserSiteEventUpdate updateEvent = new UserSiteEventUpdate(userSite.getUserSiteId(),
                userSite.getUserId(),
                userSite.getSiteId(),
                ZonedDateTime.now(clock));

        publishEvent(updateEvent, null);
    }

    public void publishDeleteUserSiteEvent(final PostgresUserSite userSite,
                                           final @NonNull ClientToken clientToken) {
        UserSiteEventDelete deleteEvent = new UserSiteEventDelete(
                userSite.getUserSiteId(),
                userSite.getUserId(),
                userSite.getSiteId(),
                ZonedDateTime.now(clock));
        publishEvent(deleteEvent, clientToken);
    }

    private void publishEvent(final UserSiteEventAbstract event,
                              final ClientToken clientToken) {

        Message<UserSiteEventAbstract> message = MessageBuilder
                .withPayload(event)
                .setHeader("type", event.getType().name())
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.MESSAGE_KEY, event.getUserId().toString())
                .setHeader(CLIENT_TOKEN_HEADER_NAME, Optional.ofNullable(clientToken).map(AbstractClientToken::getSerialized).orElse(null))
                .build();

        kafkaTemplate.send(message);
    }
}
