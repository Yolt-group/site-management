# Health orchestration event

`site-management` publishes health orchestration events to keep a wide audience (the whole system) informed about finished refreshes.

A health orchestration event has these properties:
- `type` the type of the event
- `correlationId` an identifier with which grouping health orchestration events is possible within the scope of a single data fetch
- `userSiteIds` one or more identifiers of affected user sites
- `origin` the initial trigger that caused data ingestion to happen

At the moment there is a single event type: `RefreshFinished`. 
Despite its name, this event type only signifies that data ingestion has finished for all user-sites in an activity. 
Data enrichment is likely still being performed when this event is published.


Health orchestration events were introduced as an alternative to [activity events](activity-event.md), as these
are only meant for internal coordination between services involved in the refresh flow (`site-management`, 
`accounts-and-transactions`, and `preprocessing`).
