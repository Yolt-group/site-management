package nl.ing.lovebird.sitemanagement.providerresponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.providershared.callback.CallbackResponseDTO;
import nl.ing.lovebird.providershared.form.LoginSucceededDTO;
import nl.ing.lovebird.providershared.form.ProviderServiceMAFResponseDTO;
import nl.ing.lovebird.sitemanagement.exception.CallbackIdentifierNotKnownException;
import nl.ing.lovebird.sitemanagement.legacy.logging.LogBaggage;
import nl.ing.lovebird.sitemanagement.providercallback.ProviderCallbackAsyncService;
import nl.ing.lovebird.sitemanagement.providerclient.FetchDataResultDTO;
import nl.ing.lovebird.sitemanagement.providerclient.KafkaProducerService;
import nl.ing.lovebird.sitemanagement.providerclient.NoSupportedAccountDTO;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequest;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequestRepository;
import nl.ing.lovebird.sitemanagement.usersite.ConnectionStatus;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProviderServiceResponseConsumer {
    private static final String NO_PROVIDER_REQUEST_FOR_USER = "No provider request for user ";

    private final ProviderRequestRepository providerRequestRepository;
    private final GenericDataProviderResponseProcessor genericDataProviderResponseProcessor;
    private final ScrapingDataProviderResponseProcessor scrapingDataProviderResponseProcessor;
    private final KafkaProducerService kafkaProducerService;
    private final ProviderCallbackAsyncService providerCallbackAsyncService;
    private final ObjectMapper objectMapper;
    private final UserSiteService userSiteService;

    private String removeQuotesFromHeader(String header) {
        if (header == null) {
            return null;
        }
        // Handle JSON encoded strings. Remove when all pods on > 13.0.25
        // See: https://yolt.atlassian.net/browse/CHAP-145
        if (header.length() > 1 && header.startsWith("\"") && header.endsWith("\"")) {
            return header.substring(1, header.length() - 1);
        }
        return header;
    }

    @KafkaListener(topicPattern = "${yolt.kafka.topics.providerAccounts.topic-name}",
            concurrency = "${yolt.kafka.topics.providerAccounts.listener-concurrency}")
    public void providerAccountsMessage(
            ConsumerRecord<?, byte[]> record,
            @Header(value = "payload-type", required = false) String payloadType,
            @Header(value = CLIENT_TOKEN_HEADER_NAME) final ClientUserToken clientUserToken) {
        byte[] payload = record.value();

        payloadType = removeQuotesFromHeader(payloadType);
        try {
            if (ProviderMessageType.CALLBACK_RESPONSE.name().equals(payloadType)) {
                // Applicable only for: Budget Insight, Saltedge.
                Assert.notNull(clientUserToken, "client-token header required, but missing (or is invalid)");
                providerCallbackAsyncService.processCallback(deserializeValue(payload, CallbackResponseDTO.class));
            } else if (ProviderMessageType.MFA.name().equals(payloadType)) {
                // Applicable only for: Yodlee, Budget Insight.
                Assert.notNull(clientUserToken, "client-token header required, but missing (or is invalid)");
                processProviderServiceMAFResponseMessage(deserializeValue(payload, ProviderServiceMAFResponseDTO.class), clientUserToken);
            } else if (ProviderMessageType.LOGIN_SUCCEEDED.name().equals(payloadType)) {
                processLoginSucceededMessage(deserializeValue(payload, LoginSucceededDTO.class));
            } else if (ProviderMessageType.NO_SUPPORTED_ACCOUNTS.name().equals(payloadType)) {
                // Applicable for all providers.
                processNoSupportedAccountDTO(clientUserToken, deserializeValue(payload, NoSupportedAccountDTO.class));
            } else /* if (ProviderMessageType.PROVIDER_SERVICE_RESPONSE.name().equals(payloadType)) */ { //NOSONAR
                // Applicable only for: all providers.
                processProviderServiceResponseMessage(clientUserToken, deserializeValue(payload, FetchDataResultDTO.class));
            }
        } catch (RuntimeException e) {
            UUID correlationId = UUID.randomUUID();
            log.error("Unexpected exception reading provider accounts. The exception cannot be logged because it might contain sensitive " +
                    "data. Check the errortopic for more information. Correlation Id: {}", correlationId, e);
            kafkaProducerService.publishDataError(new String(payload, StandardCharsets.UTF_8), correlationId);
        } catch (CallbackIdentifierNotKnownException e) {
            log.warn("Callback external identifier not found", e);
        }
    }

    private void processLoginSucceededMessage(LoginSucceededDTO loginSucceededDTO) {
        PostgresUserSite userSite = userSiteService.getUserSite(loginSucceededDTO.getUserId(), loginSucceededDTO.getUserSiteId());
        userSiteService.updateUserSiteStatus(userSite, ConnectionStatus.CONNECTED, null, null);
    }

    private void processNoSupportedAccountDTO(ClientUserToken clientUserToken, NoSupportedAccountDTO noSupportedAccountDTO) {
        if (noSupportedAccountDTO.getProviderRequestId() != null) {
            // We know what we are doing.
            ProviderRequest providerRequest = providerRequestRepository.get(clientUserToken.getUserIdClaim(), noSupportedAccountDTO.getProviderRequestId())
                    .orElseThrow(() -> new IllegalStateException("Could not find provider request with id " + noSupportedAccountDTO.getProviderRequestId()));

            genericDataProviderResponseProcessor.processNoSupportedAccountsMessage(
                    noSupportedAccountDTO.getUserId(),
                    noSupportedAccountDTO.getUserSiteId(),
                    providerRequest.getUserSiteActionType());
        } else {
            // Fall back to the callback service. We 'lost' the associated providerRequest (or it was a spontaneous callback from a scraper).
            providerCallbackAsyncService.processNoSupportedAccountsFromCallback(noSupportedAccountDTO.getUserId(), noSupportedAccountDTO.getUserSiteId());
        }
    }

    private void processProviderServiceResponseMessage(final @NonNull ClientUserToken clientUserToken,
                                                       final @NonNull FetchDataResultDTO providerServiceResponseDTO) {

        ProviderRequest providerRequest = providerRequestRepository.get(clientUserToken.getUserIdClaim(), providerServiceResponseDTO.getProviderRequestId())
                .orElseThrow(() -> new IllegalStateException(NO_PROVIDER_REQUEST_FOR_USER + clientUserToken.getUserIdClaim()));

        Optional<PostgresUserSite> optionalUserSite = retrieveUserSite(providerRequest.getUserId(), providerRequest.getUserSiteId());
        try (LogBaggage b = LogBaggage.builder()
                .userId(providerRequest.getUserId())
                .userSiteId(providerRequest.getUserSiteId())
                .build()) {
            genericDataProviderResponseProcessor.process(
                    providerRequest.getUserSiteId(),
                    optionalUserSite,
                    providerServiceResponseDTO.getProviderServiceResponseStatus(),
                    providerRequest.getUserSiteActionType(),
                    providerRequest.getActivityId(),
                    clientUserToken);
        }
    }

    private void processProviderServiceMAFResponseMessage(
            ProviderServiceMAFResponseDTO providerServiceMAFResponseDTO,
            ClientUserToken clientUserToken
    ) {
        ProviderRequest providerRequest = providerRequestRepository.get(clientUserToken.getUserIdClaim(), providerServiceMAFResponseDTO.getProviderRequestId())
                .orElseThrow(() -> new IllegalStateException(NO_PROVIDER_REQUEST_FOR_USER + clientUserToken.getUserIdClaim()));
        scrapingDataProviderResponseProcessor.processMfaWithKnownCause(providerServiceMAFResponseDTO, providerRequest, clientUserToken);
    }

    private Optional<PostgresUserSite> retrieveUserSite(@NonNull UUID userId, @NonNull UUID userSiteId) {
        List<PostgresUserSite> userSites = userSiteService.getAllUserSitesIncludingDeletedOnes(userId);
        return userSites.stream()
                .filter(us -> userSiteId.equals(us.getUserSiteId()))
                .findAny();
    }

    private <T> T deserializeValue(final byte[] value, Class<T> clazz) {
        try {
            return objectMapper.readValue(value, clazz);
        } catch (IOException e) { //NOSONAR : this is not logged because it might contain sensitive data.
            throw new IllegalArgumentException("Unable to deserialize payload of string with length " + value.length + " to type " + clazz.getName());
        }
    }
}
