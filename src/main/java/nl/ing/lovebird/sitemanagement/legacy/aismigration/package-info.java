/**
 * @deprecated this functionality is deprecated and is used only in "read-only" mode.  In the past, site-management
 * used to have knowledge of the concept "account" and had functionality that facilitated "migrating" accounts.
 *
 * We cannot remove this functionality yet because we still need to know the ids of migrated accounts, these are necessary
 * to fill the field {@link nl.ing.lovebird.sitemanagement.providerclient.UserSiteDataFetchInformation#getUserSiteMigratedAccountIds()},
 * providers uses this information to filter out accounts that occur in this list (i.e. they are not sent to A&T).
 *
 * There exists a metric in the providers codebase that keeps track of how often accounts are filtered based on the
 * field ~~, if this metric is 0 for an extended period of time we can be confident that this functionality is no
 * longer useful and can thus be removed.
 *
 * The relevant code snippet in providers, introduced with https://git.yolt.io/providers/providers/-/merge_requests/2063
 *                 // This metric was added at the request of the YTS Core team.  The 'account migration' functionality
 *                 // is deprecated and we are interested in knowing if we could potentially drop the userSiteMigratedAccountIds
 *                 // field from the UserSiteDataFetchInformation object.  To this end we keep track of how often we filter
 *                 // out accounts based on this list.  If the counter remains 0 we can remove this list since it is clearly no
 *                 // longer needed.
 *                 if (migratedAccountsExternalIds.contains(account.getAccountId())) {
 *                     registry.counter("account_migration_filter", "provider", providerName).increment();
 *                 }
 */
package nl.ing.lovebird.sitemanagement.legacy.aismigration;