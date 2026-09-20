package in.gov.sih.sih26135.repository.analytics;

import in.gov.sih.sih26135.entity.analytics.EmploymentVerificationFact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentVerificationFactRepository extends
    JpaRepository<EmploymentVerificationFact, Long> {

  Optional<EmploymentVerificationFact> findByVerificationNumber(String verificationNumber);

  List<EmploymentVerificationFact> findByTraineeId(Long traineeId);

  List<EmploymentVerificationFact> findByEmploymentId(Long employmentId);

  List<EmploymentVerificationFact> findByPlacementId(Long placementId);

  List<EmploymentVerificationFact> findByEmployerId(Long employerId);

  List<EmploymentVerificationFact> findByRecordVerificationStatusId(Long statusId);

  List<EmploymentVerificationFact> findByEmploymentVerificationMethodId(Long methodId);

  List<EmploymentVerificationFact> findByIsCurrentTrue();

  List<EmploymentVerificationFact> findByIsVerifiedFlag(Boolean isVerifiedFlag);
}
