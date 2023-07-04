package nl.ing.lovebird.sitemanagement.providerclient;

import nl.ing.lovebird.clienttokens.ClientToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.Mockito.mock;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class EncryptionDetailsServiceTest {

    private static final UUID TEST_SITE_UUID = UUID.fromString("0285a320-7dca-11e8-adc0-fa7ae01bbebc");
    private static final ClientToken CLIENT_TOKEN = mock(ClientToken.class);

    @Mock
    FormProviderRestClient formProviderRestClient;

    @InjectMocks
    private EncryptionDetailsService encryptionDetailsService;

    @Test
    void testGetCachedPublicBudgetInsightKey_testSite() {
        ReflectionTestUtils.setField(encryptionDetailsService, "cacheEncryptionDetails", true);
        encryptionDetailsService.getEncryptionDetails(TEST_SITE_UUID, "BUDGET_INSIGHT", CLIENT_TOKEN);
    }

}
