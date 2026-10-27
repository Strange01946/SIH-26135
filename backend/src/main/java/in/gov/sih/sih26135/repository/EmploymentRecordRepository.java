package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.EmploymentRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentRecordRepository extends JpaRepository<EmploymentRecord, Long> {

  Optional<EmploymentRecord> findByEmploymentNumber(String employmentNumber);

  boolean existsByEmploymentNumber(String employmentNumber);

  List<EmploymentRecord> findByTraineeId(Long traineeId);

  List<EmploymentRecord> findByEmployerId(Long employerId);

  Optional<EmploymentRecord> findByPlacementRecordId(Long placementRecordId);

  List<EmploymentRecord> findByEmployerBranchId(Long employerBranchId);

  List<EmploymentRecord> findByJobRoleId(Long jobRoleId);

  List<EmploymentRecord> findByEngagementTypeId(Long engagementTypeId);

  List<EmploymentRecord> findByEmploymentSpellStatusId(Long employmentSpellStatusId);

  List<EmploymentRecord> findByIsCurrent(Boolean isCurrent);

  List<EmploymentRecord> findByTraineeIdAndIsCurrent(Long traineeId, Boolean isCurrent);

  List<EmploymentRecord> findByStartDate(LocalDate startDate);

  List<EmploymentRecord> findByEndDate(LocalDate endDate);

  List<EmploymentRecord> findByRecordVerificationStatusId(Long recordVerificationStatusId);

  List<EmploymentRecord> findByDeletedAtIsNull();

  Page<EmploymentRecord> findByDeletedAtIsNull(Pageable pageable);

  Page<EmploymentRecord> findByTraineeIdAndDeletedAtIsNull(Long traineeId, Pageable pageable);

  Page<EmploymentRecord> findByEmployerIdAndDeletedAtIsNull(Long employerId, Pageable pageable);

  Page<EmploymentRecord> findByEmploymentSpellStatusIdAndDeletedAtIsNull(Long employmentSpellStatusId, Pageable pageable);

  Page<EmploymentRecord> findByTraineeIdAndIsCurrentAndDeletedAtIsNull(Long traineeId, Boolean isCurrent, Pageable pageable);
}
