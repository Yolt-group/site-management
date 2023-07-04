package nl.ing.lovebird.sitemanagement.flywheel;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;

/**
 * This class is a utility class used by the flywheel.
 * It is responsible to give proper UUID ranges given a # refreshes per day, and the time on which the flywheel runs.
 * <p>
 * On initialization everything is setup and prepared, so accessing it through {@link #getUUIRange(int, LocalTime)} is pretty costless.
 * <p>
 * How this is done:
 * First we divide the range from 0 ---> MAX_UUID (2^128 -1)  in (roughly) 1440 chunks. 1440 are the amount of minutes in a day.
 * See the 'indexPairs' in the constructor.
 * This means, that if a client just wants to refresh a user once a day, for every minute of the day you can get one of the 1440 chunks.
 * If a client wants 2 refreshes a day, we can take the first 2 chunks in the first minute (and combine them), which effectively divides the full range in 720 chunks.
 * <p>
 * We only consider 1,2,3,4 refreshes per day, as 4 is the maximumum limit set by PSD2. These are all multiples of 1440, so we dividing these chunks is relatively straight forward.
 * <p>
 * In ascii art:
 * we divide 0 to MAX_UUID in 1440 (24*60) blocks:
 * 0                                                                                   2^128-1
 * ┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴... ┴───┴───┴───┴
 * Then for the first minute we refresh:
 * n=1=refreshesPerDay
 * 0                                                                                   2^128-1
 * ┴▀▀▀┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴... ┴───┴───┴───┴
 * n=2=refreshesPerDay
 * 0                                                                                   2^128-1
 * ┴▀▀▀┴▀▀▀┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴... ┴───┴───┴───┴
 * etc.
 * Second minute:
 * n=1=refreshesPerDay
 * 0                                                                                   2^128-1
 * ┴───┴▀▀▀┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴... ┴───┴───┴───┴
 * n=2=refreshesPerDay
 * 0                                                                                   2^128-1
 * ┴───┴───┴▀▀▀┴▀▀▀┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴... ┴───┴───┴───┴
 */
@Slf4j
@Service
public class FlywheelUserUUIDRangeSelector {

    private static final BigInteger MAX_UUID = BigInteger.TWO.pow(128).subtract(BigInteger.ONE);
    private static final int MINUTES_IN_DAY = 24 * 60;

    private final Map<Integer, Map<Integer, UUIDRange>> uuidRangeByMinuteByRefreshesADay;

    public FlywheelUserUUIDRangeSelector() {

        List<Pair<BigInteger, BigInteger>> indexPairs = divideMaxUUIDByMinutesInDay();

        assert indexPairs.size() == MINUTES_IN_DAY;

        // We only consider 1,2,3,4 refreshes a day. 4 is PSD2 maximum. For 0 refreshes a day this class should not be consulted as there exists no range.
        uuidRangeByMinuteByRefreshesADay = IntStream.of(1, 2, 3, 4).boxed().collect(toMap(Function.identity(), i -> createUUIDRangeForNrOfRefreshes(i, indexPairs)));
    }

    public UUIDRange getUUIRange(int nrOfRefreshesADay, LocalTime localTime) {
        if (nrOfRefreshesADay <= 0) {
            throw new IllegalArgumentException("There is no possible UUIDRange for 0 refreshes a day. There is no range");
        }
        if (nrOfRefreshesADay > 5) {
            log.error("5 refreshes a day requested. This is not allowed by PSD2. Please change configuration. Now continuing with 4 (max) refreshes a day..");
            nrOfRefreshesADay = 4;
        }
        return uuidRangeByMinuteByRefreshesADay.get(nrOfRefreshesADay).get(localTime.getMinute() + 60 * localTime.getHour());
    }

    private Map<Integer, UUIDRange> createUUIDRangeForNrOfRefreshes(Integer nrOfRefreshesADay, List<Pair<BigInteger, BigInteger>> indexPairs) {
        return IntStream.range(0, MINUTES_IN_DAY)
                .boxed()
                .collect(toMap(Function.identity(), minute -> {
                    int lowerIndex = nrOfRefreshesADay * minute;
                    var lowerPair = indexPairs.get(lowerIndex % MINUTES_IN_DAY);
                    var higherPair = indexPairs.get((lowerIndex + nrOfRefreshesADay - 1) % MINUTES_IN_DAY);
                    return new UUIDRange(fromBigInt(lowerPair.getLeft()), fromBigInt(higherPair.getRight()));
                }));
    }

    private List<Pair<BigInteger, BigInteger>> divideMaxUUIDByMinutesInDay() {

        BigInteger stepSize = MAX_UUID.divide(BigInteger.valueOf(MINUTES_IN_DAY));
        List<Pair<BigInteger, BigInteger>> indexPairs = new ArrayList<>(MINUTES_IN_DAY);
        indexPairs.add(Pair.of(BigInteger.ZERO, stepSize));
        for (int i = 1; i < MINUTES_IN_DAY - 1; i++) {
            BigInteger previousEnd = indexPairs.get(i - 1).getRight();
            indexPairs.add(Pair.of(previousEnd, previousEnd.add(stepSize)));
        }
        // Account for the rounding error (remainder get dropped) in step size, this last pair is a little big larger than the others.
        indexPairs.add(Pair.of(indexPairs.get(MINUTES_IN_DAY - 2).getRight(), MAX_UUID));
        // in fact, its just 255 uuids larger in a space of 2^128, which is small to 2^128-1 / 1440
        assert MAX_UUID.subtract(indexPairs.get(MINUTES_IN_DAY - 2).getRight()).subtract(stepSize).intValue() == 255;

        return indexPairs;
    }


    public static UUID fromBigInt(BigInteger bigInteger) {
        String bigIntegerAsHex = bigInteger.toString(16);
        String leftPadded = org.apache.commons.lang3.StringUtils.leftPad(bigIntegerAsHex, 32, "0");
        String withDashes = leftPadded.replaceAll("^(.{8})(.{4})(.{4})(.{4})(.{12})$", "$1-$2-$3-$4-$5");
        return UUID.fromString(withDashes);
    }

    static record UUIDRange(UUID left, UUID right) {
    }
}
