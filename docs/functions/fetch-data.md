# Fetch data

Colloquially: 'refreshing a user site'.
This operation is retrieves data about a [user's](../concepts/user.md) financials:

- accounts
- transactions
- beneficiaries
- etc.


## Triggers

These are the possible triggers for a data fetch:

1. the [user](../concepts/user.md) triggers a data fetch manually for one of their [user-sites](../concepts/user-site.md)
2. the [user](../concepts/user.md) triggers a data fetch manually for all of their [user-sites](../concepts/user-site.md)
3. our internal background process ([the 'flywheel'](../processes/internal-flywheel.md)) fetches data for all of the [user-sites](../concepts/user-site.md) of a user
4. a user has just [added a site](add-site.md) which triggers a fetch for that 1 [user-site](../concepts/user-site.md)
5. a user has just updated a user-site which triggers a fetch for that 1 [user-site](../concepts/user-site.md)
6. a scraping provider sends us data via a callback for a single user-site

There is a central place in the codebase of `site-management` that coordinates _triggering_ a data fetch.
That place is `nl.ing.lovebird.sitemanagement.usersite.UserSiteRefreshService`.

