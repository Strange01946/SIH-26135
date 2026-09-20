package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.SalaryProgressionFact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaryProgressionFactRepository extends
    JpaRepository<SalaryProgressionFact, Long> {

  List<SalaryProgressionFact> findByTraineeId(Long traineeId);

  List<SalaryProgressionFact> findByEnrollmentId(Long enrollmentId);

  List<SalaryProgressionFact> findByCourseId(Long courseId);

  List<SalaryProgressionFact> findByProviderId(Long providerId);

  List<SalaryProgressionFact> findByEngagementTypeId(Long engagementTypeId);
}
