package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.SkillGapFactResponse;
import in.gov.sih.sih26135.entity.analytics.SkillGapFact;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapFactMapper;
import in.gov.sih.sih26135.repository.analytics.SkillGapFactRepository;
import in.gov.sih.sih26135.service.SkillGapFactService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapFactServiceImpl implements SkillGapFactService {

  private final SkillGapFactRepository repository;
  private final SkillGapFactMapper mapper;

  public SkillGapFactServiceImpl(
      SkillGapFactRepository repository,
      SkillGapFactMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SkillGapFactResponse> getAllSkillGaps() {
    return repository.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public SkillGapFactResponse getSkillGapById(Long skillGapId) {
    if (skillGapId == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    SkillGapFact entity = repository.findById(skillGapId)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapFact", "skillGapId"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapFactResponse getSkillGapByNumber(String skillGapNumber) {
    if (skillGapNumber == null || skillGapNumber.isBlank()) {
      throw new BadRequestException("Skill gap number is required");
    }
    SkillGapFact entity = repository.findBySkillGapNumber(skillGapNumber.trim())
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapFact", "skillGapNumber"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<SkillGapFactResponse> getSkillGapsByTraineeId(Long traineeId) {
    if (traineeId == null) {
      throw new BadRequestException("Trainee ID is required");
    }
    return repository.findByTraineeId(traineeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapFactResponse> getSkillGapsBySkillId(Long skillId) {
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    return repository.findBySkillId(skillId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapFactResponse> getSkillGapsByCourseId(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return repository.findByCourseId(courseId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapFactResponse> getSkillGapsByJobRoleId(Long jobRoleId) {
    if (jobRoleId == null) {
      throw new BadRequestException("Job role ID is required");
    }
    return repository.findByJobRoleId(jobRoleId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapFactResponse> getSkillGapsBySeverityId(Long severityId) {
    if (severityId == null) {
      throw new BadRequestException("Severity ID is required");
    }
    return repository.findBySkillGapSeverityId(severityId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapFactResponse> getSkillGapsByStatusId(Long statusId) {
    if (statusId == null) {
      throw new BadRequestException("Status ID is required");
    }
    return repository.findBySkillGapStatusId(statusId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapFactResponse> getCurrentSkillGaps() {
    return repository.findByIsCurrentTrue().stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapFactResponse> getSkillGapsByTraineeDistrictId(Long traineeDistrictId) {
    if (traineeDistrictId == null) {
      throw new BadRequestException("Trainee district ID is required");
    }
    return repository.findByTraineeDistrictId(traineeDistrictId).stream()
        .map(mapper::toResponse)
        .toList();
  }
}
