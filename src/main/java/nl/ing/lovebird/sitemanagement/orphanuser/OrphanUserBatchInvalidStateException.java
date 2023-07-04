package nl.ing.lovebird.sitemanagement.orphanuser;

public class OrphanUserBatchInvalidStateException extends RuntimeException {

    public OrphanUserBatchInvalidStateException(OrphanUserBatch.Status expected, OrphanUserBatch.Status actual) {
        super(String.format("OrphanUserBatch has incorrect state - expected: %s, actual: %s", expected, actual));
    }
}
