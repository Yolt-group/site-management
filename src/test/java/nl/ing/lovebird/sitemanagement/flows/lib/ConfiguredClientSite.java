package nl.ing.lovebird.sitemanagement.flows.lib;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSiteDTO;
import nl.ing.lovebird.sitemanagement.sites.Site;

import java.util.UUID;

/**
 * A Site that is enabled and ready for use by a client.
 */
@Getter
@RequiredArgsConstructor
public class ConfiguredClientSite {

    final ClientSiteDTO clientSite;
    final Site site;

    /**
     * The registered site.
     */
    public UUID getSiteId() {
        return site.getId();
    }

    public Site site() {
        return site;
    }

    /**
     * The unique identifier by which site-management knows {@link #getRedirectBaseUrlAIS()}.
     */
    public UUID getAISRedirectUrlId() {

        if (clientSite.getServices().getAis() == null) {
            throw new IllegalStateException();
        }
        return clientSite.getServices().getAis().getOnboarded().getRedirectUrlIds().get(0);
    }

    /**
     * The url of the client to which the bank will send a user after the user has successfully given consent.
     */
    public String getRedirectBaseUrlAIS() {
        return "https://localhost/ais";
    }

    /**
     * The url of the client to which the bank will send a user after the user has successfully given consent.
     */
    public String getRedirectBaseUrlPIS() {
        return "https://localhost/pis";
    }

}
