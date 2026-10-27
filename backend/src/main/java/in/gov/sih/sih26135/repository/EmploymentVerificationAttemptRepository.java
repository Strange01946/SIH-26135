package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.EmploymentVerificationAttempt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentVerificationAttemptRepository extends
    JpaRepository<EmploymentVerificationAttempt, Long> {

  Optional<EmploymentVerificationAttempt> findByEmploymentVerificationRequestIdAndAttemptNumber(
      Long requestId, Integer attemptNumber);

  boolean existsByEmploymentVerificationRequestIdAndAttemptNumber(Long requestId,
      Integer attemptNumber);

  List<EmploymentVerificationAttempt> findByEmploymentVerificationRequestId(Long requestId);

  List<EmploymentVerificationAttempt> findByEmploymentVerificationMethodId(Long methodId);

  List<EmploymentVerificationAttempt> findByEmploymentVerificationAttemptStatusId(Long statusId);

  List<EmploymentVerificationAttempt> findByAttemptedByUserId(Long attemptedByUserId);

  List<EmploymentVerificationAttempt> findByCommunicationLogId(Long communicationLogId);

  Page<EmploymentVerificationAttempt> findByEmploymentVerificationRequestId(Long requestId, Pageable pageable);

  Page<EmploymentVerificationAttempt> findByEmploymentVerificationMethodId(Long methodId, Pageable pageable);

  Page<EmploymentVerificationAttempt> findByEmploymentVerificationAttemptStatusId(Long statusId, Pageable pageable);
}
