package nl.ing.lovebird.sitemanagement.providercallback;

import com.datastax.driver.core.utils.UUIDs;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestContext
public class UserExternalIdRepositoryIntegrationTest {


    @Autowired
    private UserExternalIdRepository userExternalIdRepository;


    @BeforeEach
    void setUp() {
        userExternalIdRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userExternalIdRepository.deleteAll();
    }

    @Test
    void testSave_getById_andFindByExternalId() {
        UUID userId = UUIDs.random();
        String externalId1 = "external_id_1";
        String externalId2 = "external_id_2";

        // Save to repository
        userExternalIdRepository.save(new UserExternalId(userId, "STARLINGBANK", externalId1));
        userExternalIdRepository.save(new UserExternalId(userId, "MONZO", externalId2));

        // Read from repository
        assertThat(userExternalIdRepository.findById(new UserExternalId.Id(userId, "STARLINGBANK")).isPresent()).isTrue();
        assertThat(userExternalIdRepository.findById(new UserExternalId.Id(userId, "MONZO")).isPresent()).isTrue();
        assertThat(userExternalIdRepository.findById(new UserExternalId.Id(userId, "YODLEE")).isPresent()).isFalse();
        assertThat(userExternalIdRepository.findById(new UserExternalId.Id(UUID.randomUUID(), "STARLINGBANK")).isPresent()).isFalse();

        // Find by
        assertThat(userExternalIdRepository.findByProviderAndExternalUserId("STARLINGBANK", externalId1).isPresent()).isTrue();
        assertThat(userExternalIdRepository.findByProviderAndExternalUserId( "MONZO", externalId2).isPresent()).isTrue();
        assertThat(userExternalIdRepository.findByProviderAndExternalUserId("STARLINGBANK", externalId2).isPresent()).isFalse();
        assertThat(userExternalIdRepository.findByProviderAndExternalUserId("STARLINGBANK", UUID.randomUUID().toString()).isPresent()).isFalse();
    }

}
