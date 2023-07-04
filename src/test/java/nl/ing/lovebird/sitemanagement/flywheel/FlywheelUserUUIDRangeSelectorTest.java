package nl.ing.lovebird.sitemanagement.flywheel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class FlywheelUserUUIDRangeSelectorTest {


    static FlywheelUserUUIDRangeSelector flywheelUserUUIDRangeSelector = new FlywheelUserUUIDRangeSelector();


    @Test
    public void given_1refreshADay_then_ItShouldStartWith0AndEndWithMaxUUID_and_allStepsInBetweenAreChained() {
        FlywheelUserUUIDRangeSelector.UUIDRange firstMinuteOfDayRange = flywheelUserUUIDRangeSelector.getUUIRange(1, LocalTime.MIN);
        assertThat(firstMinuteOfDayRange.left()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        assertThat(firstMinuteOfDayRange.right()).isEqualTo(UUID.fromString("002d82d8-2d82-d82d-82d8-2d82d82d82d8"));

        // For the reader: (further more generic for each minute in a day)
        FlywheelUserUUIDRangeSelector.UUIDRange secondMinute = flywheelUserUUIDRangeSelector.getUUIRange(1, LocalTime.MIN.plusMinutes(1));
        assertThat(secondMinute.left()).isEqualTo(UUID.fromString("002d82d8-2d82-d82d-82d8-2d82d82d82d8"));
        assertThat(secondMinute.right()).isEqualTo(UUID.fromString("005b05b0-5b05-b05b-05b0-5b05b05b05b0"));

        // Same as above but for every minute of the day.
        for (LocalDateTime localDateTime = LocalDateTime.MIN.plusSeconds(10); localDateTime.getHour() == 23 && localDateTime.getMinute() == 58; localDateTime = localDateTime.plusMinutes(1)) {
            FlywheelUserUUIDRangeSelector.UUIDRange uuiRange = flywheelUserUUIDRangeSelector.getUUIRange(1, localDateTime.toLocalTime());
            FlywheelUserUUIDRangeSelector.UUIDRange nextMinute = flywheelUserUUIDRangeSelector.getUUIRange(1, localDateTime.toLocalTime().plusMinutes(1));
            assertThat(uuiRange.right()).isEqualTo(nextMinute.left());
        }
        FlywheelUserUUIDRangeSelector.UUIDRange lastMinuteOfDay = flywheelUserUUIDRangeSelector.getUUIRange(1, LocalTime.of(23, 59, 10));
        assertThat(lastMinuteOfDay.left()).isEqualTo(UUID.fromString("ffd27d27-d27d-27d2-7d27-d27d27d27c28"));
        assertThat(lastMinuteOfDay.right()).isEqualTo(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));

    }

    /**
     * Generalized above {@link #given_1refreshADay_then_ItShouldStartWith0AndEndWithMaxUUID_and_allStepsInBetweenAreChained}
     * @param nrOfRefreshesPerDay
     */
    @ParameterizedTest
    @ValueSource(ints = {1,2,3,4})
    public void given_NrefreshADay_then_ItShouldStartWith0AndEndWithMaxUUID_and_allStepsInBetweenAreChained(int nrOfRefreshesPerDay) {
        FlywheelUserUUIDRangeSelector.UUIDRange firstMinuteOfDayRange = flywheelUserUUIDRangeSelector.getUUIRange(nrOfRefreshesPerDay, LocalTime.MIN);
        assertThat(firstMinuteOfDayRange.left()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000000"));

        // Same as above but for every minute of the day.
        for (LocalDateTime localDateTime = LocalDateTime.MIN.plusSeconds(10); localDateTime.getHour() == 23 && localDateTime.getMinute() == 58; localDateTime = localDateTime.plusMinutes(1)) {
            FlywheelUserUUIDRangeSelector.UUIDRange uuiRange = flywheelUserUUIDRangeSelector.getUUIRange(nrOfRefreshesPerDay, localDateTime.toLocalTime());
            FlywheelUserUUIDRangeSelector.UUIDRange nextMinute = flywheelUserUUIDRangeSelector.getUUIRange(nrOfRefreshesPerDay, localDateTime.toLocalTime().plusMinutes(1));
            assertThat(uuiRange.right()).isEqualTo(nextMinute.left());

        }
        FlywheelUserUUIDRangeSelector.UUIDRange lastMinuteOfDay = flywheelUserUUIDRangeSelector.getUUIRange(nrOfRefreshesPerDay, LocalTime.of(23, 59, 10));
        assertThat(lastMinuteOfDay.right()).isEqualTo(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2,3,4})
    public void given_NrefreshADay_And_ARandomUserId_then_thisUserIdShouldBeWithinRangeNTimes(int nrOfRefreshesPerDay) {
        UUID randomUserId = UUID.randomUUID();
        int hits = 0;
        for (LocalDateTime localDateTime = LocalDateTime.MIN.plusSeconds(10); localDateTime.getDayOfMonth() == LocalDateTime.MIN.getDayOfMonth(); localDateTime = localDateTime.plusMinutes(1)) {
            FlywheelUserUUIDRangeSelector.UUIDRange uuidRange = flywheelUserUUIDRangeSelector.getUUIRange(nrOfRefreshesPerDay, localDateTime.toLocalTime());
            if (isInBetween(randomUserId, uuidRange)) {
                hits++;
            }
        }
        assertThat(hits).isEqualTo(nrOfRefreshesPerDay);
    }

    private boolean isInBetween(UUID randomUserId, FlywheelUserUUIDRangeSelector.UUIDRange uuidRange) {
        return uuidCompare(randomUserId, uuidRange.left()) >= 0 && uuidCompare(randomUserId, uuidRange.right()) == -1;
    }

    /**
     *  This next assertion is really wonky... because comparison of UUID in postgres and Java works in a different way.
     *  In postgress uuid('802d82d8-2d82-d82d-82d8-2d82d82d8258') > uuid('7fffffff-ffff-ffff-ffff-ffffffffff80') = true.
     *  In java, UUID.fromString("802d82d8-2d82-d82d-82d8-2d82d82d8258").compareTo(UUID.fromString("7fffffff-ffff-ffff-ffff-ffffffffff80")) = -1
     *  This is because in java the most significant bits (repr. as long) flip to negative at some point, making it small than.
     *  https://bugs.openjdk.java.net/browse/JDK-7025832
     */
    public static int uuidCompare(UUID first, UUID second) {
        return new BigInteger(first.toString().replace("-", ""), 16).compareTo(new BigInteger(second.toString().replace("-", ""), 16));
    }
}