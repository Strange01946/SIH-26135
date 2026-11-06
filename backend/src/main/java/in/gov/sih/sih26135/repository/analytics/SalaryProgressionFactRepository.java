package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.SalaryProgressionFact;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaryProgressionFactRepository extends
    JpaRepository<SalaryProgressionFact, Long> {

  List<SalaryProgressionFact> findByTraineeId(Long traineeId);

  Page<SalaryProgressionFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<SalaryProgressionFact> findByEnrollmentId(Long enrollmentId);

  Page<SalaryProgressionFact> findByEnrollmentId(Long enrollmentId, Pageable pageable);

  List<SalaryProgressionFact> findByCourseId(Long courseId);

  Page<SalaryProgressionFact> findByCourseId(Long courseId, Pageable pageable);

  List<SalaryProgressionFact> findByProviderId(Long providerId);

  Page<SalaryProgressionFact> findByProviderId(Long providerId, Pageable pageable);

  List<SalaryProgressionFact> findByEngagementTypeId(Long engagementTypeId);

  Page<SalaryProgressionFact> findByEngagementTypeId(Long engagementTypeId, Pageable pageable);
}
