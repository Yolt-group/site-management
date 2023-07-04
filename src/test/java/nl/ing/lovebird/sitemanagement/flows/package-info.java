/**
 * This package contains a series of "integration" tests that stub away or fake messages from external services.  These
 * tests give us confidence that the core flows of site-management that drive user interaction with UserSites work as
 * expected.
 *
 * If you are new, the easiest flow to start with to get acquainted with the behaviour of a typical "add bank flow" is
 * {@link nl.ing.lovebird.sitemanagement.flows.singlestep.SingleRedirectStepSuccessTest}.  It is --hopefully-- well
 * documented, more so than the other flows in any case, and should make site-managements place in the system clear.
 */
package nl.ing.lovebird.sitemanagement.flows;