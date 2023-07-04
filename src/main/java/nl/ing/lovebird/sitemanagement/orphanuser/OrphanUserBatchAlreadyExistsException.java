package nl.ing.lovebird.sitemanagement.orphanuser;

import java.util.UUID;

public class OrphanUserBatchAlreadyExistsException extends RuntimeException {

    private static final String MSG = "Batch with id %s for provider %s already exists";

    public OrphanUserBatchAlreadyExistsException(UUID batchId, String provider) {
        super(String.format(MSG, batchId, provider));
    }
}
