package nl.ing.lovebird.sitemanagement.users;

public enum StatusType {
    ACTIVE,
    /**
     * app related. yts-users are never blocked.
     */
    @Deprecated
    BLOCKED,
    UNKNOWN;

}
