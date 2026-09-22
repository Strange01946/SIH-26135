package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateJobRoleRequest;
import in.gov.sih.sih26135.dto.request.UpdateJobRoleRequest;
import in.gov.sih.sih26135.dto.response.JobRoleResponse;
import in.gov.sih.sih26135.entity.Industry;
import in.gov.sih.sih26135.entity.JobRole;
import in.gov.sih.sih26135.entity.RefQualificationLevel;
import in.gov.sih.sih26135.entity.Sector;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.JobRoleMapper;
import in.gov.sih.sih26135.repository.IndustryRepository;
import in.gov.sih.sih26135.repository.JobRoleRepository;
import in.gov.sih.sih26135.repository.RefQualificationLevelRepository;
import in.gov.sih.sih26135.repository.SectorRepository;
import in.gov.sih.sih26135.service.JobRoleService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JobRoleServiceImpl implements JobRoleService {

  private final JobRoleRepository jobRoleRepository;
  private final SectorRepository sectorRepository;
  private final IndustryRepository industryRepository;
  private final RefQualificationLevelRepository refQualificationLevelRepository;
  private final JobRoleMapper jobRoleMapper;

  public JobRoleServiceImpl(
      JobRoleRepository jobRoleRepository,
      SectorRepository sectorRepository,
      IndustryRepository industryRepository,
      RefQualificationLevelRepository refQualificationLevelRepository,
      JobRoleMapper jobRoleMapper) {
    this.jobRoleRepository = jobRoleRepository;
    this.sectorRepository = sectorRepository;
    this.industryRepository = industryRepository;
    this.refQualificationLevelRepository = refQualificationLevelRepository;
    this.jobRoleMapper = jobRoleMapper;
  }

  @Override
  public JobRoleResponse getById(Long id) {
    if (id == null) {
      throw new BadRequestException("Job role ID is required");
    }
    JobRole jobRole = jobRoleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("JobRole", "id"));
    return jobRoleMapper.toResponse(jobRole);
  }

  @Override
  public JobRoleResponse getByCode(String jobRoleCode) {
    if (jobRoleCode == null || jobRoleCode.isBlank()) {
      throw new BadRequestException("Job role code is required");
    }
    JobRole jobRole = jobRoleRepository.findByJobRoleCode(jobRoleCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("JobRole", "jobRoleCode"));
    return jobRoleMapper.toResponse(jobRole);
  }

  @Override
  public List<JobRoleResponse> getAllJobRoles() {
    return jobRoleRepository.findAll().stream()
        .map(jobRoleMapper::toResponse)
        .toList();
  }

  @Override
  public List<JobRoleResponse> getJobRolesBySectorId(Long sectorId) {
    if (sectorId == null) {
      throw new BadRequestException("Sector ID is required");
    }
    return jobRoleRepository.findBySectorId(sectorId).stream()
        .map(jobRoleMapper::toResponse)
        .toList();
  }

  @Override
  public List<JobRoleResponse> getJobRolesByIndustryId(Long industryId) {
    if (industryId == null) {
      throw new BadRequestException("Industry ID is required");
    }
    return jobRoleRepository.findByIndustryId(industryId).stream()
        .map(jobRoleMapper::toResponse)
        .toList();
  }

  @Override
  public List<JobRoleResponse> getJobRolesByQualificationLevelId(Long qualificationLevelId) {
    if (qualificationLevelId == null) {
      throw new BadRequestException("Qualification level ID is required");
    }
    return jobRoleRepository.findByQualificationLevelId(qualificationLevelId).stream()
        .map(jobRoleMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public JobRoleResponse createJobRole(CreateJobRoleRequest request) {
    if (request == null) {
      throw new BadRequestException("Job role creation request cannot be null");
    }
    if (request.getJobRoleCode() == null || request.getJobRoleCode().isBlank()) {
      throw new BadRequestException("Job role code is required");
    }
    if (request.getJobRoleName() == null || request.getJobRoleName().isBlank()) {
      throw new BadRequestException("Job role name is required");
    }
    if (request.getSectorId() == null) {
      throw new BadRequestException("Sector ID is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    String jobRoleCode = request.getJobRoleCode().trim();
    if (jobRoleRepository.existsByJobRoleCode(jobRoleCode)) {
      throw new ConflictException("Job role code already exists", "JOB_ROLE_CODE_ALREADY_EXISTS");
    }

    Sector sector = sectorRepository.findById(request.getSectorId())
        .orElseThrow(() -> new ResourceNotFoundException("Sector", "sectorId"));

    Industry industry = null;
    if (request.getIndustryId() != null) {
      industry = industryRepository.findById(request.getIndustryId())
          .orElseThrow(() -> new ResourceNotFoundException("Industry", "industryId"));
    }

    RefQualificationLevel qualificationLevel = null;
    if (request.getQualificationLevelId() != null) {
      qualificationLevel = refQualificationLevelRepository.findById(request.getQualificationLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("RefQualificationLevel", "qualificationLevelId"));
    }

    JobRole entity = jobRoleMapper.toEntity(request, sector, industry, qualificationLevel);
    entity.setJobRoleCode(jobRoleCode);
    entity.setJobRoleName(request.getJobRoleName().trim());
    if (request.getDescription() != null) {
      entity.setDescription(request.getDescription().trim());
    }
    if (request.getNcoCode() != null) {
      entity.setNcoCode(request.getNcoCode().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    JobRole saved = jobRoleRepository.save(entity);
    return jobRoleMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public JobRoleResponse updateJobRole(Long id, UpdateJobRoleRequest request) {
    if (id == null) {
      throw new BadRequestException("Job role ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Job role update request cannot be null");
    }

    JobRole jobRole = jobRoleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("JobRole", "id"));

    if (request.getJobRoleName() != null && !request.getJobRoleName().isBlank()) {
      jobRole.setJobRoleName(request.getJobRoleName().trim());
    }

    if (request.getDescription() != null) {
      jobRole.setDescription(request.getDescription().trim());
    }

    if (request.getSectorId() != null) {
      Sector sector = sectorRepository.findById(request.getSectorId())
          .orElseThrow(() -> new ResourceNotFoundException("Sector", "sectorId"));
      jobRole.setSector(sector);
    }

    if (request.getIndustryId() != null) {
      Industry industry = industryRepository.findById(request.getIndustryId())
          .orElseThrow(() -> new ResourceNotFoundException("Industry", "industryId"));
      jobRole.setIndustry(industry);
    }

    if (request.getQualificationLevelId() != null) {
      RefQualificationLevel qualificationLevel = refQualificationLevelRepository.findById(request.getQualificationLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("RefQualificationLevel", "qualificationLevelId"));
      jobRole.setQualificationLevel(qualificationLevel);
    }

    if (request.getNcoCode() != null) {
      jobRole.setNcoCode(request.getNcoCode().trim());
    }

    if (request.getLifecycleStatusId() != null) {
      jobRole.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    LocalDateTime now = LocalDateTime.now();
    jobRole.setUpdatedAt(now);

    JobRole saved = jobRoleRepository.save(jobRole);
    return jobRoleMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteJobRole(Long id) {
    if (id == null) {
      throw new BadRequestException("Job role ID is required");
    }
    JobRole jobRole = jobRoleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("JobRole", "id"));

    LocalDateTime now = LocalDateTime.now();
    jobRole.setDeletedAt(now);
    jobRole.setUpdatedAt(now);
    jobRoleRepository.save(jobRole);
  }
}
