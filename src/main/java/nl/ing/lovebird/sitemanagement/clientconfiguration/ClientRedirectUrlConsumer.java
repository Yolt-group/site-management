package nl.ing.lovebird.sitemanagement.clientconfiguration;

import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.clienttokens.ClientToken;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static nl.ing.lovebird.clienttokens.constants.ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME;
import static nl.ing.lovebird.sitemanagement.clientconfiguration.ClientRedirectUrlMessageType.*;

@Slf4j
@Component
class ClientRedirectUrlConsumer {

    private final ClientRedirectUrlService clientRedirectUrlService;

    public ClientRedirectUrlConsumer(ClientRedirectUrlService clientRedirectUrlService) {
        this.clientRedirectUrlService = clientRedirectUrlService;
    }

    @KafkaListener(topics = "${yolt.kafka.topics.clientRedirectUrls.topic-name}",
            concurrency = "${yolt.kafka.topics.clientRedirectUrls.listener-concurrency}")
    public void clientRedirectUrlUpdate(@Header(value = CLIENT_TOKEN_HEADER_NAME) final ClientToken clientToken,
                                        @Payload ClientRedirectUrlDTO clientRedirectUrlUpdateDTO,
                                        @Header("message_type") String messageType) {
        try {
            log.debug("Got update for client redirect url for client {} and redirect url id {}",
                    clientRedirectUrlUpdateDTO.getClientId(),
                    clientRedirectUrlUpdateDTO.getRedirectUrlId());

            processEvent(clientToken, clientRedirectUrlUpdateDTO, parse(messageType));
        } catch (Exception e) {
            log.error("Unexpected exception reading client applications update: {}", e.getMessage(), e);
        }
    }

    private ClientRedirectUrlMessageType parse(String payloadType) {
        // Handle JSON encoded strings. Remove when all pods on > 13.0.25
        // See: https://yolt.atlassian.net/browse/CHAP-145
        if (payloadType.length() > 1 && payloadType.startsWith("\"") && payloadType.endsWith("\"")) {
            payloadType = payloadType.substring(1, payloadType.length() - 1);
        }
        return ClientRedirectUrlMessageType.valueOf(payloadType);
    }

    private void processEvent(final ClientToken clientToken, final ClientRedirectUrlDTO clientRedirectUrlDTO, final ClientRedirectUrlMessageType messageType) {
        if (CLIENT_REDIRECT_URL_CREATED == messageType || CLIENT_REDIRECT_URL_UPDATED == messageType) {
            clientRedirectUrlService.save(
                    new ClientId(clientToken.getClientIdClaim()),
                    clientRedirectUrlDTO.getRedirectUrlId(),
                    clientRedirectUrlDTO.getUrl());
            log.info("Client redirect url {} for client {} and redirect url id {}.", CLIENT_REDIRECT_URL_CREATED == messageType ? "saved" : "updated", clientToken.getClientIdClaim(), clientRedirectUrlDTO.redirectUrlId);
        } else if (CLIENT_REDIRECT_URL_DELETED == messageType) {
            UUID redirectUrlId = clientRedirectUrlDTO.getRedirectUrlId();
            clientRedirectUrlService.delete(new ClientId(clientToken.getClientIdClaim()), redirectUrlId);
            log.info("Client redirect url deleted for client {} and redirect url id {}.", clientToken.getClientIdClaim(), clientRedirectUrlDTO.redirectUrlId);
        }
    }
}
