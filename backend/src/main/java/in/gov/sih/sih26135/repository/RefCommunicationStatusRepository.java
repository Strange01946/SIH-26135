package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefCommunicationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefCommunicationStatusRepository extends JpaRepository<RefCommunicationStatus, Long> {

  Optional<RefCommunicationStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);

  List<RefCommunicationStatus> findByIsSuccessFlag(Boolean isSuccessFlag);

  List<RefCommunicationStatus> findByIsFailureFlag(Boolean isFailureFlag);
}
