package nl.ing.lovebird.sitemanagement.health.webhook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;


@EqualsAndHashCode
@RequiredArgsConstructor
@Builder(toBuilder = true)
@ToString
public class WebhookEventEnvelope {

    @NonNull
    @ToString.Include
    @Schema(required = true, description = "The client identifier.", example = "dacba0a6-2305-4359-b942-fea028602a7b")
    public final UUID clientId;

    @NonNull
    @ToString.Include
    @Schema(required = true, description = "The internal user identifier.", example = "dacba0a6-2305-4359-b942-fea028602a7b")
    public final UUID userId;

    @NonNull
    @ToString.Include
    @Schema(required = true, description = "The webhook event type (e.a.)", example = "ACTIVITY_LIFECYCLE")
    public final WebhookEventType webhookEventType;

    @NonNull
    @Schema(required = true, description = "The untyped (but JSON compatible) payload to be send to the webhook endpoint of the client.", example = "{...}")
    public final AISWebhookEventPayload payload;

    @NonNull
    @ToString.Include
    @Schema(required = true, description = "The moment this webhook event was send from the origin.", example = "2018-01-01T10:10:10.123[GMT]")
    public final ZonedDateTime submittedAt;

    public enum WebhookEventType {
        AIS
    }
}
