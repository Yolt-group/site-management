package nl.ing.lovebird.sitemanagement.site;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.sitemanagement.legacy.HateoasUtils;
import nl.ing.lovebird.sitemanagement.legacy.sites.sitewithcountry.SiteWithCountryController;
import nl.ing.lovebird.sitemanagement.legacy.usersite.SiteLoginController;
import nl.ing.lovebird.sitemanagement.site.SiteDTO.LinkDTO;
import nl.ing.lovebird.sitemanagement.sites.Site;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static nl.ing.lovebird.sitemanagement.usersite.UserSiteDerivedAttributes.isScrapingSite;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Slf4j
@Component
public class SiteDTOMapper {

    private final String siteIconLink;

    private final String siteLogoLink;

    public SiteDTOMapper(@Value("${route.content.site-icons}") final String siteIconLink,
                         @Value("${route.content.site-logos}") final String siteLogoLink) {
        this.siteIconLink = siteIconLink;
        this.siteLogoLink = siteLogoLink;
    }

    private static final Map<AccountType, String> BACKWARD_COMPATIBILITY_PRIMARY_LABEL_MAP = new EnumMap<>(AccountType.class);

    static {
        BACKWARD_COMPATIBILITY_PRIMARY_LABEL_MAP.put(AccountType.CURRENT_ACCOUNT, "Current accounts");
        BACKWARD_COMPATIBILITY_PRIMARY_LABEL_MAP.put(AccountType.SAVINGS_ACCOUNT, "Savings");
        BACKWARD_COMPATIBILITY_PRIMARY_LABEL_MAP.put(AccountType.CREDIT_CARD, "Credit Cards");
        BACKWARD_COMPATIBILITY_PRIMARY_LABEL_MAP.put(AccountType.PENSION, "Pensions");
        BACKWARD_COMPATIBILITY_PRIMARY_LABEL_MAP.put(AccountType.PREPAID_ACCOUNT, "Prepaid");
        BACKWARD_COMPATIBILITY_PRIMARY_LABEL_MAP.put(AccountType.INVESTMENT, "Investments");
    }


    public SiteDTO createSiteDTO(final Site site) {

        final SiteDTO.SiteLinksDTO siteLinksDTO = getSiteLinksDTO(site.getId());

        return new SiteDTO(
                site.getId(),
                site.getName(),
                constructPrimaryLabel(site.getAccountTypeWhitelist()),
                site.getAccountTypeWhitelist(),
                isScrapingSite(site.getProvider()) ? LoginType.FORM : LoginType.URL,
                isScrapingSite(site.getProvider()) ? ConnectionType.SCRAPER : ConnectionType.DIRECT_CONNECTION,
                site.getServices(),
                siteLinksDTO,
                site.getGroupingBy(),
                site.getUsesStepTypes(),
                site.getAvailableInCountries(),
                SiteConnectionHealthStatus.SITE_CONNECTION_HEALH_STATUS_NOT_AVAILABLE,
                false);
    }

    public SiteDTO.SiteLinksDTO getSiteLinksDTO(final UUID siteId) {
        return new SiteDTO.SiteLinksDTO(
                linkDto(getSitePath(siteId)),
                linkDto(getLogoPath(siteId)),
                linkDto(getIconPath(siteId)),
                linkDto(initiateUserSitePath(siteId)));
    }

    private LinkDTO linkDto(String path) {
        return new LinkDTO(path);
    }


    public static String constructPrimaryLabel(final List<AccountType> whiteListedAccountTypes) {
        if (whiteListedAccountTypes.isEmpty()) {
            return "No accounts";
        }

        final String label = whiteListedAccountTypes.stream()
                .sorted(Comparator.comparingInt(AccountType::ordinal))
                .map(BACKWARD_COMPATIBILITY_PRIMARY_LABEL_MAP::get)
                .collect(Collectors.joining(", "));

        final int lastComma = label.lastIndexOf(',');

        if (lastComma > -1) {
            return label.substring(0, lastComma) + label.substring(lastComma).replace(",", " and");
        }

        return label;
    }

    public String getLogoPath(final UUID siteId) {
        return siteLogoLink.replace("{siteId}", siteId.toString());
    }

    public String getIconPath(final UUID siteId) {
        return siteIconLink.replace("{siteId}", siteId.toString());
    }

    private String initiateUserSitePath(UUID siteId) {
        return HateoasUtils.makePath(linkTo(methodOn(SiteLoginController.class)
                .initiateUserSite(siteId, null, null, null)
        ).toUri().toString());
    }

    public String getSitePath(final UUID siteId) {
        return HateoasUtils.createPath(methodOn(SiteWithCountryController.class).getSite(siteId, null));
    }
}
