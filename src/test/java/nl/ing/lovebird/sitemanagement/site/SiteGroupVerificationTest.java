package nl.ing.lovebird.sitemanagement.site;

import com.google.common.collect.Sets;
import info.debatty.java.stringsimilarity.Damerau;
import info.debatty.java.stringsimilarity.JaroWinkler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;

import nl.ing.lovebird.sitemanagement.lib.CountryCode;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SiteCreatorUtil;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@IntegrationTestContext
@Slf4j
public class SiteGroupVerificationTest {

    private static final List<String> FALSE_POSITIVES_IGNORE_LIST = List.of("Saltedge Fake Bank with Error", "Saltedge Fake Bank with SMS"
            , "Saltedge Fake Bank with Error", "Saltedge Fake Bank with Token"
            , "Yodlee test bank", "Yolt test bank"
            , "Saltedge Fake Bank with Select", "Saltedge Fake Bank with Token"
            , "Budget Insight Testbank 1", "Budget Insight Testbank 2"
            , "Saltedge Fake Bank with Updates", "Saltedge Fake Bank with Token"
            , "Saltedge Fake Bank with SMS", "Saltedge Fake Bank with Token"
            , "Scen. 2: FR Bank Migration Success Remainder still on scraping all account types"
            , "Scen. 2: FR Bank Migration Success Remainder still on scraping direct connection"
            , "Scen. 5: FR Bank Migration Partial migration (noLongerSupported)"
            , "Barclaycard"
            , "Revolut"
    );

    // There's a need to decide where's the border between "long" strings and "short" strings.
    // This is because higher similarity value is harder to achieve for shorter strings, and still lower value
    // might indicate that there's a problem
    private static final int STRING_LENGTH_BOUNDARY = 5;
    // An assumption - if there's more than 2 characters difference in length, it's less likely to be a typo
    private static final int RELEVANT_LENGTH_DIFFERENCE = 2;
    // limit of similarity for short strings
    private static final double SHORT_STRING_SIMILARITY_LEVEL = 0.85;
    // limit of similarity for long strings
    private static final double LONG_STRING_SIMILARITY_LEVEL = 0.90;
    // double check limit - just to be more sure
    private static final double DOUBLE_CHECK_SIMILARITY_LEVEL = 0.8;
    // similarity limit for values in brackets, e.g. Some bank (FR),  Some Bank (AUS) - only values in brackets should be checked
    private static final double BRACKETS_SIMILARITY_LEVEL = 0.8;

    // two algorithms chosen to check similarity
    private final Damerau DAMERAU = new Damerau();
    private final JaroWinkler JARO_WINKLER = new JaroWinkler();

    @Autowired
    private SitesProvider sitesProvider;

    @Test
    void shouldDetectIssuesBecauseOfTypos() {
        Site s1 = generateSite("My test bank", Collections.singletonList(CountryCode.NL));
        Site s2 = generateSite("My test bnak", Collections.singletonList(CountryCode.NL));

        Site s3 = generateSite("Crédit Agricole", Collections.singletonList(CountryCode.NL));
        Site s4 = generateSite("Credit Agricole", Collections.singletonList(CountryCode.NL));

        assertThat(shouldSkipChecking(s1, s2)).isFalse();
        assertThat(shouldSkipChecking(s3, s4)).isFalse();

        Optional<SuspiciousSitePair> suspiciousSitePair1 = detectPossibleIssues(s1, s2);
        Optional<SuspiciousSitePair> suspiciousSitePair2 = detectPossibleIssues(s3, s4);

        assertThat(suspiciousSitePair1.isPresent()).isTrue();
        assertThat(suspiciousSitePair2.isPresent()).isTrue();
    }

    @Test
    void shouldSkipCheckingBecauseOfSameGroup() {
        Site s1 = SiteCreatorUtil.createTestSite(UUID.randomUUID(), "My test bank", "YOLT_PROVIDER", List.of(AccountType.values()),
                List.of(CountryCode.NL), Map.of(ServiceType.AIS, List.of(LoginRequirement.REDIRECT)),  "MTB",  null, null, null);
        Site s2 = SiteCreatorUtil.createTestSite(UUID.randomUUID(), "My test bnak", "YOLT_PROVIDER", List.of(AccountType.values()),
                List.of(CountryCode.NL), Map.of(ServiceType.AIS, List.of(LoginRequirement.REDIRECT)),  "MTB",  null, null, null);
        assertThat(shouldSkipChecking(s1, s2)).isTrue();
    }

    @Test
    void shouldSkipCheckingBecauseOfSignificantDifferenceOfBracketsValues() {
        Site s1 = generateSite("MyTestBank (GB)", List.of(CountryCode.NL, CountryCode.GB));
        Site s2 = generateSite("MyTestBank (NL)", Collections.singletonList(CountryCode.NL));
        assertThat(shouldSkipChecking(s1, s2)).isTrue();
    }

    @Test
    void shouldSkipCheckingBecauseOfSignificantLengthDifference() {
        Site s1 = generateSite("Bank", Collections.singletonList(CountryCode.NL));
        Site s2 = generateSite("INGBank", Collections.singletonList(CountryCode.NL));
        assertThat(shouldSkipChecking(s1, s2)).isTrue();
    }

    @Test
    void shouldSkipBecauseNoCountriesOverlap() {
        Site s1 = generateSite("My test bank", Collections.singletonList(CountryCode.FR));
        Site s2 = generateSite("My test bnak", Collections.singletonList(CountryCode.NL));
        assertThat(shouldSkipChecking(s1, s2)).isTrue();
    }

