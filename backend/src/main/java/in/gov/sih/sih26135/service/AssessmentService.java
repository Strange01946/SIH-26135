package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateAssessmentRequest;
import in.gov.sih.sih26135.dto.request.UpdateAssessmentRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResponse;
import java.time.LocalDate;
import java.util.List;

public interface AssessmentService {

  AssessmentResponse getById(Long id);

  AssessmentResponse getByCode(String assessmentCode);

  List<AssessmentResponse> getAllAssessments();

  List<AssessmentResponse> getByCourseId(Long courseId);

  List<AssessmentResponse> getByBatchId(Long batchId);

  List<AssessmentResponse> getByProgramId(Long programId);

  List<AssessmentResponse> getByAssessmentTypeId(Long assessmentTypeId);

  List<AssessmentResponse> getByAssessmentDate(LocalDate assessmentDate);

  List<AssessmentResponse> getByLifecycleStatusId(Long lifecycleStatusId);

  AssessmentResponse createAssessment(CreateAssessmentRequest request);

  AssessmentResponse updateAssessment(Long id, UpdateAssessmentRequest request);

  void deleteAssessment(Long id);
}
