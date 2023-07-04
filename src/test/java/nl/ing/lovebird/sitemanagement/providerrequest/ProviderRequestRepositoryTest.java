package nl.ing.lovebird.sitemanagement.providerrequest;

import com.datastax.driver.core.Session;
import nl.ing.lovebird.sitemanagement.configuration.IntegrationTestContext;
import nl.ing.lovebird.sitemanagement.usersite.UserSiteActionType;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper;
import nl.ing.lovebird.testsupport.cassandra.CassandraHelper.OpenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintViolationException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestContext
public class ProviderRequestRepositoryTest {

    @Autowired
    private ProviderRequestRepository providerRequestRepository;

    @Test
    void testSaveAndGet() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        ProviderRequest savedProviderRequest = new ProviderRequest(id, activityId, userId, UUID.randomUUID(), UserSiteActionType.USER_REFRESH);
        providerRequestRepository.saveValidated(savedProviderRequest);

        Optional<ProviderRequest> providerRequest = providerRequestRepository.get(userId, id);
        assertThat(providerRequest.get()).isEqualTo(savedProviderRequest);

        assertThat(providerRequestRepository.find(userId, activityId).get(0)).isEqualTo(savedProviderRequest);

    }

    @Test
    void testValidation() {
        assertThatThrownBy(() -> {
            ProviderRequest savedProviderRequest = new ProviderRequest(UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(), UserSiteActionType.USER_REFRESH);
            providerRequestRepository.saveValidated(savedProviderRequest);
        }).isInstanceOf(ConstraintViolationException.class);
    }
}
