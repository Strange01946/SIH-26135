package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EmploymentRetentionFact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentRetentionFactRepository extends
    JpaRepository<EmploymentRetentionFact, Long> {

  List<EmploymentRetentionFact> findByTraineeId(Long traineeId);

  List<EmploymentRetentionFact> findByEnrollmentId(Long enrollmentId);

  List<EmploymentRetentionFact> findByEngagementTypeId(Long engagementTypeId);

  List<EmploymentRetentionFact> findByIsCurrentTrue();
}
