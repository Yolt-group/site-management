package nl.ing.lovebird.sitemanagement.configuration;

import nl.ing.lovebird.sitemanagement.exception.ErrorConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class ErrorConstantsTest {

    /**
     * Might look silly, but prevents conflicts if 2 features are being developed where an errocode was added with just the previous + 1.
     */
    @Test
    void testThatAllErrorCodesAreUnique() {
        long uniqueErrorCodes = Arrays.stream(ErrorConstants.values()).map(ErrorConstants::getCode)
                .distinct()
                .count();

        assertThat(uniqueErrorCodes).isEqualTo(ErrorConstants.values().length);
    }

}