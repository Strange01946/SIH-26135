package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.EmploymentVerificationEvidence;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmploymentVerificationEvidenceRepository extends
    JpaRepository<EmploymentVerificationEvidence, Long> {

  Optional<EmploymentVerificationEvidence> findByEmploymentVerificationIdAndDocumentReferenceCode(
      Long verificationId, String documentReferenceCode);

  boolean existsByEmploymentVerificationIdAndDocumentReferenceCode(Long verificationId,
      String documentReferenceCode);

  List<EmploymentVerificationEvidence> findByEmploymentVerificationId(Long verificationId);

  List<EmploymentVerificationEvidence> findByEmploymentVerificationAttemptId(Long attemptId);

  List<EmploymentVerificationEvidence> findByEmploymentVerificationEvidenceTypeId(Long typeId);

  List<EmploymentVerificationEvidence> findByUploadedByUserId(Long uploadedByUserId);
}
