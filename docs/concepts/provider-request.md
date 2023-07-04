# Provider request

Table: `provider_request_v2`.
Related materialized view: `provider_request_v2_by_user_activity`;

This table contains a row for some asynchronous calls to providers.
The state kept in the table is necessary to process the response at a later point in time (e.g. to correlate the request to some [activity](activity.md)).
