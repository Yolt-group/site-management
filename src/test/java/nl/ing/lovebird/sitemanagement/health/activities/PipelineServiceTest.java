package nl.ing.lovebird.sitemanagement.health.activities;

import nl.ing.lovebird.activityevents.events.RefreshUserSitesEvent;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.clienttokens.test.TestJwtClaims;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1;
import nl.ing.lovebird.sitemanagement.accountsandtransactions.dtos.AccountDTOv1.AccountType;
import nl.ing.lovebird.sitemanagement.health.dspipeline.DatasciencePipelinePayload.AccountContext;
import nl.ing.lovebird.sitemanagement.health.dspipeline.DatasciencePipelineValue;
import nl.ing.lovebird.sitemanagement.health.dspipeline.RefreshPeriod;
import nl.ing.lovebird.sitemanagement.health.dspipeline.StartDatasciencePipelineProducer;
import nl.ing.lovebird.sitemanagement.health.service.AccountsServiceV1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static nl.ing.lovebird.sitemanagement.health.activities.ActivityEventTestHelper.createIngestionFinishedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTest {

    @Mock
    private AccountsServiceV1 accountsService;

    @Mock
    private ActivityEventService activityEventService;

    @Mock
    private StartDatasciencePipelineProducer startDatasciencePipelineProducer;

    @InjectMocks
    private PipelineService pipelineService;

    @Test
    public void startPipelineShouldSendExpectedDataToDS() {
        var clientId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var clientClaims = TestJwtClaims.createClientUserClaims("junit", UUID.randomUUID(), clientId, userId);
        var clientUserToken = new ClientUserToken("mocked-client-token-" + clientId, clientClaims);

        var activityId = randomUUID();
        var userSiteIdOne = randomUUID();
        var userSiteIdTwo = randomUUID();

        var accountOne = AccountDTOv1.builder()
                .id(randomUUID())
                .type(AccountType.CURRENT_ACCOUNT)
                .lastDataFetchTime(Optional.empty())
                .userSite(mock(AccountDTOv1.UserSiteDTOv1.class))
                .build();
        var accountTwo = AccountDTOv1.builder()
                .id(randomUUID())
                .type(AccountType.SAVINGS_ACCOUNT)
                .lastDataFetchTime(Optional.empty())
                .userSite(mock(AccountDTOv1.UserSiteDTOv1.class))
                .build();

        var accounts = List.of(accountOne, accountTwo);
        var mappedAccounts = List.of(
                new AccountContext(accountOne.id, AccountType.CURRENT_ACCOUNT),
                new AccountContext(accountTwo.id, AccountType.SAVINGS_ACCOUNT)
        );

        when(activityEventService.getAllEvents(activityId)).thenReturn(List.of(
                createIngestionFinishedEvent(activityId, userSiteIdOne, new RefreshPeriod("2018-01", "2018-04")),
                createIngestionFinishedEvent(activityId, userSiteIdTwo, new RefreshPeriod("2015-01", "2015-04"))
        ));

        when(accountsService.getAccounts(any())).thenReturn(accounts);

        pipelineService.startPipeline(clientUserToken, activityId);

        var pipelineValueCaptor = ArgumentCaptor.forClass(DatasciencePipelineValue.class);
        verify(startDatasciencePipelineProducer).sendRefreshTriggeredEvent(eq(clientUserToken), pipelineValueCaptor.capture());

        var payload = pipelineValueCaptor.getValue().getPayload();
        assertThat(payload.getActivityId()).isEqualTo(activityId);
        assertThat(payload.getUserContext().getPreferredCurrency()).isEqualTo("EUR");
        assertThat(payload.getUserContext().getCountryCode()).isEqualTo("NL");
        assertThat(payload.getAccountsContext()).isEqualTo(mappedAccounts);
        assertThat(payload.getRefreshPeriod()).isEqualTo(new RefreshPeriod("2015-01", "2018-04"));
    }

    @Test
    public void startPipelineWithoutRefreshPeriodShouldProduceDSMessageWithoutRefreshPeriod() {
        var clientId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var clientClaims = TestJwtClaims.createClientUserClaims("junit", UUID.randomUUID(), clientId, userId);
        var clientUserToken = new ClientUserToken("mocked-client-token-" + clientId, clientClaims);

        var activityId = randomUUID();

        pipelineService.startPipelineWithoutRefreshPeriod(clientUserToken, activityId);

        var pipelineValueCaptor = ArgumentCaptor.forClass(DatasciencePipelineValue.class);
        verify(startDatasciencePipelineProducer).sendRefreshTriggeredEvent(eq(clientUserToken), pipelineValueCaptor.capture());
        assertThat(pipelineValueCaptor.getValue().getPayload().getRefreshPeriod()).isNull();

        verifyNoInteractions(activityEventService);
    }

    @Test
    public void calculateFinalRefreshPeriodShouldCalculatePeriodAsExpected() {
        var activityId = randomUUID();

        when(activityEventService.getAllEvents(activityId)).thenReturn(List.of(
                createIngestionFinishedEvent(activityId, randomUUID(), new RefreshPeriod(null, null)),
                createIngestionFinishedEvent(activityId, randomUUID(), new RefreshPeriod("2013-01", null)),
                createIngestionFinishedEvent(activityId, randomUUID(), new RefreshPeriod("2018-01", "2018-04")),
                createIngestionFinishedEvent(activityId, randomUUID(), new RefreshPeriod("2015-01", "2015-04")),
                createIngestionFinishedEvent(activityId, randomUUID(), new RefreshPeriod("2018-02", "2018-02"))
        ));

        RefreshPeriod refreshPeriod = pipelineService.calculateFinalRefreshPeriod(activityId);
        assertThat(refreshPeriod.getStartYearMonth()).isEqualTo("2013-01");
        assertThat(refreshPeriod.getEndYearMonth()).isEqualTo("2018-04");
    }

    @Test
    public void calculateFinalRefreshPeriodShouldAllowOnlyNulls() {
        var userId = randomUUID();
        var userSiteId = randomUUID();
        var activityId = randomUUID();

        var refreshUserSitesEvent = new RefreshUserSitesEvent(userId, activityId, ZonedDateTime.now(), Collections.singletonList(userSiteId));
        var ingestionFinishedEventLast = createIngestionFinishedEvent(activityId, userSiteId, new RefreshPeriod(null, null));

        when(activityEventService.getAllEvents(activityId)).thenReturn(List.of(refreshUserSitesEvent, ingestionFinishedEventLast));

        RefreshPeriod refreshPeriod = pipelineService.calculateFinalRefreshPeriod(activityId);

        assertThat(refreshPeriod).isNull();
    }
}
