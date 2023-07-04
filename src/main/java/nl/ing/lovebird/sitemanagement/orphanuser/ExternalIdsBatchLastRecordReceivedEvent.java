package nl.ing.lovebird.sitemanagement.orphanuser;

import lombok.Getter;
import nl.ing.lovebird.sitemanagement.lib.types.ClientId;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class ExternalIdsBatchLastRecordReceivedEvent extends ApplicationEvent {

    private final ClientId clientId;
    private final String provider;
    private final UUID orphanUserBatchId;

    public ExternalIdsBatchLastRecordReceivedEvent(Object source, ClientId clientId, String provider, UUID orphanUserBatchId) {
        super(source);
        this.clientId = clientId;
        this.provider = provider;
        this.orphanUserBatchId = orphanUserBatchId;
    }
}
