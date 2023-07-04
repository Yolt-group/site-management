package nl.ing.lovebird.sitemanagement.externalconsent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import nl.ing.lovebird.sitemanagement.sites.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * This is used for testing purposes so we can set the consent expiry for the user under test in order to verify consent renewal works from Cucumber.
 */
@Component
public class ConsentExpiryStub {

    private static final UUID ID_YOLTBANK_OPENBANKING_STUBBED = UUID.fromString("ca8a362a-a351-4358-9f1c-a8b4b91ed65b");
    private final RestTemplate restTemplate;
    private final String stubsBaseUrl;

    @Autowired
    public ConsentExpiryStub(final ObjectMapper objectMapper) {
        this(objectMapper, "https://stubs");
    }

    public ConsentExpiryStub(final ObjectMapper objectMapper, String stubsBaseUrl) {
        this.restTemplate = new RestTemplateBuilder()
                .messageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        this.stubsBaseUrl = stubsBaseUrl;
    }

    int consentExpiryForUser(UUID userId, Site site) {
        if (site.getId().equals(ID_YOLTBANK_OPENBANKING_STUBBED)) {
            final StubConsentExpiry stubConsentExpiry = restTemplate.getForObject(stubsBaseUrl + "/stubs/site-consent-expiries/users/{userId}/sites/{siteId}", StubConsentExpiry.class, userId, site.getId());
            return stubConsentExpiry.getExpiryInDays();
        } else {
            return site.getConsentExpiryInDays();
        }
    }

    @Data
    public static class StubConsentExpiry {
        private int expiryInDays = 90;
    }
}
