package nl.ing.lovebird.sitemanagement.health.webhook.dto;


import lombok.*;
import nl.ing.lovebird.activityevents.events.ConnectionStatus;
import nl.ing.lovebird.activityevents.events.FailureReason;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;

/*
 *         {
 *           "activityId": "01879009-02b3-47bf-b413-5d065c54bb81",
 *           "activity": "CREATE_USER_SITE | UPDATE_USER_SITE | BACKGROUND_REFRESH | USER_REFRESH",
 *           "event": "DATA_SAVED | ACTIVITY_FINISHED",
 *           "dateTime": "01-01-2020T02:02:02+01:00",
 *           "userSites": [
 *             {
 *               "userSiteId": "2bf8da35-d366-4275-81fc-8f41074dc9ce",
 *               "lastDataFetch" : "2020-01-01T01:01:01+02:00",
 *               "connectionStatus" : "CONNECTED"
 *               "accounts" : [
 *                 {
 *                   "accountId": "2bf8da35-d366-4275-81fc-8f41074dc9ce",
 *                   "oldestChangedTransaction" : "01-01-2020"
 *                 }
 *               ]
 *             }
 *           ]
 *         }
 *
 *
 */

@ToString
@Builder(toBuilder = true)
@EqualsAndHashCode
@RequiredArgsConstructor
public class AISWebhookEventPayload {

    @NonNull
    public final UUID activityId;

    @NonNull
    public final AISWebhookEventPayload.WebhookActivity activity;

    @NonNull
    public final WebhookEvent event;

    @NonNull
    public final ZonedDateTime dateTime;

    @NonNull
    public final Set<UserSiteResult> userSites;

    @Builder(toBuilder = true)
    @ToString
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class UserSiteResult {
        @NonNull
        public final UUID userSiteId;
        @NonNull
        public final ConnectionStatus connectionStatus;
        @NonNull
        public final Optional<ZonedDateTime> lastDataFetch;
        @NonNull
        public final Optional<FailureReason> failureReason;
        @Nullable
        public final List<AffectedAccount> accounts;
    }

    @Value
    @Builder
    @ToString
    @EqualsAndHashCode
    public static class AffectedAccount implements Comparable<AffectedAccount> {
        UUID accountId;
        LocalDate oldestChangedTransaction;

        @Override
        public int compareTo(@NonNull AffectedAccount other) {
            return Comparator.comparing(AffectedAccount::getAccountId)
                    .thenComparing(AffectedAccount::getOldestChangedTransaction)
                    .compare(this, other);
        }
    }

    public enum WebhookActivity {
        CREATE_USER_SITE,
        UPDATE_USER_SITE,
        BACKGROUND_REFRESH,
        USER_REFRESH,
        USER_FEEDBACK
    }

    public enum WebhookEvent {
        DATA_SAVED,
        ACTIVITY_FINISHED
    }
}
