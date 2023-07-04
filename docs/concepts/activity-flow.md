# Activity flow
`site-management` is responsible for orchestrating the full lifecycle of [activities](activity.md) for a user-site. 
For this purpose, it keeps track of all data-fetch and data-enrichment events.
Based on these events, it exposes activities and sends out webhooks when applicable.

In this document, the words data-fetch and (user-site) refresh are used interchangeably and should be interpreted as the operation in which Yolt attempts to fetch new account and transaction information for a given user-site.


## Lifecycle
An activity 'springs into existence' whenever a `StartEvent` is received over Kafka, or `site-management` triggers one 
itself. 
Once an activity has been started, it will be kept around for at most 2 days.


## Start events
Most of the activity flow lies in orchestrating data-fetch and data-enrichment events. 
Not all activities deal with data-fetches, however; some only deal with data-enrichment events.

To be specific, the following `StartEvent`s result in an activity that deals with data-fetches and data-enrichment events:

| `StartEvent`                     | Origin Service      | Explanation                                                                             |
|----------------------------------|---------------------|-----------------------------------------------------------------------------------------|
| `CreateUserSiteEvent`            |  `site-management`  |  A user-site is created so we fetch data                                                |
| `RefreshUserSitesEvent`          |  `site-management`  |  The client triggered a data-fetch for one or multiple user-sites                       |
| `RefreshUserSitesFlywheelEvent`  |  `site-management`  |  Our automated batch-job (flywheel) scheduled a refresh for one or multiple user-sites  |
| `UpdateUserSiteEvent`            |  `site-management`  |  The user-site's access-means were refreshed so we fetch data                           |


While the remaining `StartEvent`s only trigger data science pipelines and orchestrate subsequent data-enrichment events:

| `StartEvent`                     | Origin Service      | Explanation                                                                             |
|----------------------------------|---------------------|-----------------------------------------------------------------------------------------|
| `DeleteUserSiteEvent`            |  `site-management`  |  A user-site has been deleted, which might impact enrichment data                       |
| `CategorizationFeedbackEvent`    |  `preprocessing`    |  Categorization feedback was given, which might impact enrichment data                  |
| `CounterpartiesFeedbackEvent`    |  `preprocessing`    |  Counterparties feedback was given, which might impact enrichment data                  |
| `TransactionCyclesFeedbackEvent` |  `preprocessing`    |  Transaction cycles feedback was given, which might impact enrichment data              |


### Intermediate events
Different events are used to track an activity's state while it's in progress.
All intermediate events are scoped to a single user-site.
We currently have the following set of intermediate events:

| Event Type                            | Origin Service                | Explanation                                                                                       |
|---------------------------------------|-------------------------------|---------------------------------------------------------------------------------------------------|
| `RefreshedUserSiteEvent`              |  `site-management`            |  We have finished fetching data for a single user-site; the data-fetch was unsuccessful           |
| `IngestionFinishedEvent`              |  `accounts-and-transactions`  |  We have finished ingesting (reconciliation + persisting) the fetched data for a single user-site |

The following should always be true for the intermediate events of a refresh activity: _the total sum of `RefreshedUserSiteEvent`s and `IngestionFinishedEvent`s is equal to the number of user-sites in the activity_.


### Terminating events
Activities, unfortunately, do not have strict terminating events. 
The following events come closest to being terminating (but not completely):

| Event Type                            | Origin Service                | Explanation                                                                                          |
|---------------------------------------|-------------------------------|------------------------------------------------------------------------------------------------------|
| `AggregationFinishedEvent`            |  `site-management`            |  We have finished ingesting the accounts and transactions for the given user-site(s)                 |
| `TransactionEnrichmentFinishedEvent`  |  `accounts-and-transactions`  |  Data science has finished enriching the transactions for the given user-site(s)                     |

As a rule of thumb, `AggregationFinishedEvent`s can be considered as terminating event for customers without an enrichment
contract, while `TransactionEnrichmentFinishedEvent`s can be considered as terminating event for customers with an enrichment contract.

There are some unfortunate nuances involved, however:
1. Activities that only trigger data science pipelines and orchestrate subsequent data-enrichment events (see section [_Start events_](#start-events)) do not get an `AggregationFinishedEvent`. The reason for this is that there is no ingestion going on for these activities. A consequence of this is that activities of this type will never have a terminating event when the client doesn't have an enrichment contract.
2. `AggregationFinishedEvent`s also occur for customers with an enrichment contract. For those customers, it should not be considered a terminating event.


### Order of events in an activity
Depending on the type of `StartEvent` and the contract of the client, different activity flows are possible.
[This diagram](../../diagrams/activity-flow-only-data-enrichment.puml) illustrates the activity flow for activities that
only trigger data science pipelines and deal with subsequent data-enrichment events.
[This diagram](../../diagrams/activity-flow-data-fetch-and-enrichment.puml) does the same, but then for activities that
also deal with data-fetch events.

Keep in mind that there is no way to predict the order in which `IngestionFinishedEvent`s and `RefreshedUserSiteEvent`s arrive. 
These events can arrive in any order because the system is asynchronously fetching data for all user-sites in an activity.


## Known Quirks

### Pipelines are triggered for _all_ clients
The data enrichment pipeline is started for all clients, regardless of their contract. The reason for this is that the
data science team needs this data for internal processes and analysis. See [this thread](https://lovebirdteam.slack.com/archives/CNF2YANLR/p1649950334190739?thread_ts=1649943998.313389&cid=CNF2YANLR) for more information.

To ensure clients without an enrichment contract don't get access to enrichment data, `accounts-and-transactions` 
makes sure to not process the enrichment data when a client doesn't have an enrichment contract.
While doing so, it also makes sure to not send a `TransactionEnrichmentFinishedEvent` to `site-management`.


### Spontaneous Callbacks
In regular scenarios we are initiating a data-fetch on behalf of our users or because of an automated batch job.

Some scraping sites involve a third party pushing unsolicited data to us through the `callbacks` services.
This results in transaction data arriving 'spontaneously'.

The flow of 'unsolicited data' is complex.
This data first arrives at `callbacks` and is then propagated to `site-management` to figure out to which user the data belongs.
`site-management` in turn propagates the data to `providers`.
An optional round-trip is made (`providers` -> `site-management` -> `providers`) after parsing the acc/trx data (only `providers` can do this).
Eventually though, `providers` will send the data to `accounts-and-transactions` over Kafka.
It is at this point that `providers` *may* come to the conclusion that it's an unsolicited data fetch and set `activityId <- new UUID(0, 0)`

Note: search the code for `ACTIVITY_ID_OF_SPONTANEOUS_CALLBACK_NOT_TRIGGERED_BY_USER_ACTIVITY` to find the code.
