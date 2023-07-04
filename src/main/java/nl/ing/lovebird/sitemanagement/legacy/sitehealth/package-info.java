/**
 * @deprecated
 * This functionality queries Prometheus (which we should not do) to figure out if a bank is "healthy".  This information
 * is then included in a http endpoint to our clients.
 *
 * Reasons that this needs to go:
 * - we are depending on Prometheus for this information (sre does not like this)
 * - it's not terribly useful in its current form (we serve rations between 1 and 0 that indicate success for two operations),
 *   a client probably wants to know instead: "does the bank work?" yes/no
 */
package nl.ing.lovebird.sitemanagement.legacy.sitehealth;