package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Certification;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, Long> {

  Optional<Certification> findByCertificateNumber(String certificateNumber);

  boolean existsByCertificateNumber(String certificateNumber);

  List<Certification> findByTraineeId(Long traineeId);

  List<Certification> findByTrainingEnrollmentId(Long enrollmentId);

  List<Certification> findByCourseId(Long courseId);

  List<Certification> findByProgramId(Long programId);

  List<Certification> findByAssessmentResultId(Long assessmentResultId);

  List<Certification> findByCertificateStatusId(Long certificateStatusId);

  List<Certification> findByCertificateVerificationStatusId(Long certificateVerificationStatusId);

  List<Certification> findByIssueDate(LocalDate issueDate);
}
