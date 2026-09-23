package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSurveyQuestionRequest;
import in.gov.sih.sih26135.dto.request.UpdateSurveyQuestionRequest;
import in.gov.sih.sih26135.dto.response.SurveyQuestionResponse;
import in.gov.sih.sih26135.entity.SurveyQuestion;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SurveyQuestionMapper;
import in.gov.sih.sih26135.repository.SurveyQuestionRepository;
import in.gov.sih.sih26135.repository.SurveyResponseAnswerRepository;
import in.gov.sih.sih26135.service.SurveyQuestionService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SurveyQuestionServiceImpl implements SurveyQuestionService {

  private final SurveyQuestionRepository surveyQuestionRepository;
  private final SurveyResponseAnswerRepository surveyResponseAnswerRepository;
  private final SurveyQuestionMapper surveyQuestionMapper;

  public SurveyQuestionServiceImpl(
      SurveyQuestionRepository surveyQuestionRepository,
      SurveyResponseAnswerRepository surveyResponseAnswerRepository,
      SurveyQuestionMapper surveyQuestionMapper) {
    this.surveyQuestionRepository = surveyQuestionRepository;
    this.surveyResponseAnswerRepository = surveyResponseAnswerRepository;
    this.surveyQuestionMapper = surveyQuestionMapper;
  }

  @Override
  @Transactional
  public SurveyQuestionResponse createSurveyQuestion(CreateSurveyQuestionRequest request) {
    if (request == null) {
      throw new BadRequestException("Survey question creation request cannot be null");
    }
    if (request.getSurveyTemplateVersionId() == null) {
      throw new BadRequestException("Survey template version ID is required");
    }
    if (request.getQuestionCode() == null || request.getQuestionCode().isBlank()) {
      throw new BadRequestException("Question code is required");
    }
    if (request.getQuestionText() == null || request.getQuestionText().isBlank()) {
      throw new BadRequestException("Question text is required");
    }
    if (request.getQuestionTypeId() == null) {
      throw new BadRequestException("Question type ID is required");
    }
    if (request.getDisplayOrder() == null || request.getDisplayOrder() < 1) {
      throw new BadRequestException("Display order must be at least 1", "INVALID_DISPLAY_ORDER");
    }

    String code = request.getQuestionCode().trim();
    if (surveyQuestionRepository.findBySurveyTemplateVersionIdAndQuestionCode(request.getSurveyTemplateVersionId(), code).isPresent()) {
      throw new ConflictException("Question code already exists for this survey template version", "DUPLICATE_VERSION_QUESTION_CODE");
    }

    if (surveyQuestionRepository.findBySurveyTemplateVersionIdAndDisplayOrder(request.getSurveyTemplateVersionId(), request.getDisplayOrder()).isPresent()) {
      throw new ConflictException("Display order already exists for this survey template version", "DUPLICATE_VERSION_DISPLAY_ORDER");
    }

    SurveyQuestion question = surveyQuestionMapper.toEntity(request);
    question.setQuestionCode(code);
    question.setQuestionText(request.getQuestionText().trim());

    LocalDateTime now = LocalDateTime.now();
    question.setCreatedAt(now);
    question.setUpdatedAt(now);

    SurveyQuestion saved = surveyQuestionRepository.save(question);
    return surveyQuestionMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SurveyQuestionResponse updateSurveyQuestion(Long id, UpdateSurveyQuestionRequest request) {
    if (id == null) {
      throw new BadRequestException("Question ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Update request cannot be null");
    }

    SurveyQuestion question = surveyQuestionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyQuestion", "id"));

    if (request.getDisplayOrder() != null && !request.getDisplayOrder().equals(question.getDisplayOrder())) {
      if (request.getDisplayOrder() < 1) {
        throw new BadRequestException("Display order must be at least 1", "INVALID_DISPLAY_ORDER");
      }
      surveyQuestionRepository.findBySurveyTemplateVersionIdAndDisplayOrder(question.getSurveyTemplateVersionId(), request.getDisplayOrder())
          .filter(q -> !q.getId().equals(id))
          .ifPresent(q -> {
            throw new ConflictException("Display order already exists for this survey template version", "DUPLICATE_VERSION_DISPLAY_ORDER");
          });
      question.setDisplayOrder(request.getDisplayOrder());
    }

    if (request.getQuestionText() != null && !request.getQuestionText().isBlank()) {
      question.setQuestionText(request.getQuestionText().trim());
    }

    if (request.getQuestionTypeId() != null) {
      question.setQuestionTypeId(request.getQuestionTypeId());
    }

    if (request.getIsRequired() != null) {
      question.setIsRequired(request.getIsRequired());
    }

    question.setUpdatedAt(LocalDateTime.now());
    SurveyQuestion updated = surveyQuestionRepository.save(question);
    return surveyQuestionMapper.toResponse(updated);
  }

  @Override
  public SurveyQuestionResponse getSurveyQuestionById(Long id) {
    if (id == null) {
      throw new BadRequestException("Question ID is required");
    }
    SurveyQuestion question = surveyQuestionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyQuestion", "id"));
    return surveyQuestionMapper.toResponse(question);
  }

  @Override
  public SurveyQuestionResponse getSurveyQuestionByVersionAndCode(Long versionId, String questionCode) {
    if (versionId == null) {
      throw new BadRequestException("Survey template version ID is required");
    }
    if (questionCode == null || questionCode.isBlank()) {
      throw new BadRequestException("Question code is required");
    }
    SurveyQuestion question = surveyQuestionRepository.findBySurveyTemplateVersionIdAndQuestionCode(versionId, questionCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("SurveyQuestion", "versionId, questionCode"));
    return surveyQuestionMapper.toResponse(question);
  }

  @Override
  public SurveyQuestionResponse getSurveyQuestionByVersionAndDisplayOrder(Long versionId, Integer displayOrder) {
    if (versionId == null) {
      throw new BadRequestException("Survey template version ID is required");
    }
    if (displayOrder == null) {
      throw new BadRequestException("Display order is required");
    }
    SurveyQuestion question = surveyQuestionRepository.findBySurveyTemplateVersionIdAndDisplayOrder(versionId, displayOrder)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyQuestion", "versionId, displayOrder"));
    return surveyQuestionMapper.toResponse(question);
  }

  @Override
  public List<SurveyQuestionResponse> getQuestionsByVersion(Long versionId) {
    if (versionId == null) {
      throw new BadRequestException("Survey template version ID is required");
    }
    return surveyQuestionRepository.findBySurveyTemplateVersionId(versionId).stream()
        .map(surveyQuestionMapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyQuestionResponse> getQuestionsByVersionOrdered(Long versionId) {
    if (versionId == null) {
      throw new BadRequestException("Survey template version ID is required");
    }
    return surveyQuestionRepository.findBySurveyTemplateVersionIdOrderByDisplayOrderAsc(versionId).stream()
        .map(surveyQuestionMapper::toResponse)
        .toList();
  }

  @Override
  public List<SurveyQuestionResponse> getQuestionsByType(Long questionTypeId) {
    if (questionTypeId == null) {
      throw new BadRequestException("Question type ID is required");
    }
    return surveyQuestionRepository.findByQuestionTypeId(questionTypeId).stream()
        .map(surveyQuestionMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteSurveyQuestion(Long id) {
    if (id == null) {
      throw new BadRequestException("Question ID is required");
    }
    SurveyQuestion question = surveyQuestionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SurveyQuestion", "id"));

    if (!surveyResponseAnswerRepository.findBySurveyQuestionId(id).isEmpty()) {
      throw new ConflictException("Cannot delete survey question with associated response answers", "QUESTION_HAS_ANSWERS");
    }

    surveyQuestionRepository.delete(question);
  }
}
