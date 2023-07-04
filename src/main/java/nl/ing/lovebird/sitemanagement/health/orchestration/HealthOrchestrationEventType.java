package nl.ing.lovebird.sitemanagement.health.orchestration;

@SuppressWarnings("squid:S00115")
public enum HealthOrchestrationEventType {

    /**
     * Despite its name, this value indicates that account- and transaction data has been ingested. It could be that
     * the enrichment process is still in progress, if the client has this claim.
     */
    RefreshFinished;
}
