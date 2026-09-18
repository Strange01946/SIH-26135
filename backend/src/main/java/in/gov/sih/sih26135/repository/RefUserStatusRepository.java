package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefUserStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefUserStatusRepository extends JpaRepository<RefUserStatus, Long> {

  Optional<RefUserStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);
}
