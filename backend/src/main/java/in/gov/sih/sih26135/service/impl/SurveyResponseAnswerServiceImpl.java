package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSurveyResponseAnswerRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyResponseAnswerRequest;
import in.gov.sih.sih26135.dto.response.SurveyResponseAnswerResponse;
import in.gov.sih.sih26135.entity.SurveyQuestion;
import in.gov.sih.sih26135.entity.SurveyResponse;
import in.gov.sih.sih26135.entity.SurveyResponseAnswer;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SurveyResponseAnswerMapper;
import in.gov.sih.sih26135.repository.SurveyQuestionRepository;
import in.gov.sih.sih26135.repository.SurveyResponseAnswerRepository;
import in.gov.sih.sih26135.repository.SurveyResponseRepository;
import in.gov.sih.sih26135.service.SurveyResponseAnswerService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SurveyResponseAnswerServiceImpl implements SurveyResponseAnswerService {

  private static final Long STATUS_SUBMITTED_ID = 3L;

  private final SurveyResponseAnswerRepository surveyResponseAnswerRepository;
  private final SurveyResponseRepository surveyResponseRepository;
  private final SurveyQuestionRepository surveyQuestionRepository;
  private final SurveyResponseAnswerMapper surveyResponseAnswerMapper;

  public SurveyResponseAnswerServiceImpl(
      SurveyResponseAnswerRepository surveyResponseAnswerRepository,
      SurveyResponseRepository surveyResponseRepository,
      SurveyQuestionRepository surveyQuestionRepository,
      SurveyResponseAnswerMapper surveyResponseAnswerMapper) {
    this.surveyResponseAnswerRepository = surveyResponseAnswerRepository;
    this.surveyResponseRepository = surveyResponseRepository;
    this.surveyQuestionRepository = surveyQuestionRepository;
    this.surveyResponseAnswerMapper = surveyResponseAnswerMapper;
  }

  @Override
  @Transactional
  public SurveyResponseAnswerResponse createSurveyResponseAnswer(CreateSurveyResponseAnswerRequest request) {
    if (request == null) {
      throw new BadRequestException("Survey response answer creation request cannot be null");
    }
    if (request.getSurveyResponseId() == null) {
      throw new BadRequestException("Survey response ID is required");
    }
    if (request.getSurveyQuestionId() == null) {
      throw new BadRequestException("Survey question ID is required");
    }

    if (surveyResponseAnswerRepository.existsBySurveyResponseIdAndSurveyQuestionId(request.getSurveyResponseId(), request.getSurveyQuestionId())) {
      throw new ConflictException("Answer already exists for this survey response and question", "DUPLICATE_RESPONSE_ANSWER");
    }

    SurveyResponse response = surveyResponseRepository.findById(request.getSurveyResponseId())
        .orElseThrow(() -> new ResourceNotFoundException("SurveyResponse", "surveyResponseId"));

    if (STATUS_SUBMITTED_ID.equals(response.getSurveyResponseStatusId())) {
      throw new BadRequestException("Cannot add answers to an already submitted survey response", "RESPONSE_ALREADY_SUBMITTED");
    }

    SurveyQuestion question = surveyQuestionRepository.findById(request.getSurveyQuestionId())
        .orElseThrow(() -> new ResourceNotFoundException("SurveyQuestion", "surveyQuestionId"));

    if (!question.getSurveyTemplateVersionId().equals(response.getSurveyTemplateVersionId())) {
      throw new BadRequestException("Survey question does not belong to the template version of this survey response", "QUESTION_VERSION_MISMATCH");
    }

    boolean hasText = request.getAnswerText() != null && !request.getAnswerText().isBlank();
    if (request.getSelectedOptionId() == null && !hasText
        && request.getNumericValue() == null && request.getBooleanValue() == null && request.getDateValue() == null) {
      throw new BadRequestException("At least one answer value must be provided", "EMPTY_ANSWER_VALUE");
    }

    SurveyResponseAnswer answer = surveyResponseAnswerMapper.toEntity(request, response, question);
    if (hasText) {
      answer.setAnswerText(request.getAnswerText().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    answer.setCreatedAt(now);
    answer.setUpdatedAt(now);

    SurveyResponseAnswer saved = surveyResponseAnswerRepository.save(answer);
    return surveyResponseAnswerMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SurveyResponseAnswerResponse updateSurveyResponseAnswer(Long id, UpdateSurveyResponseAnswerRequest request) {
    if (id == null) {
      throw new BadRequestException("Answer ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Update request cannot be null");
    }

    SurveyResponseAnswer answer = surveyResponseAnswerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyResponseAnswer", "id"));

    SurveyResponse response = answer.getSurveyResponse();
    if (STATUS_SUBMITTED_ID.equals(response.getSurveyResponseStatusId())) {
      throw new BadRequestException("Cannot update answers of an already submitted survey response", "RESPONSE_ALREADY_SUBMITTED");
    }

    if (request.getSelectedOptionId() != null) {
      answer.setSelectedOptionId(request.getSelectedOptionId());
    }
    if (request.getAnswerText() != null) {
      answer.setAnswerText(request.getAnswerText().trim());
    }
    if (request.getNumericValue() != null) {
      answer.setNumericValue(request.getNumericValue());
    }
    if (request.getBooleanValue() != null) {
      answer.setBooleanValue(request.getBooleanValue());
    }
    if (request.getDateValue() != null) {
      answer.setDateValue(request.getDateValue());
    }

    answer.setUpdatedAt(LocalDateTime.now());
    SurveyResponseAnswer updated = surveyResponseAnswerRepository.save(answer);
    return surveyResponseAnswerMapper.toResponse(updated);
  }

  @Override
  public SurveyResponseAnswerResponse getSurveyResponseAnswerById(Long id) {
    if (id == null) {
      throw new BadRequestException("Answer ID is required");
    }
    SurveyResponseAnswer answer = surveyResponseAnswerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyResponseAnswer", "id"));
    return surveyResponseAnswerMapper.toResponse(answer);
  }

  @Override
  public SurveyResponseAnswerResponse getAnswerByResponseAndQuestion(Long surveyResponseId, Long surveyQuestionId) {
    if (surveyResponseId == null) {
      throw new BadRequestException("Survey response ID is required");
    }
    if (surveyQuestionId == null) {
      throw new BadRequestException("Survey question ID is required");
    }
    SurveyResponseAnswer answer = surveyResponseAnswerRepository.findBySurveyResponseIdAndSurveyQuestionId(surveyResponseId, surveyQuestionId)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyResponseAnswer", "surveyResponseId, surveyQuestionId"));
    return surveyResponseAnswerMapper.toResponse(answer);
  }

  @Override
  public List<SurveyResponseAnswerResponse> getAnswersByResponse(Long surveyResponseId) {
    if (surveyResponseId == null) {
      throw new BadRequestException("Survey response ID is required");
    }
    return surveyResponseAnswerRepository.findBySurveyResponseId(surveyResponseId).stream()
        .map(surveyResponseAnswerMapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyResponseAnswerResponse> getAnswersByQuestion(Long surveyQuestionId) {
    if (surveyQuestionId == null) {
      throw new BadRequestException("Survey question ID is required");
    }
    return surveyResponseAnswerRepository.findBySurveyQuestionId(surveyQuestionId).stream()
        .map(surveyResponseAnswerMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteSurveyResponseAnswer(Long id) {
    if (id == null) {
      throw new BadRequestException("Answer ID is required");
    }
    SurveyResponseAnswer answer = surveyResponseAnswerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyResponseAnswer", "id"));

    if (STATUS_SUBMITTED_ID.equals(answer.getSurveyResponse().getSurveyResponseStatusId())) {
      throw new ConflictException("Cannot delete answer of an already submitted survey response", "CANNOT_DELETE_SUBMITTED_ANSWER");
    }

    surveyResponseAnswerRepository.delete(answer);
  }
}
