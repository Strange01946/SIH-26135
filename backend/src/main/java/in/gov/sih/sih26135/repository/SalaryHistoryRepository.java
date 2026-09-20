package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SalaryHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaryHistoryRepository extends JpaRepository<SalaryHistory, Long> {

  List<SalaryHistory> findByEmploymentRecordId(Long employmentRecordId);

  List<SalaryHistory> findByTraineeId(Long traineeId);

  List<SalaryHistory> findByEffectiveFrom(LocalDate effectiveFrom);

  List<SalaryHistory> findByEffectiveTo(LocalDate effectiveTo);

  List<SalaryHistory> findByObservationMonthOffset(Integer observationMonthOffset);

  Optional<SalaryHistory> findByEmploymentRecordIdAndEffectiveFrom(Long employmentRecordId, LocalDate effectiveFrom);

  Optional<SalaryHistory> findByEmploymentRecordIdAndObservationMonthOffset(Long employmentRecordId, Integer observationMonthOffset);

  List<SalaryHistory> findByEmploymentInfoSourceId(Long employmentInfoSourceId);

  List<SalaryHistory> findByRecordVerificationStatusId(Long recordVerificationStatusId);
}
