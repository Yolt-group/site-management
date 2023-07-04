package nl.ing.lovebird.sitemanagement.providerclient;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.exception.HttpException;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import nl.ing.lovebird.sitemanagement.providercallback.CallbackRequestDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
public class FormProviderRestClient {

    private final RestTemplate restTemplate;
    private final ErrorCodeExtractor errorCodeExtractor;
    private final String endpointBaseUrl;

    public FormProviderRestClient(RestTemplateBuilder builder, @Value("${lovebird.providers.endpointBaseUrl}") String endpointBaseUrl,
                                  @Value("${lovebird.providers.httpTimeoutFormProvidersInSeconds}") int httpTimeoutFormProvidersInSeconds,
                                  ErrorCodeExtractor errorCodeExtractor) {
        this.endpointBaseUrl = endpointBaseUrl;
        this.errorCodeExtractor = errorCodeExtractor;
        this.restTemplate = builder.setReadTimeout(Duration.ofSeconds(httpTimeoutFormProvidersInSeconds)).build();
    }

    public UUID fetchProviderExternalUserIds(final String provider, final ClientToken clientToken) throws HttpException {
        String url = String.format("%s/form/%s/external-user-ids", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientToken)
                .build();
        HttpEntity<Void> request = new HttpEntity<>(null, headers);
        try {
            String response = restTemplate.exchange(url, HttpMethod.GET, request, String.class).getBody();
            return StringUtils.isBlank(response) ? null : UUID.fromString(response);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public void deleteOrphanUserAtProvider(final String provider, final String externalUserId, ClientToken clientToken) throws HttpException {
        String url = String.format("%s/form/%s/external-user-ids/%s", endpointBaseUrl, provider, externalUserId);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientToken)
                .build();
        HttpEntity<Void> request = new HttpEntity<>(null, headers);
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public AccessMeansDTO accessMeansRefresh(
            String provider,
            FormRefreshAccessMeansDTO formRefreshAccessMeansDTO,
            ClientUserToken clientUserToken
    ) throws HttpException {
        String url = String.format("%s/form/%s/access-means/refresh", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .build();
        HttpEntity<FormRefreshAccessMeansDTO> request = new HttpEntity<>(formRefreshAccessMeansDTO, headers);
        try {
            ResponseEntity<AccessMeansDTO> response = restTemplate.postForEntity(url, request, AccessMeansDTO.class);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public void deleteUserSite(
            String provider,
            FormDeleteUserSiteDTO formDeleteUserSite,
            ClientUserToken clientUserToken,
            UUID siteId
    ) throws HttpException {
        log.info("Deleting user-site external-id={}", formDeleteUserSite.getUserSiteExternalId()); //NOSHERIFF

        String url = String.format("%s/form/%s/delete-user-site", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .siteId(siteId)
                .build();
        HttpEntity<FormDeleteUserSiteDTO> request = new HttpEntity<>(formDeleteUserSite, headers);
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public void deleteUser(String provider, FormDeleteUser formDeleteUser, ClientUserToken clientUserToken) throws HttpException {
        log.info("Deleting user-id={}", formDeleteUser.getAccessMeansDTO().getUserId());

        String url = String.format("%s/form/%s/delete-user", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .build();
        HttpEntity<FormDeleteUser> request = new HttpEntity<>(formDeleteUser, headers);
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public LoginFormResponseDTO fetchLoginForm(String provider, FormFetchLoginDTO formFetchLoginDTO, ClientUserToken clientUserToken, UUID siteId) throws HttpException {
        String url = String.format("%s/form/%s/fetch-login-form", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .siteId(siteId)
                .build();
        HttpEntity<FormFetchLoginDTO> request = new HttpEntity<>(formFetchLoginDTO, headers);

        try {
            ResponseEntity<LoginFormResponseDTO> loginFormResponseResponseEntity = restTemplate.postForEntity(url, request, LoginFormResponseDTO.class);
            return loginFormResponseResponseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public EncryptionDetailsDTO getEncryptionDetails(
            String provider,
            ClientToken clientToken
    ) throws HttpException {
        String url = String.format("%s/form/%s/get-encryption-details", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder().clientToken(clientToken).build();
        HttpEntity<FormGetEncryptionDetailsDTO> request = new HttpEntity<>(new FormGetEncryptionDetailsDTO(new ClientId(clientToken.getClientIdClaim())), headers);
        try {
            ResponseEntity<EncryptionDetailsDTO> encryptionDetails = restTemplate.postForEntity(url, request, EncryptionDetailsDTO.class);
            return encryptionDetails.getBody();
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }

    }

    public FormCreateNewUserResponseDTO createNewUser(
            String provider,
            FormCreateNewUserRequestDTO formCreateNewUserRequest,
            ClientUserToken clientUserToken
    ) throws HttpException {
        String url = String.format("%s/form/%s/create-new-user", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .siteId(formCreateNewUserRequest.getSiteId())
                .build();
        HttpEntity<FormCreateNewUserRequestDTO> request = new HttpEntity<>(formCreateNewUserRequest, headers);

        try {
            ResponseEntity<FormCreateNewUserResponseDTO> formCreateNewUserResponseDTOResponseEntity = restTemplate.postForEntity(url, request, FormCreateNewUserResponseDTO.class);
            return formCreateNewUserResponseDTOResponseEntity.getBody();
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }

    }

    public void updateExternalUserSite(
            String provider,
            FormUpdateExternalUserSiteDTO formUpdateExternalUserSite,
            ClientUserToken clientUserToken
    ) throws HttpException {
        String url = String.format("%s/form/%s/update-external-user-site", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .siteId(formUpdateExternalUserSite.getSiteId())
                .build();
        HttpEntity<FormUpdateExternalUserSiteDTO> request = new HttpEntity<>(formUpdateExternalUserSite, headers);
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public void createNewExternalUserSite(
            String provider,
            FormCreateNewExternalUserSiteDTO formCreateNewExternalUserSite,
            ClientUserToken clientUserToken
    ) throws HttpException {
        String url = String.format("%s/form/%s/create-new-external-user-site", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .siteId(formCreateNewExternalUserSite.getSiteId())
                .build();
        HttpEntity<FormCreateNewExternalUserSiteDTO> request = new HttpEntity<>(formCreateNewExternalUserSite, headers);

        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }

    }

    public void submitMfa(String provider,
                          FormSubmitMfaDTO formSubmitMfaDTO,
                          ClientUserToken clientUserToken
    ) throws HttpException {
        String url = String.format("%s/form/%s/submit-mfa", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .siteId(formSubmitMfaDTO.getSiteId())
                .build();
        HttpEntity<FormSubmitMfaDTO> request = new HttpEntity<>(formSubmitMfaDTO, headers);
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public void triggerRefreshAndFetchData(
            String provider,
            FormTriggerRefreshAndFetchDataDTO formTriggerRefreshAndFetchData,
            ClientUserToken clientUserToken
    ) throws HttpException {
        String url = String.format("%s/form/%s/trigger-refresh-and-fetch-data", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .siteId(formTriggerRefreshAndFetchData.getSiteId())
                .build();
        HttpEntity<FormTriggerRefreshAndFetchDataDTO> request = new HttpEntity<>(formTriggerRefreshAndFetchData, headers);
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

    public void processCallback(String provider,
                                CallbackRequestDTO callbackRequest,
                                ClientUserToken clientUserToken
    ) throws HttpException {
        String url = String.format("%s/form/%s/process-callback", endpointBaseUrl, provider);
        MultiValueMap<String, String> headers = new ProvidersHttpHeadersBuilder()
                .clientToken(clientUserToken)
                .build();
        HttpEntity<CallbackRequestDTO> request = new HttpEntity<>(callbackRequest, headers);
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (HttpStatusCodeException e) {
            throw new HttpException(e.getRawStatusCode(), errorCodeExtractor.getFunctionalErrorCode(e));
        }
    }

}
