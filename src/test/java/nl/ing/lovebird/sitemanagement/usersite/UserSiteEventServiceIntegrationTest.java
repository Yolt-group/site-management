package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersiteevent.UserSiteEventService;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.usersite.UserSiteTestUtil.bulkPersistPostgresUserSites;
import static nl.ing.lovebird.sitemanagement.usersiteevent.EventType.UPDATE_USER_SITE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@IntegrationTestContext
class UserSiteEventServiceIntegrationTest {

    @Autowired
    private UserSiteEventService userSiteEventService;

    @Autowired
    private UserSiteEventTestConsumer consumer;

    @BeforeEach
    public void onBeforeEach() {
        consumer.reset();
    }

    @Test
    public void shouldPublish31MessagesOverKafka() {
        UUID siteId = randomUUID();

        List<PostgresUserSite> postgresUserSites = bulkPersistPostgresUserSites(31, ClientId.random(), siteId, UUID::randomUUID,
                (builder, i) -> builder, Function.identity());

        postgresUserSites.forEach(userSite -> userSiteEventService.publishUserSiteUpdate(userSite.getUserId(), userSite.getUserSiteId(), userSite.getSiteId()));

        // verify that publishUserSiteUpdate was called for every user-site (31 times)
        verify(userSiteEventService, times(31)).publishUserSiteUpdate(any(), any(), eq(siteId));

        // verify there are at least 31 kafka messages produced
        Awaitility.await().untilAsserted(() -> Assertions.assertThat(consumer.getConsumed()
                .stream()
                .filter(consumed -> consumed.getValue().getSiteId().equals(siteId))
                .filter(consumed -> consumed.getMessageType().equals(UPDATE_USER_SITE.name()))
                .count()).isEqualTo(31));
    }
}
