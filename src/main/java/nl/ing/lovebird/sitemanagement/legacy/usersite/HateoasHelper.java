package nl.ing.lovebird.sitemanagement.legacy.usersite;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.legacy.HateoasUtils;
import nl.ing.lovebird.sitemanagement.legacy.sites.sitewithcountry.SiteWithCountryController;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


/**
 *
 * This Utility class is used by several controllers to workout HATEOAS links pointing to these Controllers' end points
 *
 */

@Slf4j
@Deprecated
class HateoasHelper {

    private HateoasHelper() {

    }

    /**
     * add all the HATEOAS links to an existing userSiteDTO
     */
    public static LegacyUserSiteDTO enrichWithHateoasLinks(LegacyUserSiteDTO legacyUserSiteDTO) {

        final UUID siteId = legacyUserSiteDTO.getSiteId();
        final UUID userSiteId = legacyUserSiteDTO.getId();

        final
        LegacyUserSiteDTO.UserSiteLinksDTO userSiteLinksDTO = new LegacyUserSiteDTO.UserSiteLinksDTO(
                HateoasUtils.createPath(methodOn(SiteWithCountryController.class).getSite(siteId, null)),
                getRefreshPath(legacyUserSiteDTO.getId()),
                getDeletePath(legacyUserSiteDTO.getId()),
                "",
                getRenewConsentPath(legacyUserSiteDTO.getId()),
                getNextStepPath(userSiteId)
        );
        legacyUserSiteDTO.setLinks(userSiteLinksDTO);

        return legacyUserSiteDTO;

    }


    private static String getNextStepPath(UUID userSiteId) {
        return HateoasUtils.createPath(methodOn(UserSiteController.class).getNextStep(userSiteId, null));
    }

    private static String getRenewConsentPath(UUID userSiteId) {
        return HateoasUtils.createPath(methodOn(UserSiteController.class).renewAccess(
                userSiteId, null, null, null
        ));

    }

    private static String getRefreshPath(final UUID userSiteId) {
        final String path = HateoasUtils.createPath(methodOn(UserSiteController.class).refreshUserSite(
                userSiteId, null, null, null
        ));
        log.debug("Path to refresh: {}", path);
        return path;
    }

    private static String getDeletePath(final UUID userSiteId) {
        final String path = HateoasUtils.createPath(
                methodOn(UserSiteController.class).deleteUserSite(userSiteId, null, null));
        log.debug("Path to delete: {}", path);
        return path;
    }

}
