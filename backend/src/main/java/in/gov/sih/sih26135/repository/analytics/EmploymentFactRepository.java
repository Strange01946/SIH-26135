package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EmploymentFact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentFactRepository extends JpaRepository<EmploymentFact, Long> {

  Optional<EmploymentFact> findByEmploymentNumber(String employmentNumber);

  List<EmploymentFact> findByTraineeId(Long traineeId);

  Page<EmploymentFact> findByTraineeId(Long traineeId, Pageable pageable);

  List<EmploymentFact> findByEmployerId(Long employerId);

  Page<EmploymentFact> findByEmployerId(Long employerId, Pageable pageable);

  List<EmploymentFact> findByEnrollmentId(Long enrollmentId);

  Page<EmploymentFact> findByEnrollmentId(Long enrollmentId, Pageable pageable);

  List<EmploymentFact> findByPlacementId(Long placementId);

  Page<EmploymentFact> findByPlacementId(Long placementId, Pageable pageable);

  List<EmploymentFact> findByJobRoleId(Long jobRoleId);

  Page<EmploymentFact> findByJobRoleId(Long jobRoleId, Pageable pageable);

  List<EmploymentFact> findByIsCurrentTrue();

  Page<EmploymentFact> findByIsCurrentTrue(Pageable pageable);

  List<EmploymentFact> findByEngagementTypeId(Long engagementTypeId);

  Page<EmploymentFact> findByEngagementTypeId(Long engagementTypeId, Pageable pageable);

  List<EmploymentFact> findByTraineeDistrictId(Long traineeDistrictId);

  Page<EmploymentFact> findByTraineeDistrictId(Long traineeDistrictId, Pageable pageable);
}
