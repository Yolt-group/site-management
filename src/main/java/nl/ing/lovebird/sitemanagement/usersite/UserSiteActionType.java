package nl.ing.lovebird.sitemanagement.usersite;

public enum UserSiteActionType {

    CREATE_USER_SITE,
    UPDATE_USER_SITE,
    /**
     * @deprecated Should not be used. We now don't see this is a seperate actoin/session/flow anymore. It's part of either a CREATE/UPDATE/RERESH
     */
    @Deprecated
    SUBMIT_MFA,
    USER_REFRESH,
    FLYWHEEL_REFRESH,
    PROVIDER_FLYWHEEL_REFRESH,
    /**
     * @deprecated This is not used anymore.
     * Still here so we can still deserialize it for values that are already in the database.
     * Be aware (in switch-cases mainly) that this statuscode is still present in the database. This value was used when callback data
     * arrived from the provider that was actually due to a CREATE/UPDATE/REFRESH
     */
    @Deprecated
    PROVIDER_CALLBACK

}
