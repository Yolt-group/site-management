package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.ing.lovebird.sitemanagement.configuration.ApplicationConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExternalIdsBatchLastRecordReceivedEventListener {

    private final OrphanUserService orphanUserService;
    private final OrphanUserBatchRepository orphanUserBatchRepository;

    @Async(ApplicationConfiguration.BATCH_JOB_EXECUTOR)
    @EventListener
    public void handleEvent(@NotNull ExternalIdsBatchLastRecordReceivedEvent event) {
        log.info("All data for batch {} and provider {} is received, starting finding orphaned users",
                event.getOrphanUserBatchId(), event.getProvider());

        orphanUserBatchRepository.updateStatus(event.getClientId(), event.getProvider(), event.getOrphanUserBatchId(), OrphanUserBatch.Status.PREPARE_PROCESSING);
        orphanUserService.findAndSaveOrphanUsers(event.getClientId(), event.getProvider(), event.getOrphanUserBatchId());

        log.info("All orphaned users were identified for batch {} and provider {}, now batch is ready to be executed",
                event.getOrphanUserBatchId(), event.getProvider());
        orphanUserBatchRepository.updateStatus(event.getClientId(), event.getProvider(), event.getOrphanUserBatchId(), OrphanUserBatch.Status.PREPARE_PROCESSING_FINISHED);
    }
}
