package nl.ing.lovebird.sitemanagement.accountsandtransactions;

import lombok.NonNull;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.constants.ClientTokenConstants;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Component
public class AccountsAndTransactionsClient {
    private final WebClient webClient;

    public AccountsAndTransactionsClient(
            WebClient.Builder webClientBuilder,
            @Value("${lovebird.accountsAndTransactions.endpointBaseUrl}") String baseUrl) {
        webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public List<UserSiteTransactionStatusSummary> retrieveUserSiteTransactionStatusSummary(@NonNull ClientUserToken clientUserToken) {
        var userIdString = clientUserToken.getUserIdClaim().toString();
        return webClient.get()
                .uri("/internal/{userId}/user-site-transaction-status-summary", userIdString)
                .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UserSiteTransactionStatusSummary>>() {
                })
                .block(Duration.ofSeconds(2));
    }

    public List<AccountDTOv1> getAccounts(@NonNull ClientUserToken clientUserToken) {
        var userIdString = clientUserToken.getUserIdClaim();

        return webClient.get()
                .uri("/v1/users/{userId}/accounts", userIdString)
                .header(ClientTokenConstants.CLIENT_TOKEN_HEADER_NAME, clientUserToken.getSerialized())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<AccountDTOv1>>() {
                })
                .block(Duration.ofSeconds(2));
    }

}
