package nl.ing.lovebird.sitemanagement.sites;

/**
 * Banks model consents differently on their end, this enum contains information that is relevant for clients when
 * managing user-sites.
 *
 * We use 3 different terms for 'user' below, these are:
 * - yolt-user: a client-user entity managed by the client of yolt, basically "our" (Yolt's) concept of a user
 * - bank-user: a set of credentials with which a user can make him-/herself known to a bank and login to the online bank environment
 * - external-user: a user administered in the backend environment of one of our clients, typically there is a one-to-one correspondence between a yolt-user and an external-user
 *
 * Note: a {@link Site} can be endowed with more than one of these 'behaviours', we don't need this currently but
 * it might be useful at some point.
 */
public enum ConsentBehavior {

    /**
     * The bank requires the bank-user to complete a separate consent flow for every account that the external-user wants to share
     * with the TPP.
     *
     * What this means for clients: the client must create a separate user-site for every account that the external-user wants to share with the client.
     */
    CONSENT_PER_ACCOUNT,

}
