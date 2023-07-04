# Migrating sites

Migration comes into play when we have added a `DIRECT_CONNECTION` for a site that we already provided by `SCRAPING` previously.
We want users to use the new `DIRECT_CONNECTION`.
However, a `DIRECT_CONNECTION` site might not always support all the accounttypes that were supported through `SCRAPING`.

Therefore, whenever a migration takes place, there are three sites that play a role:

- `source`: the original `SCRAPING` site that a user is connected to
- `direct`: a new `DIRECT_CONNECTION` site that the user has not yet connected, this site supports a subset of the account types of `source`
- `remainder`: a `SCRAPING` site that has the same provider as `source`, this site has less supported account types

The below is true for the `source`, `direct`, and `remainder` sites: 

- `source.provider` = `remainder.provider`
- `direct.whiteListedAccountTypes` ⋃ `remainder.whiteListedAccountTypes` = `source.whiteListedAccountTypes`
- `direct.whiteListedAccountTypes` ⋂ `remainder.whiteListedAccountTypes` = Ø


The process of migration differs per client, we describe the process for each client separately.


## Client: a5154eb9-9f47-43b4-81b1-fce67813c002

Uses the property `UserSite.migrationStatus` `[1]` to determine if a user should migrate.

This is the process:

1. Yolt (product owner / admin) starts the migration process for the `source` site.  We use the "initiate migration" functionality in YAP to do this.
2. A background process then sets the `UserSite.migrationStatus` to `MIGRATION_NEEDED` for all usersites that belong to the `source` site. 
3. We will look at the `groupingBy` property to find the corresponding `direct` site to which the user must migrate their usersites.
4. After adding the `direct` site the user is free to add the `remainder` site.
5. They have the `source` usersite removed.

---

`[1]` The property `usersite.migrationStatus` was previously used for an 'old' migration strategy of the Yolt app that has been phased out already.


## Client: Yolt -- 297ecda4-fd60-4999-8575-b25ad23b249c

The Yolt app uses the property `UserSite.newConnection` to determine if a user should migrate to a `direct` site.
The property `newConnection` is not exposed by `site-management`, instead it is added by a bff [1] through which the Yolt app calls `site-management`.

Before we explain how the value of the property `newConnection` is computed we need to explain which sites `site-management` returns, as this is user dependent.
The [sites list](../functions/list-sites.md) that we return for a user of the Yolt app will only ever include at most 2 out of 3 relevant sites for a `groupingBy`:
- `source`: included iff a user has a user-site linked to this site
- `direct`: always included
- `remainder`: included iff `source` is not returned

We can now distinguish these two groups of users:
- `existing` users have a usersite that is linked to the `source` site
- `new` users do not have a usersite linked to the `source` site

The bff [1] sets the property `newConnection` to the id of the `direct` site for `existing` users.

If the app encounters a usersite with a non-null `newConnection` property, the user is urged to add the `direct` site.
Any duplicate account will be merged by the user manually, this relates to the concept of migrated accounts.
The `source` will not be deleted.
Instead `providers` will drop the 'merged/migrated accounts' from now on.

---

[1] `yolt-app-bff-usersites`

## Open questions

- should we not have metrics in place to monitor this process?
- what if the `direct` site gains support for additional account types
  Then the whitelisted account types is expanded on the `direct` and removed from the `scraping` site. 
  We need to have a test (YCO-588) that makes sure this happens.
