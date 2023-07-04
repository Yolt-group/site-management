# Internal flywheel

The internal flywheel is a background process that performs scheduled retrieval of data for users.


## How the flywheel works

The internal flywheel has two important components:

1. scheduling of users
2. the flywheel background process

We will describe these separately.


## Scheduling of users

There is an endpoint `/batch/schedule-user-refreshes` that is called every night at 00:00 UTC time.
This endpoint is responsible for making a planning for the next day based on all the users in the database.
The idea is to "spread" out the users throughout the day to spread the load evenly.

The setting [5] determines how often we will fetch data for a given user in the same day.
If [5] is set to the value 1, we will smear out the users across the 23 available hours evenly.
If [5] is set to the value 2, we will  smear out the users *TWICE* across the 23 available hours evenly, so once across the first 11.5 hours and then once more across the last 11.5 hours.
And so on.

The setting [6] permits a user to override [5] on a per-client basis. 


## The flywheel background process

There is an endpoint `/flywheel/internal` that is called at set intervals (every minute between 01:00 and 23:59 UTC time) by the `batch-trigger` pod.
We use this instead of a `@Scheduled` annotation because we run a multi-cluster multi-pod setup.
Using a single instance of `batch-trigger` that calls out to one of the pods is an easy way to ensure no concurrent activity takes place.

Once `/flywheel/internal` is called the following happens.

If the flywheel is enabled [1] the flywheel will retrieve the list of users that have been scheduled for the current minute.

We decide for each user whether to fetch data yes or no, as follows:

For the Yolt App, we will skip a user if any of the following are true:
- the user has never logged in (User.lastLogin = null)
- the user has logged in, but the login was more than [2] days ago.
- the user is blocked (User.status = BLOCKED)

For all other clients, we skip a user if any of the following are true:
- the user is blocked (User.status = BLOCKED)


We will check for which of the users' user-sites we will fetch data.
We skip a user-site if any of the following are true:
- the provider is blacklisted [4]
- the user-site is in a status where we cannot perform a data fetch because user interaction is required
- the user-site has already been refreshed in the past [3] seconds
- the user-site is in a state where it is likely that a refresh will fail because a bank recently required the user to perform MFA (scraping only)
- the user-site is being migrated
- the site is in maintenance
- the site is no longer supported

Otherwise, we proceed by fetching data for a user-site.


## Appendix: configuration properties

- [1] `lovebird.flywheel.internal.enabled`:\
  boolean that indicates if the flywheel is enabled
- [3] `lovebird.flywheel.internal.throttling.minimumSecondsSinceLastRefresh`:\
  integer: if a user-site has been refreshes in the last x seconds, skip it 
- [4] `lovebird.flywheel.internal.throttling.blacklistedProviders`:\
  list of strings for which we do not perform flywheel refreshes
- [5] `lovebird.flywheel.internal.defaultRefreshesPerDay`:\
  integer value that decides how often we fetch data for a given user in every day
- [6] `lovebird.flywheel.internal.refreshesPerDay`:\
  map<uuid, int> with which [5] can be overridden on a per-client basis
 