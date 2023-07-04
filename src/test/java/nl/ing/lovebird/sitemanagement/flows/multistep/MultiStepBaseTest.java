package nl.ing.lovebird.sitemanagement.flows.multistep;

import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.Getter;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlService;
import nl.ing.lovebird.sitemanagement.clientconfiguration.ClientSitesProvider;
import nl.ing.lovebird.sitemanagement.flows.lib.FauxProvidersService;
import nl.ing.lovebird.sitemanagement.flows.lib.LoginResponseDTOAssertions;
import nl.ing.lovebird.sitemanagement.flows.lib.LoginStepV1DTOAssertions;
import nl.ing.lovebird.sitemanagement.flows.lib.OnboardedSiteContext;
import nl.ing.lovebird.sitemanagement.sites.SitesProvider;
import nl.ing.lovebird.sitemanagement.usersite.FilledInFormValueDTO;
import nl.ing.lovebird.sitemanagement.usersite.FormLoginDTO;
import nl.ing.lovebird.sitemanagement.usersite.LoginResponseDTO;
import nl.ing.lovebird.sitemanagement.usersite.LoginStepV1DTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.net.URI;
import java.time.Clock;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.RequestEntity.post;

public class MultiStepBaseTest {
    @Autowired
    protected Clock clock;

    @Autowired
    protected TestRestTemplate restTemplate;
    @Autowired
    @SuppressWarnings("rawtypes")
    protected KafkaTemplate kafkaTemplate;

    @Autowired
    protected SitesProvider sitesProvider;

    @Autowired
    protected ClientSitesProvider clientSitesProvider;

    @Autowired
    protected ClientRedirectUrlService clientRedirectUrlService;

    @Getter
    @Autowired
    protected WireMockServer wireMockServer;

    @Autowired
    protected TestClientTokens testClientTokens;

    protected OnboardedSiteContext flowContext = new OnboardedSiteContext();

    protected LoginStepV1DTO postConnect(String provider) {
        // site-management will make a call to providers to retrieve the login information for the specific bank (providers will construct a form)
        FauxProvidersService.setupPostLoginStubForFormStep(wireMockServer, provider, FauxProvidersService.creditAgricoleRegionSelectionForm());

        ResponseEntity<LoginStepV1DTO> resp = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/connect?site=%s&redirectUrlId=%s",
                        flowContext.user().getId(),
                        flowContext.clientSite().getSiteId(),
                        flowContext.clientSite().getAISRedirectUrlId()
                )))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .build(), LoginStepV1DTO.class);

        // Check the validity of the response.
        assertThat(resp.getStatusCode()).isEqualTo(OK);
        LoginStepV1DTO dto = resp.getBody();
        LoginStepV1DTOAssertions.assertIsFormStep(dto);

        return dto;
    }

    protected LoginResponseDTO postRegionForm(String provider, String stateId, Map<String, String> filledInFormValues) {
        // site-management will make a call to providers to post the form, we make providers respond with a RedirectStep.
        FauxProvidersService.setupPostAccessMeansCreateStubForRedirectStep(wireMockServer, provider,
                (baseClientRedirectUrl, state) -> "https://credit-agricole.example.com?redirect_uri=" + baseClientRedirectUrl + "&state=" + state,
                // Matches only if this condition is met.
                requestDto -> requestDto.getFilledInUserSiteFormValues() != null && requestDto.getFilledInUserSiteFormValues().getValueMap().get("region") != null
        );

        FormLoginDTO body = new FormLoginDTO();
        body.setStateId(stateId);
        body.setFilledInFormValues(filledInFormValues.entrySet().stream().map(e -> {
            var v = new FilledInFormValueDTO();
            v.setFieldId(e.getKey());
            v.setValue(e.getValue());
            return v;
        }).collect(Collectors.toList()));

        ResponseEntity<LoginResponseDTO> resp = restTemplate.exchange(
                post(URI.create(format("/v1/users/%s/user-sites", flowContext.user().getId())))
                        .headers(flowContext.httpHeadersForClientAndUser())
                        .header("PSU-IP-Address", flowContext.user().getPsuIpAddress())
                        .body(body), LoginResponseDTO.class);

        // Check the validity of the response.
        assertThat(resp.getStatusCode()).isEqualTo(OK);
        LoginResponseDTO dto = resp.getBody();
        LoginResponseDTOAssertions.assertRedirectStepResponse(dto);

        return dto;
    }
}
