package nl.ing.lovebird.sitemanagement;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import nl.ing.lovebird.activityevents.events.RefreshedUserSiteEvent;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.site.SiteService;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.FailureReason;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * This class is responsible for exposing metrics to the {@link MeterRegistry}.
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings("squid:S1192") // String literals should not be duplicated
public class SiteManagementMetrics {

    private final Clock clock;
    private final MeterRegistry meterRegistry;
    private final SiteService siteService;

    /**
     * The SCR_ prefix indicates it's for scraping providers **only**.
     * The rest of the functions are used by both types of providers.
     */
    public enum ProvidersFunction {
        GET_STEP,
        DELETE_USER_SITE,
        CREATE_ACCESS_MEANS,
        RENEW_ACCESS_MEANS,
        FETCH_DATA,

        SCR_CREATE_USER,
        SCR_DELETE_USER,
        SCR_DELETE_ORPHAN_USER,
        SCR_CREATE_USER_SITE,
        SCR_UPDATE_USER_SITE,
        SCR_GET_EXT_USER_ID,
        SCR_GET_ENCRYPTION_DETAILS,
        SCR_CALLBACK
    }

    /**
     * Number of unhandled http errors while calling providers for {@link ProvidersFunction}.
     * <p>
     * Unhandled means we didn't get a functionalErrorCode in the {@link HttpException} that we could "handle", i.e.
     * it is not an expected flow.
     */
    public void incrementCounterUnhandledProvidersHttpError(
            @NonNull SiteManagementMetrics.ProvidersFunction function,
            @NonNull String provider,
            @NonNull HttpException unhandledException
    ) {
        meterRegistry.counter("providers_http_errors",
                "function", function.name().toLowerCase(),
                "provider", provider,
                "http_code", "" + unhandledException.getHttpStatusCode(),
                "functional_error_code", unhandledException.getFunctionalErrorCode() != null ? unhandledException.getFunctionalErrorCode() : "<none>"
        ).increment();
    }

    /**
     * Keeps track of successful or failed attempts at getting a first step in a flow.
     */
    public void incrementCounterGetFirstStepSuccessFailure(
            @NonNull Site site,
            boolean succeeded
    ) {
        meterRegistry.counter("site_activity_getloginstep",
                "success", succeeded + "",
                "provider", site.getProvider(),
                "site_id", site.getId().toString(),
                "site_name", site.getName()
        ).increment();
    }

    /**
     * Number of data fetches triggered.
     * <p>
     * This counter should always be >= {@link #incrementCounterFetchDataFinish}
     */
    public void incrementCounterFetchDataStart(
            UserSiteActionType actionType,
            PostgresUserSite userSite
    ) {
        final String siteName = siteService.getSiteName(userSite.getSiteId());
        meterRegistry.counter("site_activity_started",
                "provider", userSite.getProvider(),
                "site-id", userSite.getSiteId().toString(),
                "site-name", siteName,
                "initial-action-type", actionType.name()
        ).increment();
    }

    /**
     * Keep track of how many days of data we are request (per provider).  This is incremented once for every user-site
     * for which the system is about to request data.
     *
     * @param provider                      the provider name
     * @param fetchTransactionsStartInstant we are requesting transactions that happened on or after this point in time
     */
    public void registerNumberOfDaysFetched(@NonNull String provider, @NonNull Instant fetchTransactionsStartInstant) {
        var days = fetchTransactionsStartInstant.until(Instant.now(clock), ChronoUnit.DAYS);
        DistributionSummary.builder("site_trx_history_days_requested")
                .tags("provider", provider)
                .minimumExpectedValue(1d)
                .maximumExpectedValue(550d) // upperbound = 18 months
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(days);
    }

    /**
     * Number of data fetches that were completed.
     * <p>
     * This counter should always be <= {@link #incrementCounterFetchDataStart}
     */
    public void incrementCounterFetchDataFinish(
            UserSiteActionType actionType,
            PostgresUserSite userSite,
            RefreshedUserSiteEvent.Status refreshResult
    ) {
        final String siteName = siteService.getSiteName(userSite.getSiteId());
        meterRegistry.counter("site_activity_finished",
                "provider", userSite.getProvider(),
                "site-id", userSite.getSiteId().toString(),
                "site-name", siteName,
                "initial-action-type", actionType.name(),
                "fetch-data-result", refreshResult.name()
        ).increment();
    }

    /**
     * Number of data fetches that were completed.
     * <p>
     * This counter should always be <= {@link #incrementCounterFetchDataStart}
     */
    public void incrementCounterFetchDataFinishSuccess(
            UserSiteActionType actionType,
            PostgresUserSite userSite) {
        final String siteName = siteService.getSiteName(userSite.getSiteId());
        meterRegistry.counter("site_activity_finished",
                "provider", userSite.getProvider(),
                "site-id", userSite.getSiteId().toString(),
                "site-name", siteName,
                "initial-action-type", actionType.name(),
                "fetch-data-result", "OK"
        ).increment();
    }

    /**
     * Number of times that MFA is needed by user-site.
     */
    public void incrementCounterMfaNeeded(
            UserSiteActionType actionType,
            PostgresUserSite userSite
    ) {
        final String siteName = siteService.getSiteName(userSite.getSiteId());
        meterRegistry.counter("site_mfa_needed",
                "provider", userSite.getProvider(),
                "site-id", userSite.getSiteId().toString(),
                "site-name", siteName,
                "initial-action-type", actionType.name()
        ).increment();
    }

    @SneakyThrows
    public <T> T timeUserSitesRefresh(final Callable<T> callable) {
        return timer("user_sites_refresh_for_user", callable);
    }

    public void incrementUserSiteStatusUpdate(
            @NonNull String provider,
            @NonNull ConnectionStatus curStatus,
            FailureReason curReason,
            @NonNull ConnectionStatus newStatus,
            FailureReason newReason
    ) {
        String cur = curStatus.name() + (curReason != null ? "__" + curReason.name() : "");
        String new_ = newStatus.name() + (newReason != null ? "__" + newReason.name() : "");
        meterRegistry.counter("user_site_status_updates",
                "provider", provider,
                "cur", cur,
                "new", new_
        ).increment();
    }

    @SneakyThrows
    private <T> T timer(final String name, final Callable<T> callable) {
        return meterRegistry
                .timer(name)
                .recordCallable(callable);
    }

    /**
     * Counts the number of times an end-user has successfully completed the consent flow for
     * an API connected bank.
     */
    public void incrementCounterConsentSuccess(@NonNull String provider) {
        meterRegistry.counter("consent_success", "provider", provider).increment();
    }

    /**
     * Count the number of callbacks received from scraping providers and keep metrics of what we do with the
     * callbacks.
     */
    public void scrapingProviderCallback(@NonNull String provider, String result) {
        meterRegistry.counter("scraping_provider_callback", "provider", provider, "result", result).increment();
    }

    public void incrementDisconnectedUserSiteNotRefreshedFor90Days(long affectedNotRefreshedFor90Days) {
        meterRegistry.counter("user_site_disconnected_not_refreshed_90d").increment(affectedNotRefreshedFor90Days);
    }

}
