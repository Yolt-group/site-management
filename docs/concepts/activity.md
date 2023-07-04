# Activity
An activity is not an actual entity in our system, its existence is only implied by [activity events](activity-event.md).
An activity:
1. is started either by *a user* or by an *automated process* such as a background refresh.
2. causes state to change in our system (e.g.: a user changes the categorization of a transaction)
3. *might* trigger the data science pipeline for a user
4. ends whenever the data science pipeline finishes, or whenever `site-management` decides to not start the pipeline

Some activities are comprised of multiple asynchronous events that need to complete before a next step can be started.
[Activity flow](activity-flow.md) details these steps.
