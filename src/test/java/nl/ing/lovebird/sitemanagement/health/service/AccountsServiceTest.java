package nl.ing.lovebird.sitemanagement.health.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import lombok.SneakyThrows;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestClientTokens;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1.UserSiteDTOv1;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.health.repository.domain.Account;
import nl.ing.lovebird.sitemanagement.health.repository.domain.AccountGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1.AccountType.CURRENT_ACCOUNT;
import static nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1.AccountType.SAVINGS_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@IntegrationTestContext
public class AccountsServiceTest {

    private final UriComponentsBuilder accountsUriBuilder = UriComponentsBuilder.fromPath("/accounts-and-transactions/v1/users/{userId}/accounts");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountsServiceV1 accountsService;

    @Autowired
    TestClientTokens testClientTokens;

    @Test
    @SneakyThrows
    public void shouldReturnAccountsFromAccountsAndTransactions() {
        Clock clock = Clock.fixed(ZonedDateTime.of(2013, 3, 3, 3, 3, 3, 3, ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));

        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();
        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(UUID.randomUUID(), UUID.randomUUID(), userId);

        List<AccountDTOv1> accounts = List.of(
                AccountDTOv1.builder()
                        .id(new UUID(0, 1))
                        .userSite(new UserSiteDTOv1(userId, userSiteId))
                        .type(CURRENT_ACCOUNT)
                        .lastDataFetchTime(Optional.of(Instant.now(clock)))
                        .build(),
                AccountDTOv1.builder()
                        .id(new UUID(0, 2))
                        .userSite(new UserSiteDTOv1(userId, userSiteId))
                        .type(CURRENT_ACCOUNT)
                        .lastDataFetchTime(Optional.of(Instant.now(clock)))
                        .build());

        WireMock.stubFor(WireMock.get(urlEqualTo(accountsUriBuilder.buildAndExpand(userId).toUriString()))
                .willReturn(WireMock.ok()
                        .withBody(objectMapper.writeValueAsString(accounts))
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)));

        List<AccountDTOv1> accountsDTOs = accountsService.getAccounts(clientUserToken);
        assertThat(accountsDTOs).containsExactlyInAnyOrder(
                AccountDTOv1.builder()
                        .id(new UUID(0, 1))
                        .userSite(new UserSiteDTOv1(userId, userSiteId))
                        .type(CURRENT_ACCOUNT)
                        .lastDataFetchTime(Optional.of(Instant.now(clock)))
                        .build(),
                AccountDTOv1.builder()
                        .id(new UUID(0, 2))
                        .userSite(new UserSiteDTOv1(userId, userSiteId))
                        .type(CURRENT_ACCOUNT)
                        .lastDataFetchTime(Optional.of(Instant.now(clock)))
                        .build());
    }

    @Test
    @SneakyThrows
    public void shouldReturnAccountGroups() {
        Clock clock = Clock.fixed(ZonedDateTime.of(2013, 3, 3, 3, 3, 3, 3, ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));

        UUID userId = randomUUID();
        UUID userSiteId = randomUUID();

        List<AccountDTOv1> accounts = List.of(
                AccountDTOv1.builder()
                        .id(new UUID(0, 1))
                        .userSite(new UserSiteDTOv1(userSiteId, randomUUID()))
                        .type(SAVINGS_ACCOUNT)
                        .lastDataFetchTime(Optional.of(Instant.now(clock)))
                        .build(),
                AccountDTOv1.builder()
                        .id(new UUID(0, 2))
                        .userSite(new UserSiteDTOv1(userSiteId, randomUUID()))
                        .type(CURRENT_ACCOUNT)
                        .lastDataFetchTime(Optional.of(Instant.now(clock)))
                        .build());

        WireMock.stubFor(WireMock.get(urlEqualTo(accountsUriBuilder.buildAndExpand(userId).toUriString()))
                .willReturn(WireMock.ok()
                        .withBody(objectMapper.writeValueAsString(accounts))
                        .withHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE)));

        ClientUserToken clientUserToken = testClientTokens.createClientUserToken(UUID.randomUUID(), UUID.randomUUID(), userId);
        List<AccountGroup> accountGroups = accountsService.getAccountGroups(clientUserToken);
        assertThat(accountGroups).containsExactlyInAnyOrder(
                new AccountGroup(SAVINGS_ACCOUNT, Collections.singletonList(Account.builder()
                        .id(new UUID(0, 1))
                        .userSiteId(userSiteId)
                        .lastRefreshed(Date.from(Instant.now(clock)))
                        .updated(Date.from(Instant.now(clock)))
                        .build())),
                new AccountGroup(CURRENT_ACCOUNT, Collections.singletonList(Account.builder()
                        .id(new UUID(0, 2))
                        .userSiteId(userSiteId)
                        .lastRefreshed(Date.from(Instant.now(clock)))
                        .updated(Date.from(Instant.now(clock)))
                        .build())));
    }
}
