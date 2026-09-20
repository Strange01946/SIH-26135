package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EmploymentFact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentFactRepository extends JpaRepository<EmploymentFact, Long> {

  Optional<EmploymentFact> findByEmploymentNumber(String employmentNumber);

  List<EmploymentFact> findByTraineeId(Long traineeId);

  List<EmploymentFact> findByEmployerId(Long employerId);

  List<EmploymentFact> findByEnrollmentId(Long enrollmentId);

  List<EmploymentFact> findByPlacementId(Long placementId);

  List<EmploymentFact> findByJobRoleId(Long jobRoleId);

  List<EmploymentFact> findByIsCurrentTrue();

  List<EmploymentFact> findByEngagementTypeId(Long engagementTypeId);

  List<EmploymentFact> findByTraineeDistrictId(Long traineeDistrictId);
}
