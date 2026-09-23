package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapAssessmentRequest;
import in.gov.sih.sih26135.dto.response.SkillGapAssessmentResponse;
import java.time.LocalDate;
import java.util.List;

public interface SkillGapAssessmentService {

  SkillGapAssessmentResponse createAssessment(CreateSkillGapAssessmentRequest request);

  SkillGapAssessmentResponse updateAssessment(Long id, UpdateSkillGapAssessmentRequest request);

  SkillGapAssessmentResponse getAssessmentById(Long id);

  SkillGapAssessmentResponse getAssessmentByNumber(String assessmentNumber);

  List<SkillGapAssessmentResponse> getAssessmentsByTrainee(Long traineeId);

  List<SkillGapAssessmentResponse> getAssessmentsByEnrollment(Long enrollmentId);

  List<SkillGapAssessmentResponse> getAssessmentsByCourse(Long courseId);

  List<SkillGapAssessmentResponse> getAssessmentsByBatch(Long batchId);

  List<SkillGapAssessmentResponse> getAssessmentsByJobRole(Long jobRoleId);

  List<SkillGapAssessmentResponse> getAssessmentsByJobPosting(Long jobPostingId);

  List<SkillGapAssessmentResponse> getAssessmentsByEmploymentRecord(Long employmentId);

  List<SkillGapAssessmentResponse> getAssessmentsByPlacementRecord(Long placementId);

  List<SkillGapAssessmentResponse> getAssessmentsByTraineeAssessment(Long traineeAssessmentId);

  List<SkillGapAssessmentResponse> getAssessmentsByAssessmentResult(Long assessmentResultId);

  List<SkillGapAssessmentResponse> getAssessmentsByCertification(Long certificationId);

  List<SkillGapAssessmentResponse> getAssessmentsBySurveyResponse(Long surveyResponseId);

  List<SkillGapAssessmentResponse> getAssessmentsByFollowupTask(Long followupTaskId);

  List<SkillGapAssessmentResponse> getAssessmentsByEmploymentVerification(Long employmentVerificationId);

  List<SkillGapAssessmentResponse> getAssessmentsBySource(Long sourceId);

  List<SkillGapAssessmentResponse> getAssessmentsByStatus(Long statusId);

  List<SkillGapAssessmentResponse> getAssessmentsByDate(LocalDate assessedOn);

  List<SkillGapAssessmentResponse> getAssessmentsByTraineeAndDate(Long traineeId, LocalDate assessedOn);

  void deleteAssessment(Long id);
}
