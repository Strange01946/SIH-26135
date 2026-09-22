package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateJobRoleRequest;
import in.gov.sih.sih26135.dto.response.JobRoleResponse;
import in.gov.sih.sih26135.entity.Industry;
import in.gov.sih.sih26135.entity.JobRole;
import in.gov.sih.sih26135.entity.RefQualificationLevel;
import in.gov.sih.sih26135.entity.Sector;
import org.springframework.stereotype.Component;

@Component
public class JobRoleMapper {

  public JobRoleResponse toResponse(JobRole entity) {
    if (entity == null) {
      return null;
    }

    Long sectorId = null;
    String sectorCode = null;
    String sectorName = null;
    if (entity.getSector() != null) {
      sectorId = entity.getSector().getId();
      sectorCode = entity.getSector().getSectorCode();
      sectorName = entity.getSector().getSectorName();
    }

    Long industryId = null;
    String industryCode = null;
    String industryName = null;
    if (entity.getIndustry() != null) {
      industryId = entity.getIndustry().getId();
      industryCode = entity.getIndustry().getIndustryCode();
      industryName = entity.getIndustry().getIndustryName();
    }

    Long qualificationLevelId = null;
    String qualificationLevelCode = null;
    String qualificationLevelName = null;
    if (entity.getQualificationLevel() != null) {
      qualificationLevelId = entity.getQualificationLevel().getId();
      qualificationLevelCode = entity.getQualificationLevel().getLevelCode();
      qualificationLevelName = entity.getQualificationLevel().getLevelName();
    }

    return new JobRoleResponse(
        entity.getId(),
        entity.getJobRoleCode(),
        entity.getJobRoleName(),
        entity.getDescription(),
        sectorId,
        sectorCode,
        sectorName,
        industryId,
        industryCode,
        industryName,
        qualificationLevelId,
        qualificationLevelCode,
        qualificationLevelName,
        entity.getNcoCode(),
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public JobRole toEntity(
      CreateJobRoleRequest request,
      Sector sector,
      Industry industry,
      RefQualificationLevel qualificationLevel) {
    if (request == null) {
      return null;
    }

    JobRole entity = new JobRole();
    entity.setJobRoleCode(request.getJobRoleCode());
    entity.setJobRoleName(request.getJobRoleName());
    entity.setDescription(request.getDescription());
    entity.setSector(sector);
    entity.setIndustry(industry);
    entity.setQualificationLevel(qualificationLevel);
    entity.setNcoCode(request.getNcoCode());
    entity.setLifecycleStatusId(request.getLifecycleStatusId());
    return entity;
  }
}