    // This test is for detecting possibly missed grouping_by in sites_v2 in order to prevent
    // bug that could affect end users
    @Test
    @Disabled("This test failed intermittently in the pipeline and does not work locally")
    void findPossiblyUngroupedSites() {
        List<Site> sites = sitesProvider.allSites();

        // List must not be empty so that we don't get false positives
        assertThat(sites.size()).isGreaterThan(0);

        List<SuspiciousSitePair> possibleFails = new ArrayList<>();
        List<SuspiciousSitePair> fails = new ArrayList<>();

        // checking SiteEntities against each other
        for (int i = 0; i < sites.size(); i++) {
            for (int j = i; j < sites.size(); j++) {
                if (!shouldSkipChecking(sites.get(i), sites.get(j))) {
                    Optional<SuspiciousSitePair> suspiciousSitePair = detectPossibleIssues(sites.get(i), sites.get(j));
                    suspiciousSitePair.ifPresent(possibleFails::add);
                }
            }
        }
        for (SuspiciousSitePair possibleFail : possibleFails) {
            // using Damerau algorithm to double check if it's really a fail
            log.warn("{} {} and {} {} - possible ({}) fail to double check", possibleFail.getSite1().getId(), possibleFail.getSite1().getName(), possibleFail.getSite2().getId(), possibleFail.getSite2().getName(), possibleFail.getScore());
            if (DAMERAU.similarity(possibleFail.getSite1().getName(), possibleFail.getSite2().getName()) > DOUBLE_CHECK_SIMILARITY_LEVEL) {
                fails.add(possibleFail);
            }
        }
        List<SuspiciousSitePair> filteredErrorPairs = fails.stream()
                .filter(it -> !FALSE_POSITIVES_IGNORE_LIST.contains(it.getSite1().getName()) && !FALSE_POSITIVES_IGNORE_LIST.contains(it.getSite2().getName()))
                .collect(Collectors.toList());

        for (SuspiciousSitePair fail : filteredErrorPairs) {
            log.error("{} {}   &   {} {}  ", fail.getSite1().getId(), fail.getSite1().getName(), fail.getSite2().getId(), fail.getSite2().getName());
        }
        assertThat(filteredErrorPairs.size()).isEqualTo(0);
    }

    // Checking for possible issues for long and short strings
    private Optional<SuspiciousSitePair> detectPossibleIssues(final Site s1, final Site s2) {
        double score = JARO_WINKLER.similarity(s1.getName(), s2.getName());
        if (s1.getName().length() <= STRING_LENGTH_BOUNDARY && score >= SHORT_STRING_SIMILARITY_LEVEL) {
            return Optional.of(new SuspiciousSitePair(s1, s2, score));
        } else if (s1.getName().length() > STRING_LENGTH_BOUNDARY && score >= LONG_STRING_SIMILARITY_LEVEL) {
            return Optional.of(new SuspiciousSitePair(s1, s2, score));
        }
        return Optional.empty();
    }

    private boolean shouldSkipChecking(final Site s1, final Site s2) {
        return s1.equals(s2) ||
                groupingMatches(s1, s2) ||
                noCountriesOverlap(s1, s2) ||
                isLengthDiffRelevant(s1, s2) ||
                onlyBracketsValueDiffersInsignificantly(s1, s2); // TODO: get rid of this when there's no more (NL)-like suffixes
    }

    // checking similiarity of bracket values - whole strings might be very similar but bracket values could
    // be completely different which means that it's less likely to be a problem
    private boolean onlyBracketsValueDiffersInsignificantly(final Site s1, final Site s2) {
        if (s1.getName().indexOf('(') != -1 &&
                s2.getName().indexOf('(') != -1 &&
                s1.getName().substring(0, s1.getName().indexOf('(')).equals(s2.getName().substring(0, s2.getName().indexOf('(')))
        ) {
            String s1bracketValue = s1.getName().substring(s1.getName().indexOf('(') + 1, s1.getName().indexOf(')'));
            String s2bracketValue = s2.getName().substring(s2.getName().indexOf('(') + 1, s2.getName().indexOf(')'));
            return JARO_WINKLER.similarity(s1bracketValue, s2bracketValue) < BRACKETS_SIMILARITY_LEVEL;
        }
        return false;
    }

    // if length difference is greater than 2, it's less likely that it's actual typo
    private boolean isLengthDiffRelevant(final Site s1, final Site s2) {
        return Math.abs(s1.getName().length() - s2.getName().length()) > RELEVANT_LENGTH_DIFFERENCE;
    }

    // there's no need for grouping if providers are in different countries
    private boolean noCountriesOverlap(final Site s1, Site s2) {
        if (s1.getAvailableInCountries() == null || s2.getAvailableInCountries() == null) {
            return true;
        }
        return Sets.intersection(new HashSet<>(s1.getAvailableInCountries()), new HashSet<>(s2.getAvailableInCountries())).size() == 0;
    }

    // there's no point to do the checking when both sites have same groupingBy
    private boolean groupingMatches(final Site s1, final Site s2) {
        return s1.getGroupingBy() != null && s2.getGroupingBy() != null && s1.getGroupingBy().equals(s2.getGroupingBy());
    }

    private Site generateSite(final String name, final List<CountryCode> countryCodes) {
        return SiteCreatorUtil.createTestSite(UUID.randomUUID().toString(), name,  "YOLT_PROVIDER", List.of(AccountType.values()),
                countryCodes, Map.of(ServiceType.AIS, List.of(LoginRequirement.REDIRECT)));
    }
}

@Data
@AllArgsConstructor
class SuspiciousSitePair {
    private Site site1;
    private Site site2;
    private double score;
}
