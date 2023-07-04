/**
 * @deprecated
 * This package contains 'backend for frontend' functionality that is *only* used by the Yolt App.
 *
 * In particular these endpoints (all are still used as of 2021/03/11):
 * - GET /sites
 * - GET /sites/by-group
 * - GET /sites/{siteId}
 *
 * Once these endpoints are no longer called (check Grafana), we can delete the whole package.
 */
package nl.ing.lovebird.sitemanagement.legacy.sites;
