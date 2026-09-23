package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateFollowupCampaignRequest;
import in.gov.sih.sih26135.dto.response.FollowupCampaignResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.FollowupCampaign;
import in.gov.sih.sih26135.entity.Program;
import org.springframework.stereotype.Component;

@Component
public class FollowupCampaignMapper {

  public FollowupCampaignResponse toResponse(FollowupCampaign entity) {
    if (entity == null) {
      return null;
    }

    Long programId = null;
    String programCode = null;
    String programName = null;
    if (entity.getProgram() != null) {
      programId = entity.getProgram().getId();
      programCode = entity.getProgram().getProgramCode();
      programName = entity.getProgram().getProgramName();
    }

    Long courseId = null;
    String courseCode = null;
    String courseName = null;
    if (entity.getCourse() != null) {
      courseId = entity.getCourse().getId();
      courseCode = entity.getCourse().getCourseCode();
      courseName = entity.getCourse().getCourseName();
    }

    return new FollowupCampaignResponse(
        entity.getId(),
        entity.getCampaignCode(),
        entity.getCampaignName(),
        entity.getFollowupTypeId(),
        entity.getSurveyId(),
        programId,
        programCode,
        programName,
        courseId,
        courseCode,
        courseName,
        entity.getScheduledStartDate(),
        entity.getScheduledEndDate(),
        entity.getLifecycleStatusId(),
        entity.getCreatedByUserId(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt()
    );
  }

  public FollowupCampaign toEntity(
      CreateFollowupCampaignRequest request,
      Program program,
      Course course) {
    if (request == null) {
      return null;
    }

    FollowupCampaign entity = new FollowupCampaign();
    entity.setCampaignCode(request.getCampaignCode());
    entity.setCampaignName(request.getCampaignName());
    entity.setFollowupTypeId(request.getFollowupTypeId());
    entity.setSurveyId(request.getSurveyId());
    entity.setProgram(program);
    entity.setCourse(course);
    entity.setScheduledStartDate(request.getScheduledStartDate());
    entity.setScheduledEndDate(request.getScheduledEndDate());
    entity.setLifecycleStatusId(request.getLifecycleStatusId());
    entity.setCreatedByUserId(request.getCreatedByUserId());
    return entity;
  }
}
