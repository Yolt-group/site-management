# Activity event

An activity event belongs to an [activity](activity.md).
Many activity events can occur for a given [activity](activity.md).

Every activity event has a type, these are the available types:
- `CREATE_USER_SITE`
- `UPDATE_USER_SITE`
- `DELETE_USER_SITE`
- `REFRESH_USER_SITES`
- `REFRESH_USER_SITES_FLYWHEEL`
- `REFRESHED_USER_SITE` `(*)`
- `INGESTION_FINISHED` `(*)`
- `AGGREGATION_FINISHED` `(*)`
- `COUNTERPARTIES_FEEDBACK`
- `CATEGORIZATION_FEEDBACK`
- `TRANSACTION_CYCLES_FEEDBACK`

The events marked with `(*)` do not implement the interface `StartEvent`.
This interface is a 'marker' interface and divides the activity events into two groups.

###### Notes

The java code for activity events is stored in `yolt-shared-dtos`.
