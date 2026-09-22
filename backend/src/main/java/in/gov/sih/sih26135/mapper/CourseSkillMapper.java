package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.AssignCourseSkillRequest;
import in.gov.sih.sih26135.dto.response.CourseSkillResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.CourseSkill;
import in.gov.sih.sih26135.entity.RefSkillImportance;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillLevel;
import org.springframework.stereotype.Component;

@Component
public class CourseSkillMapper {

  public CourseSkillResponse toResponse(CourseSkill entity) {
    if (entity == null) {
      return null;
    }

    String courseCode = null;
    String courseName = null;
    if (entity.getCourse() != null) {
      courseCode = entity.getCourse().getCourseCode();
      courseName = entity.getCourse().getCourseName();
    }

    String skillCode = null;
    String skillName = null;
    if (entity.getSkill() != null) {
      skillCode = entity.getSkill().getSkillCode();
      skillName = entity.getSkill().getSkillName();
    }

    Long taughtSkillLevelId = null;
    String taughtSkillLevelCode = null;
    String taughtSkillLevelName = null;
    Integer taughtSkillLevelRank = null;
    if (entity.getTaughtSkillLevel() != null) {
      taughtSkillLevelId = entity.getTaughtSkillLevel().getId();
      taughtSkillLevelCode = entity.getTaughtSkillLevel().getLevelCode();
      taughtSkillLevelName = entity.getTaughtSkillLevel().getLevelName();
      taughtSkillLevelRank = entity.getTaughtSkillLevel().getLevelRank();
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

    return new CourseSkillResponse(
        entity.getCourseId(),
        courseCode,
        courseName,
        entity.getSkillId(),
        skillCode,
        skillName,
        taughtSkillLevelId,
        taughtSkillLevelCode,
        taughtSkillLevelName,
        taughtSkillLevelRank,
        skillImportanceId,
        skillImportanceCode,
        skillImportanceName,
        skillImportanceWeight,
        entity.getIsCoreSkill(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public CourseSkillResponse toResponse(
      CourseSkill entity,
      Course course,
      Skill skill,
      SkillLevel level,
      RefSkillImportance importance) {
    if (entity == null) {
      return null;
    }

    String courseCode = course != null ? course.getCourseCode() : null;
    String courseName = course != null ? course.getCourseName() : null;
    String skillCode = skill != null ? skill.getSkillCode() : null;
    String skillName = skill != null ? skill.getSkillName() : null;

    Long taughtSkillLevelId = level != null ? level.getId() : null;
    String taughtSkillLevelCode = level != null ? level.getLevelCode() : null;
    String taughtSkillLevelName = level != null ? level.getLevelName() : null;
    Integer taughtSkillLevelRank = level != null ? level.getLevelRank() : null;

    Long skillImportanceId = importance != null ? importance.getId() : null;
    String skillImportanceCode = importance != null ? importance.getImportanceCode() : null;
    String skillImportanceName = importance != null ? importance.getImportanceName() : null;
    Integer skillImportanceWeight = importance != null ? importance.getImportanceWeight() : null;

    return new CourseSkillResponse(
        entity.getCourseId(),
        courseCode,
        courseName,
        entity.getSkillId(),
        skillCode,
        skillName,
        taughtSkillLevelId,
        taughtSkillLevelCode,
        taughtSkillLevelName,
        taughtSkillLevelRank,
        skillImportanceId,
        skillImportanceCode,
        skillImportanceName,
        skillImportanceWeight,
        entity.getIsCoreSkill(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public CourseSkill toEntity(
      AssignCourseSkillRequest request,
      Course course,
      Skill skill,
      SkillLevel level,
      RefSkillImportance importance) {
    if (request == null) {
      return null;
    }

    CourseSkill entity = new CourseSkill();
    entity.setCourse(course);
    entity.setCourseId(request.getCourseId());
    entity.setSkill(skill);
    entity.setSkillId(request.getSkillId());
    entity.setTaughtSkillLevel(level);
    entity.setSkillImportance(importance);
    entity.setIsCoreSkill(request.getIsCoreSkill() != null ? request.getIsCoreSkill() : false);
    return entity;
  }
}
