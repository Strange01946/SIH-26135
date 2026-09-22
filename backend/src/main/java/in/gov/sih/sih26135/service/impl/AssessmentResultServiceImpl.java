package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.request.UpdateAssessmentResultRequest;
import in.gov.sih.sih26135.dto.response.AssessmentResultResponse;
import in.gov.sih.sih26135.entity.AssessmentResult;
import in.gov.sih.sih26135.entity.Certification;
import in.gov.sih.sih26135.entity.RefAssessmentOutcome;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TraineeAssessment;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.AssessmentResultMapper;
import in.gov.sih.sih26135.repository.AssessmentResultRepository;
import in.gov.sih.sih26135.repository.CertificationRepository;
import in.gov.sih.sih26135.repository.RefAssessmentOutcomeRepository;
import in.gov.sih.sih26135.repository.TraineeAssessmentRepository;
import in.gov.sih.sih26135.service.AssessmentResultService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssessmentResultServiceImpl implements AssessmentResultService {

  private final AssessmentResultRepository assessmentResultRepository;
  private final TraineeAssessmentRepository traineeAssessmentRepository;
  private final RefAssessmentOutcomeRepository refAssessmentOutcomeRepository;
  private final CertificationRepository certificationRepository;
  private final AssessmentResultMapper assessmentResultMapper;

  public AssessmentResultServiceImpl(
      AssessmentResultRepository assessmentResultRepository,
      TraineeAssessmentRepository traineeAssessmentRepository,
      RefAssessmentOutcomeRepository refAssessmentOutcomeRepository,
      CertificationRepository certificationRepository,
      AssessmentResultMapper assessmentResultMapper) {
    this.assessmentResultRepository = assessmentResultRepository;
    this.traineeAssessmentRepository = traineeAssessmentRepository;
    this.refAssessmentOutcomeRepository = refAssessmentOutcomeRepository;
    this.certificationRepository = certificationRepository;
    this.assessmentResultMapper = assessmentResultMapper;
  }

  @Override
  public AssessmentResultResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Assessment result ID is required");
    }
    AssessmentResult result = assessmentResultRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AssessmentResult", "id"));
    return assessmentResultMapper.toResponse(result);
  }

  @Override
  public AssessmentResultResponse getByTraineeAssessmentId(Long traineeAssessmentId) {
    if (traineeAssessmentId == null) {
      throw new BadRequestException("Trainee assessment ID is required");
    }
    AssessmentResult result = assessmentResultRepository.findByTraineeAssessmentId(traineeAssessmentId)
        .orElseThrow(() -> new ResourceNotFoundException("AssessmentResult", "traineeAssessmentId"));
    return assessmentResultMapper.toResponse(result);
  }

  @Override
  public List<AssessmentResultResponse> getAllAssessmentResults() {
    return assessmentResultRepository.findAll().stream()
        .map(assessmentResultMapper::toResponse)
        .toList();
  }

  @Override
  public List<AssessmentResultResponse> getByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return assessmentResultRepository.findByTraineeId(traineeId).stream()
        .map(assessmentResultMapper::toResponse)
        .toList();
  }

  @Override
  public List<AssessmentResultResponse> getByAssessmentOutcomeId(Long outcomeId) {
    if (outcomeId == null) {
      throw new BadRequestException("Assessment outcome ID is required");
    }
    return assessmentResultRepository.findByAssessmentOutcomeId(outcomeId).stream()
        .map(assessmentResultMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public AssessmentResultResponse createAssessmentResult(CreateAssessmentResultRequest request) {
    if (request == null) {
      throw new BadRequestException("Assessment result creation request cannot be null");
    }
    if (request.getTraineeAssessmentId() == null) {
      throw new BadRequestException("Trainee assessment ID is required");
    }
    if (request.getMaximumScore() == null) {
      throw new BadRequestException("Maximum score is required");
    }
    if (request.getMaximumScore().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BadRequestException("Maximum score must be greater than 0", "INVALID_MAXIMUM_SCORE");
    }
    if (request.getAssessmentOutcomeId() == null) {
      throw new BadRequestException("Assessment outcome ID is required");
    }

    if (assessmentResultRepository.existsByTraineeAssessmentId(request.getTraineeAssessmentId())) {
      throw new ConflictException("Assessment result already exists for this trainee assessment", "ASSESSMENT_RESULT_ALREADY_EXISTS");
    }

    TraineeAssessment traineeAssessment = traineeAssessmentRepository.findById(request.getTraineeAssessmentId())
        .orElseThrow(() -> new ResourceNotFoundException("TraineeAssessment", "traineeAssessmentId"));

    Trainee trainee = traineeAssessment.getTrainee();
    if (request.getTraineeId() != null && trainee != null && !request.getTraineeId().equals(trainee.getId())) {
      throw new BadRequestException("Trainee ID does not match trainee assessment trainee", "TRAINEE_ASSESSMENT_MISMATCH");
    }

    BigDecimal obtainedScore = request.getObtainedScore();
    if (obtainedScore != null) {
      if (obtainedScore.compareTo(BigDecimal.ZERO) < 0) {
        throw new BadRequestException("Obtained score cannot be negative", "INVALID_OBTAINED_SCORE");
      }
      if (obtainedScore.compareTo(request.getMaximumScore()) > 0) {
        throw new BadRequestException("Obtained score cannot exceed maximum score", "INVALID_OBTAINED_SCORE");
      }
    }

    BigDecimal scorePercentage = request.getScorePercentage();
    if (scorePercentage != null) {
      if (scorePercentage.compareTo(BigDecimal.ZERO) < 0 || scorePercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
        throw new BadRequestException("Score percentage must be between 0 and 100", "INVALID_SCORE_PERCENTAGE");
      }
    } else if (obtainedScore != null) {
      scorePercentage = obtainedScore.multiply(BigDecimal.valueOf(100))
          .divide(request.getMaximumScore(), 2, RoundingMode.HALF_UP);
    }

    RefAssessmentOutcome outcome = refAssessmentOutcomeRepository.findById(request.getAssessmentOutcomeId())
        .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentOutcome", "assessmentOutcomeId"));

    AssessmentResult result = assessmentResultMapper.toEntity(request, traineeAssessment, trainee, outcome);
    result.setScorePercentage(scorePercentage);
    if (request.getRemarks() != null) {
      result.setRemarks(request.getRemarks().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    result.setCreatedAt(now);
    result.setUpdatedAt(now);

    AssessmentResult saved = assessmentResultRepository.save(result);
    return assessmentResultMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public AssessmentResultResponse updateAssessmentResult(Long id, UpdateAssessmentResultRequest request) {
    if (id == null) {
      throw new BadRequestException("Assessment result ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Assessment result update request cannot be null");
    }

    AssessmentResult result = assessmentResultRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AssessmentResult", "id"));

    BigDecimal effectiveMax = request.getMaximumScore() != null ? request.getMaximumScore() : result.getMaximumScore();
    BigDecimal effectiveObtained = request.getObtainedScore() != null ? request.getObtainedScore() : result.getObtainedScore();

    if (request.getMaximumScore() != null) {
      if (request.getMaximumScore().compareTo(BigDecimal.ZERO) <= 0) {
        throw new BadRequestException("Maximum score must be greater than 0", "INVALID_MAXIMUM_SCORE");
      }
      result.setMaximumScore(request.getMaximumScore());
    }

    if (request.getObtainedScore() != null) {
      if (request.getObtainedScore().compareTo(BigDecimal.ZERO) < 0) {
        throw new BadRequestException("Obtained score cannot be negative", "INVALID_OBTAINED_SCORE");
      }
      result.setObtainedScore(request.getObtainedScore());
    }

    if (effectiveObtained != null && effectiveObtained.compareTo(effectiveMax) > 0) {
      throw new BadRequestException("Obtained score cannot exceed maximum score", "INVALID_OBTAINED_SCORE");
    }

    if (request.getScorePercentage() != null) {
      if (request.getScorePercentage().compareTo(BigDecimal.ZERO) < 0
          || request.getScorePercentage().compareTo(BigDecimal.valueOf(100)) > 0) {
        throw new BadRequestException("Score percentage must be between 0 and 100", "INVALID_SCORE_PERCENTAGE");
      }
      result.setScorePercentage(request.getScorePercentage());
    } else if (request.getObtainedScore() != null || request.getMaximumScore() != null) {
      if (effectiveObtained != null && effectiveMax.compareTo(BigDecimal.ZERO) > 0) {
        result.setScorePercentage(
            effectiveObtained.multiply(BigDecimal.valueOf(100)).divide(effectiveMax, 2, RoundingMode.HALF_UP));
      }
    }

    if (request.getAssessmentOutcomeId() != null) {
      RefAssessmentOutcome outcome = refAssessmentOutcomeRepository.findById(request.getAssessmentOutcomeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefAssessmentOutcome", "assessmentOutcomeId"));
      result.setAssessmentOutcome(outcome);
    }

    if (request.getResultDeclaredAt() != null) {
      result.setResultDeclaredAt(request.getResultDeclaredAt());
    }

    if (request.getRemarks() != null) {
      result.setRemarks(request.getRemarks().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    result.setUpdatedAt(now);

    AssessmentResult saved = assessmentResultRepository.save(result);
    return assessmentResultMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteAssessmentResult(Long id) {
    if (id == null) {
      throw new BadRequestException("Assessment result ID is required");
    }
    AssessmentResult result = assessmentResultRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("AssessmentResult", "id"));

    List<Certification> certifications = certificationRepository.findByAssessmentResultId(id);
    if (!certifications.isEmpty()) {
      throw new ConflictException("Cannot delete assessment result because certifications exist", "ASSESSMENT_RESULT_HAS_CERTIFICATIONS");
    }

    assessmentResultRepository.delete(result);
  }
}
