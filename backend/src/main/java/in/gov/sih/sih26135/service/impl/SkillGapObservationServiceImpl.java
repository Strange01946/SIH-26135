package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSkillGapObservationRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapObservationRequest;
import in.gov.sih.sih26135.dto.response.SkillGapObservationResponse;
import in.gov.sih.sih26135.entity.RefSkillGapSeverity;
import in.gov.sih.sih26135.entity.RefSkillGapSource;
import in.gov.sih.sih26135.entity.RefSkillGapStatus;
import in.gov.sih.sih26135.entity.SkillGap;
import in.gov.sih.sih26135.entity.SkillGapObservation;
import in.gov.sih.sih26135.entity.SkillLevel;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapObservationMapper;
import in.gov.sih.sih26135.repository.RefSkillGapSeverityRepository;
import in.gov.sih.sih26135.repository.RefSkillGapSourceRepository;
import in.gov.sih.sih26135.repository.RefSkillGapStatusRepository;
import in.gov.sih.sih26135.repository.SkillGapObservationRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.repository.SkillLevelRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.SkillGapObservationService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapObservationServiceImpl implements SkillGapObservationService {

  private final SkillGapObservationRepository observationRepository;
  private final SkillGapRepository skillGapRepository;
  private final SkillLevelRepository skillLevelRepository;
  private final RefSkillGapSeverityRepository skillGapSeverityRepository;
  private final RefSkillGapStatusRepository skillGapStatusRepository;
  private final RefSkillGapSourceRepository skillGapSourceRepository;
  private final UserRepository userRepository;
  private final SkillGapObservationMapper mapper;

  public SkillGapObservationServiceImpl(
      SkillGapObservationRepository observationRepository,
      SkillGapRepository skillGapRepository,
      SkillLevelRepository skillLevelRepository,
      RefSkillGapSeverityRepository skillGapSeverityRepository,
      RefSkillGapStatusRepository skillGapStatusRepository,
      RefSkillGapSourceRepository skillGapSourceRepository,
      UserRepository userRepository,
      SkillGapObservationMapper mapper) {
    this.observationRepository = observationRepository;
    this.skillGapRepository = skillGapRepository;
    this.skillLevelRepository = skillLevelRepository;
    this.skillGapSeverityRepository = skillGapSeverityRepository;
    this.skillGapStatusRepository = skillGapStatusRepository;
    this.skillGapSourceRepository = skillGapSourceRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public SkillGapObservationResponse createObservation(CreateSkillGapObservationRequest request) {
    if (request == null) {
      throw new BadRequestException("Request cannot be null");
    }

    if (request.getSkillGapId() == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    SkillGap skillGap = skillGapRepository.findById(request.getSkillGapId())
        .orElseThrow(() -> new ResourceNotFoundException("SkillGap", "id"));

    Integer observationNumber = request.getObservationNumber();
    if (observationNumber == null) {
      List<SkillGapObservation> existing = observationRepository.findBySkillGapId(skillGap.getId());
      observationNumber = existing.stream()
          .mapToInt(SkillGapObservation::getObservationNumber)
          .max()
          .orElse(0) + 1;
    } else {
      if (observationNumber < 1) {
        throw new BadRequestException("Observation number must be greater than or equal to 1", "INVALID_OBSERVATION_NUMBER");
      }
      if (observationRepository.existsBySkillGapIdAndObservationNumber(skillGap.getId(), observationNumber)) {
        throw new ConflictException("Observation number already exists for this skill gap", "OBSERVATION_NUMBER_ALREADY_EXISTS");
      }
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

    if (request.getObservedOn() == null) {
      throw new BadRequestException("Observed on date is required");
    }

    if (request.getObservedByUserId() != null && !userRepository.existsById(request.getObservedByUserId())) {
      throw new ResourceNotFoundException("User", "observedByUserId");
    }

    SkillGapObservation observation = new SkillGapObservation(
        skillGap,
        observationNumber,
        requiredLevel,
        severity,
        status,
        source,
        request.getObservedOn()
    );
    observation.setObservedSkillLevel(observedLevel);
    observation.setTargetSkillLevel(targetLevel);
    observation.setGapLevelDelta(gapLevelDelta);
    observation.setObservedByUserId(request.getObservedByUserId());
    observation.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

    SkillGapObservation saved = observationRepository.save(observation);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SkillGapObservationResponse updateObservation(Long id, UpdateSkillGapObservationRequest request) {
    if (id == null) {
      throw new BadRequestException("Observation ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Request cannot be null");
    }

    SkillGapObservation observation = observationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapObservation", "id"));

    if (request.getRequiredSkillLevelId() != null) {
      SkillLevel requiredLevel = skillLevelRepository.findById(request.getRequiredSkillLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "requiredSkillLevelId"));
      observation.setRequiredSkillLevel(requiredLevel);
    }

    if (request.getObservedSkillLevelId() != null) {
      SkillLevel observedLevel = skillLevelRepository.findById(request.getObservedSkillLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "observedSkillLevelId"));
      observation.setObservedSkillLevel(observedLevel);
    }

    if (observation.getRequiredSkillLevel() != null && observation.getObservedSkillLevel() != null) {
      observation.setGapLevelDelta(observation.getRequiredSkillLevel().getLevelRank() - observation.getObservedSkillLevel().getLevelRank());
    } else if (request.getGapLevelDelta() != null) {
      observation.setGapLevelDelta(request.getGapLevelDelta());
    }

    if (observation.getGapLevelDelta() != null && (observation.getGapLevelDelta() < -4 || observation.getGapLevelDelta() > 4)) {
      throw new BadRequestException("Gap level delta must be between -4 and 4", "INVALID_GAP_LEVEL_DELTA");
    }

    if (request.getTargetSkillLevelId() != null) {
      SkillLevel targetLevel = skillLevelRepository.findById(request.getTargetSkillLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "targetSkillLevelId"));
      observation.setTargetSkillLevel(targetLevel);
    }

    if (request.getSkillGapSeverityId() != null) {
      RefSkillGapSeverity severity = skillGapSeverityRepository.findById(request.getSkillGapSeverityId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSeverity", "id"));
      observation.setSkillGapSeverity(severity);
    }

    if (request.getSkillGapStatusId() != null) {
      RefSkillGapStatus status = skillGapStatusRepository.findById(request.getSkillGapStatusId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapStatus", "id"));
      observation.setSkillGapStatus(status);
    }

    if (request.getSkillGapSourceId() != null) {
      RefSkillGapSource source = skillGapSourceRepository.findById(request.getSkillGapSourceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapSource", "id"));
      observation.setSkillGapSource(source);
    }

    if (request.getObservedOn() != null) {
      observation.setObservedOn(request.getObservedOn());
    }

    if (request.getObservedByUserId() != null) {
      if (!userRepository.existsById(request.getObservedByUserId())) {
        throw new ResourceNotFoundException("User", "observedByUserId");
      }
      observation.setObservedByUserId(request.getObservedByUserId());
    }

    if (request.getNotes() != null) {
      observation.setNotes(request.getNotes().trim());
    }

    SkillGapObservation updated = observationRepository.save(observation);
    return mapper.toResponse(updated);
  }

  @Override
  public SkillGapObservationResponse getObservationById(Long id) {
    if (id == null) {
      throw new BadRequestException("Observation ID is required");
    }
    SkillGapObservation entity = observationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapObservation", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapObservationResponse getObservationBySkillGapAndNumber(Long skillGapId, Integer observationNumber) {
    if (skillGapId == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    if (observationNumber == null) {
      throw new BadRequestException("Observation number is required");
    }
    SkillGapObservation entity = observationRepository.findBySkillGapIdAndObservationNumber(skillGapId, observationNumber)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapObservation", "skillGapId and observationNumber"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<SkillGapObservationResponse> getObservationsBySkillGap(Long skillGapId) {
    if (skillGapId == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    return observationRepository.findBySkillGapId(skillGapId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapObservationResponse> getObservationsBySeverity(Long severityId) {
    if (severityId == null) {
      throw new BadRequestException("Severity ID is required");
    }
    return observationRepository.findBySkillGapSeverityId(severityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapObservationResponse> getObservationsByStatus(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return observationRepository.findBySkillGapStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapObservationResponse> getObservationsBySource(Long sourceId) {
    if (sourceId == null) {
      throw new BadRequestException("Source ID is required");
    }
    return observationRepository.findBySkillGapSourceId(sourceId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapObservationResponse> getObservationsByDate(LocalDate observedOn) {
    if (observedOn == null) {
      throw new BadRequestException("Observed on date is required");
    }
    return observationRepository.findByObservedOn(observedOn).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteObservation(Long id) {
    if (id == null) {
      throw new BadRequestException("Observation ID is required");
    }
    SkillGapObservation entity = observationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapObservation", "id"));
    observationRepository.delete(entity);
  }
}
