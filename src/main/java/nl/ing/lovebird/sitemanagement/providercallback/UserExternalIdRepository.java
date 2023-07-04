package nl.ing.lovebird.sitemanagement.providercallback;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserExternalIdRepository extends CrudRepository<UserExternalId, UserExternalId.Id> {

    Optional<UserExternalId> findByProviderAndExternalUserId(String provider, String externalUserId);
}
