package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSkillGapRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapRequest;
import in.gov.sih.sih26135.dto.response.SkillGapResponse;
import in.gov.sih.sih26135.entity.RefSkillGapSeverity;
import in.gov.sih.sih26135.entity.RefSkillGapSource;
import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import in.gov.sih.sih26135.entity.RefSkillImportance;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillGap;
import in.gov.sih.sih26135.entity.SkillGapAssessment;
import in.gov.sih.sih26135.entity.SkillLevel;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapMapper;
import in.gov.sih.sih26135.repository.RefSkillGapSeverityRepository;
import in.gov.sih.sih26135.repository.RefSkillGapSourceRepository;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.repository.RefSkillImportanceRepository;
import in.gov.sih.sih26135.repository.SkillGapAssessmentRepository;
import in.gov.sih.sih26135.repository.SkillGapObservationRepository;
import in.gov.sih.sih26135.repository.SkillGapRecommendationRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.repository.SkillLevelRepository;
import in.gov.sih.sih26135.repository.SkillRepository;
import in.gov.sih.sih26135.service.SkillGapService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapServiceImpl implements SkillGapService {

  private final SkillGapRepository skillGapRepository;
  private final SkillGapAssessmentRepository assessmentRepository;
  private final SkillRepository skillRepository;
  private final SkillLevelRepository skillLevelRepository;
  private final RefSkillImportanceRepository skillImportanceRepository;
  private final RefSkillGapSeverityRepository skillGapSeverityRepository;
  private final RefSkillGapStatusRepository skillGapStatusRepository;
  private final RefSkillGapSourceRepository skillGapSourceRepository;
  private final SkillGapObservationRepository observationRepository;
  private final SkillGapRecommendationRepository recommendationRepository;
  private final SkillGapMapper mapper;

  public SkillGapServiceImpl(
      SkillGapRepository skillGapRepository,
      SkillGapAssessmentRepository assessmentRepository,
      SkillRepository skillRepository,
      SkillLevelRepository skillLevelRepository,
      RefSkillImportanceRepository skillImportanceRepository,
      RefSkillGapSeverityRepository skillGapSeverityRepository,
      RefSkillGapStatusRepository skillGapStatusRepository,
      RefSkillGapSourceRepository skillGapSourceRepository,
      SkillGapObservationRepository observationRepository,
      SkillGapRecommendationRepository recommendationRepository,
      SkillGapMapper mapper) {
    this.skillGapRepository = skillGapRepository;
    this.assessmentRepository = assessmentRepository;
    this.skillRepository = skillRepository;
    this.skillLevelRepository = skillLevelRepository;
    this.skillImportanceRepository = skillImportanceRepository;
    this.skillGapSeverityRepository = skillGapSeverityRepository;
    this.skillGapStatusRepository = skillGapStatusRepository;
    this.skillGapSourceRepository = skillGapSourceRepository;
    this.observationRepository = observationRepository;
    this.recommendationRepository = recommendationRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public SkillGapResponse createSkillGap(CreateSkillGapRequest request) {
    if (request == null) {
      throw new BadRequestException("Request cannot be null");
    }

    if (request.getSkillGapNumber() == null || request.getSkillGapNumber().isBlank()) {
      throw new BadRequestException("Skill gap number is required");
    }
    String skillGapNumber = request.getSkillGapNumber().trim();
    if (skillGapNumber.length() > 32) {
      throw new BadRequestException("Skill gap number cannot exceed 32 characters");
    }
    if (skillGapRepository.existsBySkillGapNumber(skillGapNumber)) {
      throw new ConflictException("Skill gap number already exists: " + skillGapNumber, "SKILL_GAP_NUMBER_ALREADY_EXISTS");
    }

    if (request.getSkillGapAssessmentId() == null) {
      throw new BadRequestException("Skill gap assessment ID is required");
    }
    SkillGapAssessment assessment = assessmentRepository.findById(request.getSkillGapAssessmentId())
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapAssessment", "id"));

    Trainee trainee = assessment.getTrainee();
    if (request.getTraineeId() != null && !request.getTraineeId().equals(trainee.getId())) {
      throw new BadRequestException("Trainee ID does not match assessment trainee", "SKILL_GAP_ASSESSMENT_TRAINEE_MISMATCH");
    }

    if (request.getSkillId() == null) {
      throw new BadRequestException("Skill ID is required");
    }
    Skill skill = skillRepository.findById(request.getSkillId())
        .orElseThrow(() -> new ResourceNotFoundException("Skill", "id"));

    if (skillGapRepository.existsBySkillGapAssessmentIdAndSkillId(assessment.getId(), skill.getId())) {
      throw new ConflictException("Skill gap already exists for this assessment and skill", "ASSESSMENT_SKILL_ALREADY_EXISTS");
    }

    if (request.getRequiredSkillLevelId() == null) {
      throw new BadRequestException("Required skill level ID is required");
    }
    SkillLevel requiredLevel = skillLevelRepository.findById(request.getRequiredSkillLevelId())
        .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "requiredSkillLevelId"));

    SkillLevel observedLevel = null;
    if (request.getObservedSkillLevelId() != null) {
      observedLevel = skillLevelRepository.findById(request.getObservedSkillLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "observedSkillLevelId"));
    }

    SkillLevel targetLevel = null;
    if (request.getTargetSkillLevelId() != null) {
      targetLevel = skillLevelRepository.findById(request.getTargetSkillLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "targetSkillLevelId"));
    }

    Integer gapLevelDelta;
    if (requiredLevel != null && observedLevel != null) {
      gapLevelDelta = requiredLevel.getLevelRank() - observedLevel.getLevelRank();
    } else {
      gapLevelDelta = request.getGapLevelDelta();
    }
    if (gapLevelDelta != null && (gapLevelDelta < -4 || gapLevelDelta > 4)) {
      throw new BadRequestException("Gap level delta must be between -4 and 4", "INVALID_GAP_LEVEL_DELTA");
    }

    RefSkillImportance skillImportance = null;
    if (request.getSkillImportanceId() != null) {
      skillImportance = skillImportanceRepository.findById(request.getSkillImportanceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillImportance", "id"));
    }

    if (request.getSkillGapSeverityId() == null) {
      throw new BadRequestException("Skill gap severity ID is required");
    }
    RefSkillGapSeverity severity = skillGapSeverityRepository.findById(request.getSkillGapSeverityId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSeverity", "id"));

    if (request.getSkillGapStatusId() == null) {
      throw new BadRequestException("Skill gap status ID is required");
    }
    RefSkillGapStatus status = skillGapStatusRepository.findById(request.getSkillGapStatusId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapStatus", "id"));

    if (request.getSkillGapSourceId() == null) {
      throw new BadRequestException("Skill gap source ID is required");
    }
    RefSkillGapSource source = skillGapSourceRepository.findById(request.getSkillGapSourceId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSource", "id"));

    if (request.getIdentifiedOn() == null) {
      throw new BadRequestException("Identified on date is required");
    }

    LocalDate resolvedOn = request.getResolvedOn();
    if (resolvedOn != null && resolvedOn.isBefore(request.getIdentifiedOn())) {
      throw new BadRequestException("Resolved date cannot be before identified date", "RESOLVED_DATE_BEFORE_IDENTIFIED");
    }
    if (Boolean.TRUE.equals(status.getIsResolvedFlag()) && resolvedOn == null) {
      throw new BadRequestException("Resolved on date is required when status is resolved", "RESOLVED_STATUS_REQUIRES_DATE");
    }

    boolean isCurrent = request.getIsCurrent() == null || request.getIsCurrent();
    if (isCurrent) {
      Optional<SkillGap> existingCurrent = skillGapRepository
          .findByTraineeIdAndSkillIdAndCurrentGapKey(trainee.getId(), skill.getId(), 1);
      if (existingCurrent.isPresent()) {
        SkillGap prev = existingCurrent.get();
        prev.setIsCurrent(false);
        skillGapRepository.save(prev);
      }
    }

    SkillGap gap = new SkillGap(
        skillGapNumber,
        assessment,
        trainee,
        skill,
        requiredLevel,
        severity,
        status,
        source,
        request.getIdentifiedOn()
    );
    gap.setObservedSkillLevel(observedLevel);
    gap.setTargetSkillLevel(targetLevel);
    gap.setGapLevelDelta(gapLevelDelta);
    gap.setSkillImportance(skillImportance);
    gap.setIsCurrent(isCurrent);
    gap.setResolvedOn(resolvedOn);
    gap.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

    SkillGap saved = skillGapRepository.save(gap);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SkillGapResponse updateSkillGap(Long id, UpdateSkillGapRequest request) {
    if (id == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Request cannot be null");
    }

    SkillGap gap = skillGapRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGap", "id"));

    if (request.getRequiredSkillLevelId() != null) {
      SkillLevel requiredLevel = skillLevelRepository.findById(request.getRequiredSkillLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "requiredSkillLevelId"));
      gap.setRequiredSkillLevel(requiredLevel);
    }

    if (request.getObservedSkillLevelId() != null) {
      SkillLevel observedLevel = skillLevelRepository.findById(request.getObservedSkillLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "observedSkillLevelId"));
      gap.setObservedSkillLevel(observedLevel);
    }

    if (gap.getRequiredSkillLevel() != null && gap.getObservedSkillLevel() != null) {
      gap.setGapLevelDelta(gap.getRequiredSkillLevel().getLevelRank() - gap.getObservedSkillLevel().getLevelRank());
    } else if (request.getGapLevelDelta() != null) {
      gap.setGapLevelDelta(request.getGapLevelDelta());
    }

    if (gap.getGapLevelDelta() != null && (gap.getGapLevelDelta() < -4 || gap.getGapLevelDelta() > 4)) {
      throw new BadRequestException("Gap level delta must be between -4 and 4", "INVALID_GAP_LEVEL_DELTA");
    }

    if (request.getTargetSkillLevelId() != null) {
      SkillLevel targetLevel = skillLevelRepository.findById(request.getTargetSkillLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "targetSkillLevelId"));
      gap.setTargetSkillLevel(targetLevel);
    }

    if (request.getSkillImportanceId() != null) {
      RefSkillImportance importance = skillImportanceRepository.findById(request.getSkillImportanceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillImportance", "id"));
      gap.setSkillImportance(importance);
    }

    if (request.getSkillGapSeverityId() != null) {
      RefSkillGapSeverity severity = skillGapSeverityRepository.findById(request.getSkillGapSeverityId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSeverity", "id"));
      gap.setSkillGapSeverity(severity);
    }

    if (request.getSkillGapStatusId() != null) {
      RefSkillGapStatus status = skillGapStatusRepository.findById(request.getSkillGapStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapStatus", "id"));
      gap.setSkillGapStatus(status);
    }

    if (request.getSkillGapSourceId() != null) {
      RefSkillGapSource source = skillGapSourceRepository.findById(request.getSkillGapSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSource", "id"));
      gap.setSkillGapSource(source);
    }

    if (request.getIdentifiedOn() != null) {
      gap.setIdentifiedOn(request.getIdentifiedOn());
    }

    if (request.getResolvedOn() != null) {
      if (request.getResolvedOn().isBefore(gap.getIdentifiedOn())) {
        throw new BadRequestException("Resolved date cannot be before identified date", "RESOLVED_DATE_BEFORE_IDENTIFIED");
      }
      gap.setResolvedOn(request.getResolvedOn());
    }

    if (Boolean.TRUE.equals(gap.getSkillGapStatus().getIsResolvedFlag()) && gap.getResolvedOn() == null) {
      throw new BadRequestException("Resolved on date is required when status is resolved", "RESOLVED_STATUS_REQUIRES_DATE");
    }

    if (request.getIsCurrent() != null && !request.getIsCurrent().equals(gap.getIsCurrent())) {
      if (Boolean.TRUE.equals(request.getIsCurrent())) {
        Optional<SkillGap> existingCurrent = skillGapRepository
            .findByTraineeIdAndSkillIdAndCurrentGapKey(gap.getTrainee().getId(), gap.getSkill().getId(), 1);
        if (existingCurrent.isPresent() && !existingCurrent.get().getId().equals(gap.getId())) {
          SkillGap prev = existingCurrent.get();
          prev.setIsCurrent(false);
          skillGapRepository.save(prev);
        }
      }
      gap.setIsCurrent(request.getIsCurrent());
    }

    if (request.getNotes() != null) {
      gap.setNotes(request.getNotes().trim());
    }

    SkillGap updated = skillGapRepository.save(gap);
    return mapper.toResponse(updated);
  }

  @Override
  public SkillGapResponse getSkillGapById(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    SkillGap entity = skillGapRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGap", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapResponse getSkillGapByNumber(String skillGapNumber) {
    if (skillGapNumber == null || skillGapNumber.isBlank()) {
      throw new BadRequestException("Skill gap number is required");
    }
    SkillGap entity = skillGapRepository.findBySkillGapNumber(skillGapNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("SkillGap", "skillGapNumber"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapResponse getSkillGapByAssessmentAndSkill(Long assessmentId, Long skillId) {
    if (assessmentId == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    SkillGap entity = skillGapRepository.findBySkillGapAssessmentIdAndSkillId(assessmentId, skillId)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGap", "assessmentId and skillId"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<SkillGapResponse> getSkillGapsByAssessment(Long assessmentId) {
    if (assessmentId == null) {
      throw new BadRequestException("Assessment ID is required");
    }
    return skillGapRepository.findBySkillGapAssessmentId(assessmentId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapResponse> getSkillGapsByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return skillGapRepository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapResponse> getSkillGapsBySkill(Long skillId) {
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    return skillGapRepository.findBySkillId(skillId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapResponse> getCurrentSkillGapsByTrainee(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return skillGapRepository.findByTraineeIdAndIsCurrentTrue(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapResponse> getCurrentSkillGapsBySkill(Long skillId) {
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    return skillGapRepository.findBySkillIdAndIsCurrentTrue(skillId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapResponse> getSkillGapsBySeverity(Long severityId) {
    if (severityId == null) {
      throw new BadRequestException("Severity ID is required");
    }
    return skillGapRepository.findBySkillGapSeverityId(severityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapResponse> getSkillGapsByStatus(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return skillGapRepository.findBySkillGapStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapResponse> getSkillGapsBySource(Long sourceId) {
    if (sourceId == null) {
      throw new BadRequestException("Source ID is required");
    }
    return skillGapRepository.findBySkillGapSourceId(sourceId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteSkillGap(Long id) {
    if (id == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    SkillGap gap = skillGapRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGap", "id"));

    if (!observationRepository.findBySkillGapId(id).isEmpty()) {
      throw new ConflictException("Cannot delete skill gap with existing observations", "SKILL_GAP_HAS_OBSERVATIONS");
    }

    if (!recommendationRepository.findBySkillGapId(id).isEmpty()) {
      throw new ConflictException("Cannot delete skill gap with existing recommendations", "SKILL_GAP_HAS_RECOMMENDATIONS");
    }

    skillGapRepository.delete(gap);
  }
}
