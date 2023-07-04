package nl.ing.lovebird.sitemanagement.health.orchestration;

import nl.ing.lovebird.activityevents.EventType;
import nl.ing.lovebird.activityevents.events.StartEvent;

@SuppressWarnings("squid:S00115")
public enum HealthOrchestrationEventOrigin {
    CategorizationFeedback,
    CounterpartiesFeedback,
    CreateUserSite,
    DeleteUserSite,
    RefreshUserSites,
    RefreshUserSitesFlywheel,
    TransactionCyclesFeedback,
    UpdateUserSite,
    UserUpdate;

    /**
     * @param eventType an EventType of a class  that implements {@link StartEvent}
     */
    public static HealthOrchestrationEventOrigin fromStartEventType(final EventType eventType) {
        switch (eventType) {
            case CATEGORIZATION_FEEDBACK:
                return CategorizationFeedback;
            case COUNTERPARTIES_FEEDBACK:
                return CounterpartiesFeedback;
            case CREATE_USER_SITE:
                return CreateUserSite;
            case DELETE_USER_SITE:
                return DeleteUserSite;
            case REFRESH_USER_SITES:
                return RefreshUserSites;
            case REFRESH_USER_SITES_FLYWHEEL:
                return RefreshUserSitesFlywheel;
            case TRANSACTION_CYCLES_FEEDBACK:
                return TransactionCyclesFeedback;
            case UPDATE_USER_SITE:
                return UpdateUserSite;
            default:
                throw new IllegalStateException("Don't know how to map EventType " + eventType.name() + " to HealthOrchestrationEventOrigin.");
        }
    }
}
