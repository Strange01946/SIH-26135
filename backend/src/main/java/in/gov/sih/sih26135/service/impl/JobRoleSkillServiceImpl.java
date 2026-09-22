package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.AssignJobRoleSkillRequest;
import in.gov.sih.sih26135.dto.request.UpdateJobRoleSkillRequest;
import in.gov.sih.sih26135.dto.response.JobRoleSkillResponse;
import in.gov.sih.sih26135.entity.JobRole;
import in.gov.sih.sih26135.entity.JobRoleSkill;
import in.gov.sih.sih26135.entity.JobRoleSkillId;
import in.gov.sih.sih26135.entity.RefSkillImportance;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillLevel;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.JobRoleSkillMapper;
import in.gov.sih.sih26135.repository.JobRoleRepository;
import in.gov.sih.sih26135.repository.JobRoleSkillRepository;
import in.gov.sih.sih26135.repository.RefSkillImportanceRepository;
import in.gov.sih.sih26135.repository.SkillLevelRepository;
import in.gov.sih.sih26135.repository.SkillRepository;
import in.gov.sih.sih26135.service.JobRoleSkillService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobRoleSkillServiceImpl implements JobRoleSkillService {

  private final JobRoleSkillRepository jobRoleSkillRepository;
  private final JobRoleRepository jobRoleRepository;
  private final SkillRepository skillRepository;
  private final SkillLevelRepository skillLevelRepository;
  private final RefSkillImportanceRepository refSkillImportanceRepository;
  private final JobRoleSkillMapper jobRoleSkillMapper;

  public JobRoleSkillServiceImpl(
      JobRoleSkillRepository jobRoleSkillRepository,
      JobRoleRepository jobRoleRepository,
      SkillRepository skillRepository,
      SkillLevelRepository skillLevelRepository,
      RefSkillImportanceRepository refSkillImportanceRepository,
      JobRoleSkillMapper jobRoleSkillMapper) {
    this.jobRoleSkillRepository = jobRoleSkillRepository;
    this.jobRoleRepository = jobRoleRepository;
    this.skillRepository = skillRepository;
    this.skillLevelRepository = skillLevelRepository;
    this.refSkillImportanceRepository = refSkillImportanceRepository;
    this.jobRoleSkillMapper = jobRoleSkillMapper;
  }

  @Override
  @Transactional
  public JobRoleSkillResponse assignSkillToJobRole(AssignJobRoleSkillRequest request) {
    if (request == null) {
      throw new BadRequestException("Job role skill assignment request cannot be null");
    }
    if (request.getJobRoleId() == null) {
      throw new BadRequestException("Job role ID is required");
    }
    if (request.getSkillId() == null) {
      throw new BadRequestException("Skill ID is required");
    }
    if (request.getRequiredSkillLevelId() == null) {
      throw new BadRequestException("Required skill level ID is required");
    }
    if (request.getSkillImportanceId() == null) {
      throw new BadRequestException("Skill importance ID is required");
    }

    if (jobRoleSkillRepository.existsByJobRoleIdAndSkillId(request.getJobRoleId(), request.getSkillId())) {
      throw new ConflictException("Job role skill mapping already exists", "JOB_ROLE_SKILL_ALREADY_EXISTS");
    }

    JobRole jobRole = jobRoleRepository.findById(request.getJobRoleId())
        .orElseThrow(() -> new ResourceNotFoundException("JobRole", "jobRoleId"));

    Skill skill = skillRepository.findById(request.getSkillId())
        .orElseThrow(() -> new ResourceNotFoundException("Skill", "skillId"));

    SkillLevel requiredSkillLevel = skillLevelRepository.findById(request.getRequiredSkillLevelId())
        .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "requiredSkillLevelId"));

    RefSkillImportance skillImportance = refSkillImportanceRepository.findById(request.getSkillImportanceId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillImportance", "skillImportanceId"));

    JobRoleSkill entity = jobRoleSkillMapper.toEntity(request, jobRole, skill, requiredSkillLevel, skillImportance);
    LocalDateTime now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    JobRoleSkill saved = jobRoleSkillRepository.save(entity);
    return jobRoleSkillMapper.toResponse(saved, jobRole, skill, requiredSkillLevel, skillImportance);
  }

  @Override
  @Transactional
  public JobRoleSkillResponse updateJobRoleSkill(Long jobRoleId, Long skillId, UpdateJobRoleSkillRequest request) {
    if (jobRoleId == null) {
      throw new BadRequestException("Job role ID is required");
    }
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Job role skill update request cannot be null");
    }

    JobRoleSkill jobRoleSkill = jobRoleSkillRepository.findById(new JobRoleSkillId(jobRoleId, skillId))
        .orElseThrow(() -> new ResourceNotFoundException("JobRoleSkill", "jobRoleId and skillId"));

    if (request.getRequiredSkillLevelId() != null) {
      SkillLevel level = skillLevelRepository.findById(request.getRequiredSkillLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "requiredSkillLevelId"));
      jobRoleSkill.setRequiredSkillLevel(level);
    }

    if (request.getSkillImportanceId() != null) {
      RefSkillImportance importance = refSkillImportanceRepository.findById(request.getSkillImportanceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillImportance", "skillImportanceId"));
      jobRoleSkill.setSkillImportance(importance);
    }

    jobRoleSkill.setUpdatedAt(LocalDateTime.now());
    JobRoleSkill saved = jobRoleSkillRepository.save(jobRoleSkill);
    return jobRoleSkillMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void removeSkillFromJobRole(Long jobRoleId, Long skillId) {
    if (jobRoleId == null) {
      throw new BadRequestException("Job role ID is required");
    }
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }

    JobRoleSkill jobRoleSkill = jobRoleSkillRepository.findById(new JobRoleSkillId(jobRoleId, skillId))
        .orElseThrow(() -> new ResourceNotFoundException("JobRoleSkill", "jobRoleId and skillId"));

    jobRoleSkillRepository.delete(jobRoleSkill);
  }

  @Override
  public List<JobRoleSkillResponse> getSkillsForJobRole(Long jobRoleId) {
    if (jobRoleId == null) {
      throw new BadRequestException("Job role ID is required");
    }
    return jobRoleSkillRepository.findByJobRoleId(jobRoleId).stream()
        .map(jobRoleSkillMapper::toResponse)
        .toList();
  }

  @Override
  public List<JobRoleSkillResponse> getJobRolesForSkill(Long skillId) {
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    return jobRoleSkillRepository.findBySkillId(skillId).stream()
        .map(jobRoleSkillMapper::toResponse)
        .toList();
  }
}
