package nl.ing.lovebird.sitemanagement.provider;

public enum ProviderType {

    /**
     * The provider is a scraping data provider.
     */
    SCRAPING,

    /**
     * The provider is an API connection (PSD2, OpenBanking, ...)
     */
    DIRECT_CONNECTION
}
