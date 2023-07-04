package nl.ing.lovebird.sitemanagement.lib;

import nl.ing.lovebird.sitemanagement.lib.types.ClientId;

/**
 * Identifiers of clients so we can implement ad-hoc client specific functionality.  Avoid using these if possible.
 */
public class ClientIds {

    /**
     * The values below belong to a 'testclient'. It is not used by any external party. It is used primarly in cucumber tests.
     * It is inserted via the testscripts so it is available on every environment (inc. team-envs) so the cucumber tests can be run
     * everywhere. (and you can also use this for any manual test)
     */
    public static final ClientId TEST_CLIENT = new ClientId("a2034b12-7dcc-11e8-adc0-fa7ae01bbebc");

    public static final ClientId ACCOUNTING_CLIENT = new ClientId("3e3aae2f-e632-4b78-bdf8-2bf5e5ded17e");

    public static final ClientId YTS_CREDIT_SCORING_APP = new ClientId("28d0b528-ae51-4224-8dbd-8603bbc09c20");
    
    private ClientIds() {
    }
}
