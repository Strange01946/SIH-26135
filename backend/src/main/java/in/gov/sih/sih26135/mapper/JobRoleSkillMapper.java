package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.AssignJobRoleSkillRequest;
import in.gov.sih.sih26135.dto.response.JobRoleSkillResponse;
import in.gov.sih.sih26135.entity.JobRole;
import in.gov.sih.sih26135.entity.JobRoleSkill;
import in.gov.sih.sih26135.entity.RefSkillImportance;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillLevel;
import org.springframework.stereotype.Component;

@Component
public class JobRoleSkillMapper {

  public JobRoleSkillResponse toResponse(JobRoleSkill entity) {
    if (entity == null) {
      return null;
    }

    String jobRoleCode = null;
    String jobRoleName = null;
    if (entity.getJobRole() != null) {
      jobRoleCode = entity.getJobRole().getJobRoleCode();
      jobRoleName = entity.getJobRole().getJobRoleName();
    }

    String skillCode = null;
    String skillName = null;
    if (entity.getSkill() != null) {
      skillCode = entity.getSkill().getSkillCode();
      skillName = entity.getSkill().getSkillName();
    }

    Long requiredSkillLevelId = null;
    String requiredSkillLevelCode = null;
    String requiredSkillLevelName = null;
    Integer requiredSkillLevelRank = null;
    if (entity.getRequiredSkillLevel() != null) {
      requiredSkillLevelId = entity.getRequiredSkillLevel().getId();
      requiredSkillLevelCode = entity.getRequiredSkillLevel().getLevelCode();
      requiredSkillLevelName = entity.getRequiredSkillLevel().getLevelName();
      requiredSkillLevelRank = entity.getRequiredSkillLevel().getLevelRank();
    }

    Long skillImportanceId = null;
    String skillImportanceCode = null;
    String skillImportanceName = null;
    Integer skillImportanceWeight = null;
    if (entity.getSkillImportance() != null) {
      skillImportanceId = entity.getSkillImportance().getId();
      skillImportanceCode = entity.getSkillImportance().getImportanceCode();
      skillImportanceName = entity.getSkillImportance().getImportanceName();
      skillImportanceWeight = entity.getSkillImportance().getImportanceWeight();
    }

    return new JobRoleSkillResponse(
        entity.getJobRoleId(),
        jobRoleCode,
        jobRoleName,
        entity.getSkillId(),
        skillCode,
        skillName,
        requiredSkillLevelId,
        requiredSkillLevelCode,
        requiredSkillLevelName,
        requiredSkillLevelRank,
        skillImportanceId,
        skillImportanceCode,
        skillImportanceName,
        skillImportanceWeight,
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public JobRoleSkillResponse toResponse(
      JobRoleSkill entity,
      JobRole jobRole,
      Skill skill,
      SkillLevel level,
      RefSkillImportance importance) {
    if (entity == null) {
      return null;
    }

    String jobRoleCode = jobRole != null ? jobRole.getJobRoleCode() : null;
    String jobRoleName = jobRole != null ? jobRole.getJobRoleName() : null;
    String skillCode = skill != null ? skill.getSkillCode() : null;
    String skillName = skill != null ? skill.getSkillName() : null;

    Long requiredSkillLevelId = level != null ? level.getId() : null;
    String requiredSkillLevelCode = level != null ? level.getLevelCode() : null;
    String requiredSkillLevelName = level != null ? level.getLevelName() : null;
    Integer requiredSkillLevelRank = level != null ? level.getLevelRank() : null;

    Long skillImportanceId = importance != null ? importance.getId() : null;
    String skillImportanceCode = importance != null ? importance.getImportanceCode() : null;
    String skillImportanceName = importance != null ? importance.getImportanceName() : null;
    Integer skillImportanceWeight = importance != null ? importance.getImportanceWeight() : null;

    return new JobRoleSkillResponse(
        entity.getJobRoleId(),
        jobRoleCode,
        jobRoleName,
        entity.getSkillId(),
        skillCode,
        skillName,
        requiredSkillLevelId,
        requiredSkillLevelCode,
        requiredSkillLevelName,
        requiredSkillLevelRank,
        skillImportanceId,
        skillImportanceCode,
        skillImportanceName,
        skillImportanceWeight,
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public JobRoleSkill toEntity(
      AssignJobRoleSkillRequest request,
      JobRole jobRole,
      Skill skill,
      SkillLevel level,
      RefSkillImportance importance) {
    if (request == null) {
      return null;
    }

    JobRoleSkill entity = new JobRoleSkill();
    entity.setJobRole(jobRole);
    entity.setJobRoleId(request.getJobRoleId());
    entity.setSkill(skill);
    entity.setSkillId(request.getSkillId());
    entity.setRequiredSkillLevel(level);
    entity.setSkillImportance(importance);
    return entity;
  }
}
