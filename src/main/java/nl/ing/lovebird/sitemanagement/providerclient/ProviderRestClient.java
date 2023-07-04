package nl.ing.lovebird.sitemanagement.providerclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.errorhandling.ErrorDTO;
import nl.ing.lovebird.providerdomain.ServiceType;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.sites.ProvidersSites;
import nl.ing.lovebird.sitemanagement.usersite.Step;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProviderRestClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ErrorCodeExtractor errorCodeExtractor;

    public ProviderRestClient(RestTemplateBuilder builder, @Value("${lovebird.providers.endpointBaseUrl}") String endpointBaseUrl,
                              ObjectMapper objectMapper, ErrorCodeExtractor errorCodeExtractor) {
        this.objectMapper = objectMapper;
        this.errorCodeExtractor = errorCodeExtractor;
        this.restTemplate = builder
                .rootUri(endpointBaseUrl)
                .setReadTimeout(Duration.ofSeconds(81)) // Match the read-timeout of provider plus some margin
                .build();
    }

    public AccessMeansDTO refreshAccessMeans(
            String provider,
            UUID siteId,
            RefreshAccessMeansDTO refreshAccessMeansDTO,
            ClientUserToken clientUserToken,
            boolean forceExperimentalVersion
    ) throws HttpException {
        var headers = new ProvidersHttpHeadersBuilder()
                .siteId(siteId)
                .clientToken(clientUserToken)
                .build();
        HttpEntity<RefreshAccessMeansDTO> request = new HttpEntity<>(refreshAccessMeansDTO, headers);
        try {
            Map<String, Object> parameters = createParameters(provider, forceExperimentalVersion);
            return restTemplate.postForEntity("/{provider}/access-means/refresh?forceExperimentalVersion={forceExperimentalVersion}", request, AccessMeansDTO.class, parameters).getBody();
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public AccessMeansOrStepDTO createNewAccessMeans(
            String provider,
            UUID siteId,
            ApiCreateAccessMeansDTO apiCreateAccessMeansDTO,
            ClientUserToken clientUserToken,
            boolean forceExperimentalVersion
    ) throws HttpException {
        UUID userId = apiCreateAccessMeansDTO.getUserId();
        var headers = new ProvidersHttpHeadersBuilder()
                .siteId(siteId)
                .clientToken(clientUserToken)
                .build();
        HttpEntity<ApiCreateAccessMeansDTO> request = new HttpEntity<>(apiCreateAccessMeansDTO, headers);
        try {
            Map<String, Object> parameters = createParameters(provider, forceExperimentalVersion);
            ResponseEntity<AccessMeansOrStepDTO> response = restTemplate.postForEntity("/v2/{provider}/access-means/create?forceExperimentalVersion={forceExperimentalVersion}", request, AccessMeansOrStepDTO.class, parameters);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public Step getLoginInfo(
            String provider,
            UUID siteId,
            ApiGetLoginDTO apiGetLoginDTO,
            ClientUserToken clientUserToken,
            boolean forceExperimentalVersion
    ) throws HttpException {
        var headers = new ProvidersHttpHeadersBuilder()
                .siteId(siteId)
                .clientToken(clientUserToken)
                .build();

        HttpEntity<ApiGetLoginDTO> request = new HttpEntity<>(apiGetLoginDTO, headers);
        try {
            Map<String, Object> parameters = createParameters(provider, forceExperimentalVersion);
            ResponseEntity<Step> response = restTemplate.postForEntity("/v2/{provider}/login-info?forceExperimentalVersion={forceExperimentalVersion}", request, Step.class, parameters);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public void fetchData(
            String provider,
            UUID siteId,
            ApiFetchDataDTO apiFetchDataDTO,
            ClientUserToken clientUserToken,
            boolean forceExperimentalVersion
    ) throws HttpException {
        var headers = new ProvidersHttpHeadersBuilder()
                .siteId(siteId)
                .clientToken(clientUserToken)
                .build();
        HttpEntity<ApiFetchDataDTO> request = new HttpEntity<>(apiFetchDataDTO, headers);
        try {
            Map<String, Object> parameters = createParameters(provider, forceExperimentalVersion);
            restTemplate.postForEntity("/{provider}/fetch-data?forceExperimentalVersion={forceExperimentalVersion}", request, Void.class, parameters);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public void notifyUserSiteDelete(String provider,
                                     ApiNotifyUserSiteDeleteDTO requestBody,
                                     ClientUserToken clientUserToken,
                                     boolean forceExperimentalVersion) throws HttpException {
        var headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .build();
        HttpEntity<ApiNotifyUserSiteDeleteDTO> request = new HttpEntity<>(requestBody, headers);
        try {
            Map<String, Object> parameters = createParameters(provider, forceExperimentalVersion);
            restTemplate.postForEntity("/{provider}/notify-user-site-delete?forceExperimentalVersion={forceExperimentalVersion}", request, Void.class, parameters);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    /**
     * Note: this method expected a {@link ClientToken} and not a {@link ClientUserToken}, this is an exception.  This operation isn't a user-specific operation.
     */
    public void invokeConsentTests(InvokeConsentTestingDTO invokeConsentTestingDTO, ClientToken clientToken, ServiceType serviceType) throws HttpException {
        var headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientToken)
                .build();
        HttpEntity<InvokeConsentTestingDTO> request = new HttpEntity<>(invokeConsentTestingDTO, headers);
        try {
            restTemplate.exchange(
                    "/clients/invoke-consent-tests?serviceType={serviceType}",
                    HttpMethod.POST,
                    request,
                    Void.class,
                    serviceType
            );
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public ProvidersSites getProvidersSites() throws HttpException {
        try {
            return restTemplate.getForEntity("/sites-details", ProvidersSites.class).getBody();
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), extractFunctionalErrorCode(e));
        }
    }

    private Map<String, Object> createParameters(String provider, boolean forceExperimentalVersion) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("provider", provider);
        parameters.put("forceExperimentalVersion", forceExperimentalVersion);
        return parameters;
    }

    private String extractFunctionalErrorCode(HttpStatusCodeException httpStatusCodeException) {
        try {
            return Optional.of(objectMapper.readValue(httpStatusCodeException.getResponseBodyAsString(), ErrorDTO.class))
                    .map(ErrorDTO::getCode)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

}
