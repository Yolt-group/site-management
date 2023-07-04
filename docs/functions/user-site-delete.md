# User site delete

Users sites can be deleted in the following situations:

- A client deletes a single user site by calling the endpoint `DELETE /v1/users/{userId}/user-sites/{userSiteId}`
- A client deletes a user, and later the `maintenance` service asynchronously requests `site-management` to remove all user data

*Note:* the below information omits details that come into play for user sites of scraping providers and focuses on direct connections only

## Deleting a single user site

A client calls the endpoint `DELETE /v1/users/{userId}/user-sites/{userSiteId}` to delete a single user site.
`site-management` will then do the following:

- Mark the user site as having been deleted (a soft delete)
- Delete the [external consent](../concepts/external-consent.md) associated with the user site (if it exists)
- Publish a `DeleteUserSiteEvent` on the `activity-events` topic
- Publish a `UserSiteEventDelete` on the `user-site-events` topic
- Delete the associated [user site sessions](../concepts/user-site-session.md) (if any exist)

*Note:*
The `accounts-and-transactions` service listens to `UserSiteEventDelete` events on the `user-site-events` topic.
In response to such an event all account and transaction data belonging to the user site will be deleted.
This mechanism exists to prevent significant delays between deleting a usersite by a client and the disappearance of the account and transaction data from the clients perspective.
In addition, the `accounts-and-transactions` service *also* responds to the normal user deletion calls issued by `maintenance`.


## Deleting all user sites after a user delete 

Whenever a client deletes a user, the `maintenance` service will ask all services in the cluster to remove the user data.
Upon receiving a call from `maintenance`, the following happens:

- The function `UserSiteDeleteService.deleteUserCallFromMaintenance` is invoked (as a result of `maintenance` making the http call to `site-management`).  For all user-sites this function will:
- Mark the user site as having been deleted (a soft delete)
- Schedule the actual deletion to happen at a later point by sending the userSiteId to `maintenance`

At a later point in time, the `maintenance` service will call `DELETE /user-sites/{userId}/{userSiteId}`.
This legacy endpoint performs the same logic as [Deleting a single user site](#deleting-a-single-user-site)
