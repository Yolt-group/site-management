package nl.ing.lovebird.sitemanagement.orphanuser;

import java.util.UUID;

public class OrphanUserBatchNotFoundException extends RuntimeException {

    private static final String MSG = "No orphan user batch was found for provider %s with batch ID %s.";

    public OrphanUserBatchNotFoundException(String provider, UUID orphanUserBatchId) {
        super(String.format(MSG, provider, orphanUserBatchId));
    }
}
