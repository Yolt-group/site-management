package nl.ing.lovebird.sitemanagement.consentsession;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentSessionRepository extends JpaRepository<ConsentSession, ConsentSession.ConsentSessionId> {

    Optional<ConsentSession> findByUserIdAndStateId(UUID userId, UUID stateId);

    Optional<ConsentSession> findByStateId(UUID stateId);

}
