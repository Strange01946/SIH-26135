package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EmploymentRetentionFact;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentRetentionFactRepository extends
    JpaRepository<EmploymentRetentionFact, Long> {

  List<EmploymentRetentionFact> findByTraineeId(Long traineeId);

  Page<EmploymentRetentionFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<EmploymentRetentionFact> findByEnrollmentId(Long enrollmentId);

  Page<EmploymentRetentionFact> findByEnrollmentId(Long enrollmentId, Pageable pageable);

  List<EmploymentRetentionFact> findByEngagementTypeId(Long engagementTypeId);

  Page<EmploymentRetentionFact> findByEngagementTypeId(Long engagementTypeId, Pageable pageable);

  List<EmploymentRetentionFact> findByIsCurrentTrue();

  Page<EmploymentRetentionFact> findByIsCurrentTrue(Pageable pageable);
}
