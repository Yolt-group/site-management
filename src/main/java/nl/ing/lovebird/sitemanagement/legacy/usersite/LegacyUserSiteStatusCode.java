package nl.ing.lovebird.sitemanagement.legacy.usersite;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import nl.ing.lovebird.sitemanagement.usersite.CreateOrUpdateUserSiteService;

/**
 * Status transitions when a user triggers a refresh.  See {@link UserSiteRefreshService} for these transitions.
 */
/*
 *  INITIAL_PROCESSING +--> LOGIN_SUCCEEDED +--> REFRESH_FINISHED
 *                   +               +
 *                   |               |
 *                   |               |
 *                   v               v
 *              LOGIN_FAILED    REFRESH_FAILED
 */

/**
 * Status transitions when connecting a UserSite. See {@link CreateOrUpdateUserSiteService} for these transitions.
 */
/*
 * INITIAL_PROCESSING+--------------->LOGIN_SUCCEEDED
 *            +  +                     ^
 *            |  |       +-------+     |
 *            |  |       v       +     |
 *            |  +----> STEP_NEEDED +--+
 *            |                  +
 *            |                  +----+
 *            |                       v
 *            +---------------------> LOGIN_FAILED
 */
@Getter
@Schema(name = "UserSiteStatusCode", description = "Holds the status of a particular user-site.")
@Deprecated
public enum LegacyUserSiteStatusCode {

    INITIAL_PROCESSING("This is the initial state. The user-site was added with provider.", true, false, false),

    STEP_NEEDED("If this state comes back, a new step is needed in order to connect the user site", true, true, true),

    LOGIN_SUCCEEDED("We were able to log in to the site on behalf of the user.", true, false, false),
    LOGIN_FAILED("We were not able to log in to the site on behalf of the user. The reason might give you more information.", false, true, true),

    REFRESH_FINISHED("The data is fetched from the provider.", true, true, false),
    REFRESH_FAILED("Failed to refresh the account.", false, true, true),

    // <editor-fold desc="Deprecated status codes." defaultstate="collapsed">

    /**
     * @deprecated probably a good idea to always use a more specific code instead of this one, use e.g.: LOGIN_FAILED / STEP_FAILED / REFRESH_FAILED.
     */
    @Deprecated
    UNKNOWN("An unexpected error occurred.", false, true, true);

    // </editor-fold>

    final String description;
    final boolean isSuccessState;
    final boolean isExitState;
    final boolean isActionNeeded;

    LegacyUserSiteStatusCode(final String description, final boolean isSuccessState, final boolean isExitState, final boolean isActionNeeded) {
        this.description = description;
        this.isSuccessState = isSuccessState;
        this.isExitState = isExitState;
        this.isActionNeeded = isActionNeeded;
    }

    @Override
    public String toString() {
        return String.format("%s : %s", name(), description);
    }

}
