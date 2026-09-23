package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SurveyResponseFactResponse;
import in.gov.sih.sih26135.entity.analytics.SurveyResponseFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SurveyResponseFactMapper;
import in.gov.sih.sih26135.repository.analytics.SurveyResponseFactRepository;
import in.gov.sih.sih26135.service.SurveyResponseFactService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SurveyResponseFactServiceImpl implements SurveyResponseFactService {

  private final SurveyResponseFactRepository repository;
  private final SurveyResponseFactMapper mapper;

  public SurveyResponseFactServiceImpl(
      SurveyResponseFactRepository repository,
      SurveyResponseFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SurveyResponseFactResponse> getAllSurveyResponses() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SurveyResponseFactResponse getSurveyResponseById(Long surveyResponseId) {
    if (surveyResponseId == null) {
      throw new BadRequestException("Survey response ID is required");
    }
    SurveyResponseFact entity = repository.findById(surveyResponseId)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyResponseFact", "surveyResponseId"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<SurveyResponseFactResponse> getSurveyResponsesByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseFactResponse> getSurveyResponsesBySurveyId(Long surveyId) {
    if (surveyId == null) {
      throw new BadRequestException("Survey ID is required");
    }
    return repository.findBySurveyId(surveyId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseFactResponse> getSurveyResponsesBySurveyPurposeId(Long surveyPurposeId) {
    if (surveyPurposeId == null) {
      throw new BadRequestException("Survey purpose ID is required");
    }
    return repository.findBySurveyPurposeId(surveyPurposeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseFactResponse> getSurveyResponsesBySurveyPurposeCode(String surveyPurposeCode) {
    if (surveyPurposeCode == null || surveyPurposeCode.isBlank()) {
      throw new BadRequestException("Survey purpose code is required");
    }
    return repository.findBySurveyPurposeCode(surveyPurposeCode.trim()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseFactResponse> getSurveyResponsesByProgramId(Long programId) {
    if (programId == null) {
      throw new BadRequestException("Program ID is required");
    }
    return repository.findByProgramId(programId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseFactResponse> getSurveyResponsesByCourseId(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return repository.findByCourseId(courseId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseFactResponse> getSurveyResponsesByBatchId(Long batchId) {
    if (batchId == null) {
      throw new BadRequestException("Batch ID is required");
    }
    return repository.findByBatchId(batchId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseFactResponse> getSurveyResponsesByEnrollmentId(Long enrollmentId) {
    if (enrollmentId == null) {
      throw new BadRequestException("Enrollment ID is required");
    }
    return repository.findByEnrollmentId(enrollmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseFactResponse> getSurveyResponsesByFollowupTaskId(Long followupTaskId) {
    if (followupTaskId == null) {
      throw new BadRequestException("Followup task ID is required");
    }
    return repository.findByFollowupTaskId(followupTaskId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseFactResponse> getSurveyResponsesByIsSubmittedFlag(Boolean isSubmittedFlag) {
    if (isSubmittedFlag == null) {
      throw new BadRequestException("isSubmittedFlag is required");
    }
    return repository.findByIsSubmittedFlag(isSubmittedFlag).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
