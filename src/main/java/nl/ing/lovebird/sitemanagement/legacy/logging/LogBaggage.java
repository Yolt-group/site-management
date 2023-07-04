package nl.ing.lovebird.sitemanagement.legacy.logging;

import brave.propagation.ExtraFieldPropagation;
import lombok.Builder;
import lombok.Value;
import nl.ing.lovebird.logging.MDCContextCreator;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Use this class in a try-with-resources block to ensure that stuff gets removed from the MDC after you're done.
 */
@Value
@Deprecated
public class LogBaggage implements AutoCloseable {

    UUID userId;
    UUID siteId;
    UUID userSiteId;
    String provider;
    ClientId clientId;
    Map<String, String> previousMDC;
    Map<String, String> previousTracingBaggage;

    private static final String PROVIDER_KEY = "provider";

    @Builder
    private LogBaggage(UUID userId, UUID siteId, UUID userSiteId, String provider, ClientId clientId) {
        this.userId = userId;
        this.siteId = siteId;
        this.userSiteId = userSiteId;
        this.provider = provider;
        this.clientId = clientId;
        // Start, store state so we can 'restore' anything we manipulated.
        this.previousTracingBaggage = ExtraFieldPropagation.getAll();
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        this.previousMDC = copyOfContextMap != null ? copyOfContextMap : Collections.emptyMap();
        // End, store state.
        open();
    }

    public LogBaggage(PostgresUserSite userSite) {
        this(
                userSite.getUserId(),
                userSite.getSiteId(),
                userSite.getUserSiteId(),
                userSite.getProvider(),
                userSite.getClientId()
        );
    }

    private void open() {
        if (userId != null) {
            String value = userId.toString();
            MDC.put(MDCContextCreator.USER_ID_HEADER_NAME, value);
            ExtraFieldPropagation.set(MDCContextCreator.USER_ID_HEADER_NAME, value); // This is the 'propagation-key' that will be used by open-
            // tracing. Will also be put on headers for external calls and propagation to other threads.
            // Makes sure that it will be present on MDC in subsequent threads/services.

            // Temporarily we'll also set it manually on the MDC with key 'user_id'.
            // To make sure that it will also be on the MDC on the current thread.
            MDC.put(MDCContextCreator.USER_ID_MDC_KEY, value);
        }
        if (siteId != null) {
            String value = siteId.toString();
            ExtraFieldPropagation.set(MDCContextCreator.SITE_ID_MDC_KEY, value);
            MDC.put(MDCContextCreator.SITE_ID_MDC_KEY, value);
        }
        if (userSiteId != null) {
            String value = userSiteId.toString();
            ExtraFieldPropagation.set(MDCContextCreator.USER_SITE_ID_MDC_KEY, value);
            MDC.put(MDCContextCreator.USER_SITE_ID_MDC_KEY, value);
        }
        if (provider != null) {
            MDC.put(PROVIDER_KEY, provider);
        }
        if (clientId != null && clientId.unwrap() != null) {
            MDC.put(MDCContextCreator.CLIENT_ID_HEADER_NAME, clientId.unwrap().toString());
        }
    }

    @Override
    public void close() {
        // Restore the MDC + tracing baggage to how it was before this class was created.
        MDC.setContextMap(previousMDC);
        // It is a little bit strange, but we can't 'remove' the propagation fields that are put on the span.
        // We can restore overwritten values in this way though:
        previousTracingBaggage.forEach(ExtraFieldPropagation::set);
        // Unfortunately, that means that a span will have the baggage you set on it, until it dies/finishes.
    }
}
