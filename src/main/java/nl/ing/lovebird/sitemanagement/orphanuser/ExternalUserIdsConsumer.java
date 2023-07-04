package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ExternalUserIdsConsumer {

    private static final Integer SAVE_EXTERNAL_USER_IDS_BATCH_SIZE = 100;

    private final OrphanUserBatchRepository orphanUserBatchRepository;
    private final OrphanUserExternalIdRepository orphanUserExternalIdRepository;
    private final ApplicationEventPublisher eventPublisher;

    @KafkaListener(topicPattern = "${yolt.kafka.topics.formProviderExternalUserIds.topic-name}",
            concurrency = "${yolt.kafka.topics.formProviderExternalUserIds.listener-concurrency}")
    public void providerExternalUserIdsRecord(@Payload final ProviderExternalUserIds externalUserIds) {
        log.info("Got another chunk of external user ids for provider {} and batch {}, last: {}",
                externalUserIds.getProvider(), externalUserIds.getBatchId(), externalUserIds.getIsLast());

        final boolean isLast = saveIncomingDataChunks(externalUserIds);
        if (isLast) {
            eventPublisher.publishEvent(new ExternalIdsBatchLastRecordReceivedEvent(this,
                    externalUserIds.getClientId(), externalUserIds.getProvider(), externalUserIds.getBatchId()));
        }

    }

    private boolean saveIncomingDataChunks(@NotNull final ProviderExternalUserIds providerExternalUserIds) {
        if (orphanUserBatchRepository.get(providerExternalUserIds.getClientId(), providerExternalUserIds.getProvider(), providerExternalUserIds.getBatchId()).isEmpty()) {
            throw new OrphanUserBatchNotFoundException(providerExternalUserIds.getProvider(), providerExternalUserIds.getBatchId());
        }

        final List<OrphanUserExternalId> orphanUserExternalIds = providerExternalUserIds.getExternalUserIds().stream()
                .map(id -> new OrphanUserExternalId(providerExternalUserIds.getClientId(), providerExternalUserIds.getProvider(), providerExternalUserIds.getBatchId(), id))
                .collect(Collectors.toList());
        orphanUserExternalIdRepository.saveBatch(orphanUserExternalIds, SAVE_EXTERNAL_USER_IDS_BATCH_SIZE);

        final boolean isLast = providerExternalUserIds.getIsLast();
        final OrphanUserBatch.Status status = isLast ? OrphanUserBatch.Status.PREPARE_RECEIVING_DATA_FINISHED : OrphanUserBatch.Status.PREPARE_RECEIVING_DATA;
        orphanUserBatchRepository.updateStatus(providerExternalUserIds.getClientId(), providerExternalUserIds.getProvider(), providerExternalUserIds.getBatchId(), status);
        return isLast;
    }
}
