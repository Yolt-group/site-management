# User site lock

Table: `user_site_lock`.

A lock is acquired at the start of an [activity](activity.md), the lock is 'owned' by that [activity](activity.md).
A lock is held for no longer than 600 seconds (ttl in Cassandra).

## Lifecycle

A lock is created:
- at the start of some 'maintenance' activity: e.g. [adding a site](../functions/add-site.md), or [updating a site](../functions/update-site.md).
- before [fetching data](../functions/fetch-data.md)

A lock is owned by an [activity](activity.md).

## Properties

- **userId** the user to which the [user-site](user-site.md) for which the lock exists
- **userSiteId** the id of the [user-site](user-site.md) for which the lock exists
- **activityId** the [activity](activity.md) that was the reason for creating the lock
