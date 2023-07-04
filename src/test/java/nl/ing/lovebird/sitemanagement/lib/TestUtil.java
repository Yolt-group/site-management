package nl.ing.lovebird.sitemanagement.lib;

import com.google.common.collect.ImmutableMap;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.providerdomain.AccountType;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.site.LoginRequirement;
import nl.ing.lovebird.sitemanagement.sites.Site;
import nl.ing.lovebird.sitemanagement.sites.SiteCreatorUtil;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TestUtil {

    public static final ClientId YOLT_APP_CLIENT_ID = ClientIds.YTS_CREDIT_SCORING_APP;

    public static final Map<ServiceType, List<LoginRequirement>> AIS_WITH_REDIRECT_STEPS = ImmutableMap.of(ServiceType.AIS, Collections.singletonList(LoginRequirement.REDIRECT));
    public static final Map<ServiceType, List<LoginRequirement>> AIS_WITH_FORM_STEPS = ImmutableMap.of(ServiceType.AIS, Collections.singletonList(LoginRequirement.FORM));

    public static final List<CountryCode> COUNTRY_CODES_GB = Collections.singletonList(CountryCode.GB);

    public static final List<AccountType> CURRENT_CREDIT_SAVINGS = List.of(AccountType.CURRENT_ACCOUNT, AccountType.CREDIT_CARD, AccountType.SAVINGS_ACCOUNT);

    private static final String YOLT_FR_PHONE_NUMBER = "+33772291002";
    private static final String YOLT_IT_PHONE_NUMBER = "+393460482104";
    private static final String YOLT_UK_PHONE_NUMBER = "+447704278064";


    public static ClientToken createClientToken(String serializedJwt) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder().setSkipAllDefaultValidators().setSkipSignatureVerification().build();
            JwtClaims jwtClaims = jwtConsumer.processToClaims(serializedJwt);
            return new ClientToken(serializedJwt, jwtClaims);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static Site createYoltProviderSite() {
        return SiteCreatorUtil.createTestSite("33aca8b9-281a-4259-8492-1b37706af6db", "YoltProvider", "YOLT_PROVIDER", List.of(AccountType.values()),  List.of(CountryCode.GB), AIS_WITH_REDIRECT_STEPS);
    }



}
