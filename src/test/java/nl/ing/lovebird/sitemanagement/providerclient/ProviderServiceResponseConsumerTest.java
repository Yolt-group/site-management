package nl.ing.lovebird.sitemanagement.providerclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.ing.lovebird.clienttokens.ClientUserToken;
import nl.ing.lovebird.sitemanagement.providercallback.ProviderCallbackAsyncService;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequest;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequestRepository;
import nl.ing.lovebird.sitemanagement.providercallback.ProviderCallbackAsyncService;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequest;
import nl.ing.lovebird.sitemanagement.providerrequest.ProviderRequestRepository;
import nl.ing.lovebird.sitemanagement.providerresponse.GenericDataProviderResponseProcessor;
import nl.ing.lovebird.sitemanagement.providerresponse.ProviderServiceResponseConsumer;
import nl.ing.lovebird.sitemanagement.providerresponse.ScrapingDataProviderResponseProcessor;
import nl.ing.lovebird.sitemanagement.usersite.PostgresUserSite;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType.USER_REFRESH;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class ProviderServiceResponseConsumerTest {
    private static final String NO_SUPPORTED_ACCOUNTS = "NO_SUPPORTED_ACCOUNTS";
    private static final UserSiteActionType USER_SITE_ACTION_TYPE = USER_REFRESH;

    @Mock
    private ProviderRequestRepository providerRequestRepository;
    @Mock
    private GenericDataProviderResponseProcessor genericDataProviderResponseProcessor;
    @Mock
    private ScrapingDataProviderResponseProcessor scrapingDataProviderResponseProcessor;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @Mock
    private ProviderCallbackAsyncService providerCallbackAsyncService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private UserSiteService userSiteService;
    @Mock
    private ClientUserToken clientUserToken;

    private ProviderServiceResponseConsumer subject;

    @BeforeEach
    void setUp() {
        subject = new ProviderServiceResponseConsumer(providerRequestRepository, genericDataProviderResponseProcessor,
                scrapingDataProviderResponseProcessor, kafkaProducerService, providerCallbackAsyncService, objectMapper,
                userSiteService);
        when(clientUserToken.getUserIdClaim()).thenReturn(userId);
    }

    private final UUID userId = UUID.randomUUID();
    private final UUID userSiteId = UUID.randomUUID();
    private final UUID providerRequestId = UUID.randomUUID();
    private final UUID activityId = UUID.randomUUID();

    @Test
    void noSupportedAccountsMessage_providerRequestIdIsNull() throws Exception {
        byte[] fakePayload = "{}".getBytes();
        ConsumerRecord record = mock(ConsumerRecord.class);
        when(userSiteService.getUserSite(any(UUID.class), any(UUID.class))).thenReturn(new PostgresUserSite());
        when(record.value()).thenReturn(fakePayload);
        NoSupportedAccountDTO noSupportedAccountDTO = new NoSupportedAccountDTO(userId, userSiteId, null);
        when(objectMapper.readValue(fakePayload, NoSupportedAccountDTO.class)).thenReturn(noSupportedAccountDTO);

        subject.providerAccountsMessage(record,  NO_SUPPORTED_ACCOUNTS, clientUserToken);

        verify(providerCallbackAsyncService).processNoSupportedAccountsFromCallback(userId, userSiteId);
    }

    @Test
    void noSupportedAccountsMessage_existingProviderRequestId() throws Exception {
        byte[] fakePayload = "{}".getBytes();
        ConsumerRecord record = mock(ConsumerRecord.class);
        when(userSiteService.getUserSite(any(UUID.class), any(UUID.class))).thenReturn(new PostgresUserSite());
        when(record.value()).thenReturn(fakePayload);
        NoSupportedAccountDTO noSupportedAccountDTO = new NoSupportedAccountDTO(userId, userSiteId, providerRequestId);
        when(objectMapper.readValue(fakePayload, NoSupportedAccountDTO.class)).thenReturn(noSupportedAccountDTO);
        final ProviderRequest providerRequest = new ProviderRequest(providerRequestId, activityId, userId, userSiteId, USER_SITE_ACTION_TYPE);
        when(providerRequestRepository.get(userId, providerRequestId)).thenReturn(Optional.of(providerRequest));

        subject.providerAccountsMessage(record, NO_SUPPORTED_ACCOUNTS, clientUserToken);

        verify(genericDataProviderResponseProcessor).processNoSupportedAccountsMessage(userId, userSiteId, USER_SITE_ACTION_TYPE);
    }
}
