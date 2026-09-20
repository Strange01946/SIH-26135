package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.EmploymentExitEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentExitEventRepository extends JpaRepository<EmploymentExitEvent, Long> {

  Optional<EmploymentExitEvent> findByEmploymentRecordId(Long employmentId);

  boolean existsByEmploymentRecordId(Long employmentId);

  List<EmploymentExitEvent> findByTraineeId(Long traineeId);

  List<EmploymentExitEvent> findByEnrollmentId(Long enrollmentId);

  List<EmploymentExitEvent> findBySeparationDate(LocalDate separationDate);

  List<EmploymentExitEvent> findByEmploymentExitReasonId(Long exitReasonId);

  List<EmploymentExitEvent> findBySeparationNatureId(Long separationNatureId);

  List<EmploymentExitEvent> findByEmploymentInfoSourceId(Long sourceId);

  List<EmploymentExitEvent> findByRecordVerificationStatusId(Long statusId);

  List<EmploymentExitEvent> findByVerifiedByUserId(Long verifiedByUserId);

  List<EmploymentExitEvent> findByTraineeIdAndSeparationDate(Long traineeId,
      LocalDate separationDate);
}
