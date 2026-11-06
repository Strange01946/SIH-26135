package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EmploymentExitFact;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentExitFactRepository extends
    JpaRepository<EmploymentExitFact, Long> {

  List<EmploymentExitFact> findByTraineeId(Long traineeId);

  Page<EmploymentExitFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<EmploymentExitFact> findByEmploymentId(Long employmentId);

  Page<EmploymentExitFact> findByEmploymentId(Long employmentId, Pageable pageable);

  List<EmploymentExitFact> findByEmploymentExitReasonId(Long employmentExitReasonId);

  Page<EmploymentExitFact> findByEmploymentExitReasonId(Long employmentExitReasonId, Pageable pageable);

  List<EmploymentExitFact> findBySeparationNatureId(Long separationNatureId);

  Page<EmploymentExitFact> findBySeparationNatureId(Long separationNatureId, Pageable pageable);

  List<EmploymentExitFact> findByIsVoluntaryFlag(Boolean isVoluntaryFlag);

  Page<EmploymentExitFact> findByIsVoluntaryFlag(Boolean isVoluntaryFlag, Pageable pageable);

  List<EmploymentExitFact> findByIsInvoluntaryFlag(Boolean isInvoluntaryFlag);

  Page<EmploymentExitFact> findByIsInvoluntaryFlag(Boolean isInvoluntaryFlag, Pageable pageable);
}
