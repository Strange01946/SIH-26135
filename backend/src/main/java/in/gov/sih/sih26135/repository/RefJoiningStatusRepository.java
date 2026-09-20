package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefJoiningStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefJoiningStatusRepository extends JpaRepository<RefJoiningStatus, Long> {

  Optional<RefJoiningStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);

  List<RefJoiningStatus> findByIsJoinedFlag(Boolean isJoinedFlag);
}
