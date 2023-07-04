package nl.ing.lovebird.sitemanagement.orphanuser;

import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.Clock.systemUTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class ExternalUserIdsConsumerTest {

    private static final ClientId CLIENT_ID = ClientId.random();
    private static final String PROVIDER = "YODLEE";
    private static final UUID BATCH_ID = UUID.randomUUID();

    @Mock
    private OrphanUserBatchRepository orphanUserBatchRepository;
    @Mock
    private OrphanUserExternalIdRepository orphanUserExternalIdRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Captor
    private ArgumentCaptor<List<OrphanUserExternalId>> externalIdsListCaptor;
    @InjectMocks
    private ExternalUserIdsConsumer consumer;


    @Test
    void whenNoBatchFound_shouldThrowException() {
        assertThatThrownBy(() -> {
            when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.empty());

            consumer.providerExternalUserIdsRecord(payload(false, "123"));
        }).isInstanceOf(OrphanUserBatchNotFoundException.class);
    }

    @Test
    void whenNotLastDataChunk_shouldOnlyUpdateStatus() {
        when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.of(batch()));

        consumer.providerExternalUserIdsRecord(payload(false, "123"));

        verify(orphanUserExternalIdRepository).saveBatch(externalIdsListCaptor.capture(), anyInt());
        verify(orphanUserBatchRepository).updateStatus(CLIENT_ID, PROVIDER, BATCH_ID, OrphanUserBatch.Status.PREPARE_RECEIVING_DATA);
        assertThat(externalIdsListCaptor.getValue()).containsExactlyInAnyOrder(
                new OrphanUserExternalId(CLIENT_ID, PROVIDER, BATCH_ID, "123"));
        verify(eventPublisher, never()).publishEvent(any(ApplicationEvent.class));
    }

    @Test
    void whenLastDataChunk_shouldUpdateStatus_andPublishEvent() {
        when(orphanUserBatchRepository.get(CLIENT_ID, PROVIDER, BATCH_ID)).thenReturn(Optional.of(batch()));

        consumer.providerExternalUserIdsRecord(payload(true, "456"));

        verify(orphanUserExternalIdRepository).saveBatch(externalIdsListCaptor.capture(), anyInt());
        verify(orphanUserBatchRepository).updateStatus(CLIENT_ID, PROVIDER, BATCH_ID, OrphanUserBatch.Status.PREPARE_RECEIVING_DATA_FINISHED);
        assertThat(externalIdsListCaptor.getValue()).containsExactlyInAnyOrder(
                new OrphanUserExternalId(CLIENT_ID, PROVIDER, BATCH_ID, "456"));
        verify(eventPublisher).publishEvent(any(ExternalIdsBatchLastRecordReceivedEvent.class));
    }

    private static ProviderExternalUserIds payload(boolean isLast, String... externalIds) {
        return new ProviderExternalUserIds(CLIENT_ID, BATCH_ID, PROVIDER, List.of(externalIds), isLast);
    }

    private static OrphanUserBatch batch() {
        return new OrphanUserBatch(CLIENT_ID, PROVIDER, BATCH_ID, Instant.now(systemUTC()), Instant.now(systemUTC()), OrphanUserBatch.Status.PREPARE_INITIATED);
    }
}
