package nl.ing.lovebird.sitemanagement.site;

import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SiteCreatorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.lib.TestUtil.AIS_WITH_FORM_STEPS;
import static nl.ing.lovebird.sitemanagement.lib.TestUtil.COUNTRY_CODES_GB;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SiteDTOMapperTest {

    private final SiteDTOMapper siteDTOMapper = new SiteDTOMapper(
            "/some/icon/path/{siteId}.png",
            "/some/logo/path/{siteId}.png");

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }


    @Test
    void testCreateSiteDTO_primaryLabelAllAccountTypes() {
        final UUID siteId = UUID.randomUUID();

        final Site site = SiteCreatorUtil.createTestSite(siteId.toString(), "Lloyds", "YODLEE", Arrays.asList(AccountType.values()), COUNTRY_CODES_GB, AIS_WITH_FORM_STEPS);

        final SiteDTO siteDTO = siteDTOMapper.createSiteDTO(site);

        assertThat(siteDTO.getPrimaryLabel()).isEqualTo("Current accounts, Credit Cards, Savings, Prepaid, Pensions and Investments");
    }

    @Test
    void testCreateSiteDTO_primaryLabelTwoAccountTypes() {
        final UUID siteId = UUID.randomUUID();

        final Site site = SiteCreatorUtil.createTestSite(siteId.toString(), "Lloyds", "YODLEE", List.of(AccountType.CURRENT_ACCOUNT, AccountType.CREDIT_CARD), COUNTRY_CODES_GB, AIS_WITH_FORM_STEPS);

        final SiteDTO siteDTO = siteDTOMapper.createSiteDTO(site);

        assertThat(siteDTO.getPrimaryLabel()).isEqualTo("Current accounts and Credit Cards");
    }

    @Test
    void testCreateSiteDTO_primaryLabelTwoOtherAccountTypes() {
        final UUID siteId = UUID.randomUUID();

        final Site site = SiteCreatorUtil.createTestSite(siteId.toString(), "Lloyds", "YODLEE", List.of(AccountType.SAVINGS_ACCOUNT, AccountType.CREDIT_CARD), COUNTRY_CODES_GB, AIS_WITH_FORM_STEPS);

        final SiteDTO siteDTO = siteDTOMapper.createSiteDTO(site);

        assertThat(siteDTO.getPrimaryLabel()).isEqualTo("Credit Cards and Savings");
    }

    @Test
    void testCreateSiteDTO_primaryLabelIsOrderedByOrdinal() {
        final UUID siteId = UUID.randomUUID();

        final Site site = SiteCreatorUtil.createTestSite(siteId.toString(), "Lloyds", "YODLEE", List.of(AccountType.CREDIT_CARD, AccountType.SAVINGS_ACCOUNT), COUNTRY_CODES_GB, AIS_WITH_FORM_STEPS);

        final SiteDTO siteDTO = siteDTOMapper.createSiteDTO(site);

        assertThat(siteDTO.getPrimaryLabel()).isEqualTo("Credit Cards and Savings");
    }

    @Test
    void testCreateSiteDTO_primaryLabelOneAccountType() {
        final UUID siteId = UUID.randomUUID();

        final Site site = SiteCreatorUtil.createTestSite(siteId.toString(), "Lloyds", "YODLEE", List.of(AccountType.CURRENT_ACCOUNT), COUNTRY_CODES_GB, AIS_WITH_FORM_STEPS);

        final SiteDTO siteDTO = siteDTOMapper.createSiteDTO(site);

        assertThat(siteDTO.getPrimaryLabel()).isEqualTo("Current accounts");
    }

    @Test
    void testCreateSiteDTO_primaryLabelNoAccountTypes() {
        final UUID siteId = UUID.randomUUID();

        final Site site = SiteCreatorUtil.createTestSite(siteId.toString(), "Lloyds", "YODLEE", List.of(), COUNTRY_CODES_GB, AIS_WITH_FORM_STEPS);

        final SiteDTO siteDTO = siteDTOMapper.createSiteDTO(site);

        assertThat(siteDTO.getPrimaryLabel()).isEqualTo("No accounts");
    }

    @Test
    void testGetSitePath() {
        UUID siteId = UUID.randomUUID();
        final String sitePath = siteDTOMapper.getSitePath(siteId);

        assertThat(sitePath).isEqualTo("/sites/" + siteId);
    }

}
