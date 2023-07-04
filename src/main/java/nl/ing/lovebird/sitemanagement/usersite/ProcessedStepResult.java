package nl.ing.lovebird.sitemanagement.usersite;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.UUID;

/**
 * {@link #userSiteId} is always non-null.
 * The fields {@link #activityId} and {@link #step} are mutually exclusive, exactly 1 is filled.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessedStepResult {

    UUID userSiteId;
    UUID activityId;
    Step step;

    /**
     * User has to complete another step.
     */
    public static ProcessedStepResult step(@NonNull UUID userSiteId, @NonNull Step step) {
        return new ProcessedStepResult(userSiteId, null, step);
    }

    /**
     * UserSite has been updated, we've kicked off a create/update operation that results in a data fetch.
     *
     * Return the corresponding activityId.
     */
    public static ProcessedStepResult activity(@NonNull UUID userSiteId, @NonNull UUID activityId) {
        return new ProcessedStepResult(userSiteId, activityId, null);
    }

    /**
     * Functional error during processing of the {@link Step}.
     *
     * Contract: the {@link PostgresUserSite} **must** be in status LOGIN_FAILED.
     */
    public static ProcessedStepResult loginFailed(@NonNull UUID userSiteId) {
        return new ProcessedStepResult(userSiteId, null, null);
    }

    /**
     * UserSite accessmeans were retrieved but we got an internal server error causing us to not start an activity.
     * Only return the created user-site-id.
     */
    public static ProcessedStepResult noActivity(@NonNull UUID userSiteId) {
        return new ProcessedStepResult(userSiteId, null, null);
    }
}
