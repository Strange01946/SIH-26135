package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateCourseRequest;
import in.gov.sih.sih26135.dto.response.CourseResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Industry;
import in.gov.sih.sih26135.entity.RefDeliveryMode;
import in.gov.sih.sih26135.entity.RefQualificationLevel;
import in.gov.sih.sih26135.entity.Sector;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

  public CourseResponse toResponse(Course entity) {
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
    Integer nsqfLevel = null;
    if (entity.getQualificationLevel() != null) {
      qualificationLevelId = entity.getQualificationLevel().getId();
      qualificationLevelCode = entity.getQualificationLevel().getLevelCode();
      qualificationLevelName = entity.getQualificationLevel().getLevelName();
      nsqfLevel = entity.getQualificationLevel().getNsqfLevel();
    }

    Long deliveryModeId = null;
    String deliveryModeCode = null;
    String deliveryModeName = null;
    if (entity.getDeliveryMode() != null) {
      deliveryModeId = entity.getDeliveryMode().getId();
      deliveryModeCode = entity.getDeliveryMode().getModeCode();
      deliveryModeName = entity.getDeliveryMode().getModeName();
    }

    return new CourseResponse(
        entity.getId(),
        entity.getCourseCode(),
        entity.getCourseName(),
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
        nsqfLevel,
        entity.getDurationHours(),
        entity.getDurationDays(),
        deliveryModeId,
        deliveryModeCode,
        deliveryModeName,
        entity.getLifecycleStatusId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public Course toEntity(
      CreateCourseRequest request,
      Sector sector,
      Industry industry,
      RefQualificationLevel qualificationLevel,
      RefDeliveryMode deliveryMode) {
    if (request == null) {
      return null;
    }

    Course course = new Course();
    course.setCourseCode(request.getCourseCode());
    course.setCourseName(request.getCourseName());
    course.setDescription(request.getDescription());
    course.setSector(sector);
    course.setIndustry(industry);
    course.setQualificationLevel(qualificationLevel);
    course.setDurationHours(request.getDurationHours());
    course.setDurationDays(request.getDurationDays());
    course.setDeliveryMode(deliveryMode);
    course.setLifecycleStatusId(request.getLifecycleStatusId());
    return course;
  }
}
