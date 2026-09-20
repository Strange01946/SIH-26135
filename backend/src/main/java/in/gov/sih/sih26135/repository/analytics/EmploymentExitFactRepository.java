package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EmploymentExitFact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentExitFactRepository extends
    JpaRepository<EmploymentExitFact, Long> {

  List<EmploymentExitFact> findByTraineeId(Long traineeId);

  List<EmploymentExitFact> findByEmploymentId(Long employmentId);

  List<EmploymentExitFact> findByEmploymentExitReasonId(Long employmentExitReasonId);

  List<EmploymentExitFact> findBySeparationNatureId(Long separationNatureId);

  List<EmploymentExitFact> findByIsVoluntaryFlag(Boolean isVoluntaryFlag);

  List<EmploymentExitFact> findByIsInvoluntaryFlag(Boolean isInvoluntaryFlag);
}
