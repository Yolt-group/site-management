package nl.ing.lovebird.sitemanagement.orphanuser;

import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class ExternalIdsBatchLastRecordReceivedEventListenerTest {

    @Mock
    private OrphanUserService orphanUserService;
    @Mock
    private OrphanUserBatchRepository orphanUserBatchRepository;
    @InjectMocks
    private ExternalIdsBatchLastRecordReceivedEventListener listener;

    @Test
    void testEventListener() {
        ClientId clientId = ClientId.random();
        String provider = "YODLEE";
        UUID batchId = UUID.randomUUID();
        listener.handleEvent(new ExternalIdsBatchLastRecordReceivedEvent(this, clientId, provider, batchId));

        verify(orphanUserBatchRepository).updateStatus(clientId, provider, batchId, OrphanUserBatch.Status.PREPARE_PROCESSING);
        verify(orphanUserService).findAndSaveOrphanUsers(clientId, provider, batchId);
        verify(orphanUserBatchRepository).updateStatus(clientId, provider, batchId, OrphanUserBatch.Status.PREPARE_PROCESSING_FINISHED);
        verifyNoMoreInteractions(orphanUserBatchRepository, orphanUserService);
    }
}
