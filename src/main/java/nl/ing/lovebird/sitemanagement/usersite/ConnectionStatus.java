package nl.ing.lovebird.sitemanagement.usersite;

import nl.ing.lovebird.sitemanagement.accessmeans.UserSiteAccessMeans;

public enum ConnectionStatus {
    /**
     * If a {@link PostgresUserSite} is in this status it means that we can 'talk to the bank'.  So:
     * - we have {@link UserSiteAccessMeans} in the database
     * - last time we requested data, it succeeded
     */
    CONNECTED,
    /**
     * The {@link UserSiteAccessMeans} are not present or the last time we tried to use them the bank informed us
     * that the {@link UserSiteAccessMeans} were no longer valid and had to be renewed.
     */
    DISCONNECTED,
    /**
     * The user is in the process of connecting the {@link PostgresUserSite} to the bank and has to complete a step to do so.
     */
    STEP_NEEDED
}
