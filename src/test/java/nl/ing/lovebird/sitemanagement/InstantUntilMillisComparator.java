package nl.ing.lovebird.sitemanagement;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

public class InstantUntilMillisComparator implements Comparator<Instant> {
    @Override
    public int compare(Instant o1, Instant o2) {
        return o1.truncatedTo(ChronoUnit.MILLIS).compareTo(o2.truncatedTo(ChronoUnit.MILLIS));
    }
}
