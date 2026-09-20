package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SkillGapAssessment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillGapAssessmentRepository extends JpaRepository<SkillGapAssessment, Long> {

  Optional<SkillGapAssessment> findByAssessmentNumber(String assessmentNumber);

  boolean existsByAssessmentNumber(String assessmentNumber);

  List<SkillGapAssessment> findByTraineeId(Long traineeId);

  List<SkillGapAssessment> findByEnrollmentId(Long enrollmentId);

  List<SkillGapAssessment> findByCourseId(Long courseId);

  List<SkillGapAssessment> findByBatchId(Long batchId);

  List<SkillGapAssessment> findByJobRoleId(Long jobRoleId);

  List<SkillGapAssessment> findByJobPostingId(Long jobPostingId);

  List<SkillGapAssessment> findByEmploymentRecordId(Long employmentId);

  List<SkillGapAssessment> findByPlacementRecordId(Long placementId);

  List<SkillGapAssessment> findByTraineeAssessmentId(Long traineeAssessmentId);

  List<SkillGapAssessment> findByAssessmentResultId(Long assessmentResultId);

  List<SkillGapAssessment> findByCertificationId(Long certificationId);

  List<SkillGapAssessment> findBySurveyResponseId(Long surveyResponseId);

  List<SkillGapAssessment> findByFollowupTaskId(Long followupTaskId);

  List<SkillGapAssessment> findByEmploymentVerificationId(Long employmentVerificationId);

  List<SkillGapAssessment> findBySkillGapSourceId(Long skillGapSourceId);

  List<SkillGapAssessment> findBySkillGapAssessmentStatusId(Long statusId);

  List<SkillGapAssessment> findByAssessedOn(LocalDate assessedOn);

  List<SkillGapAssessment> findByTraineeIdAndAssessedOn(Long traineeId, LocalDate assessedOn);
}
