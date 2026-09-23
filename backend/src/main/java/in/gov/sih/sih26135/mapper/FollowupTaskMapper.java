package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateFollowupTaskRequest;
import in.gov.sih.sih26135.dto.response.FollowupTaskResponse;
import in.gov.sih.sih26135.entity.EmploymentRecord;
import in.gov.sih.sih26135.entity.FollowupCampaign;
import in.gov.sih.sih26135.entity.FollowupTask;
import in.gov.sih.sih26135.entity.PlacementRecord;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.entity.TrainingEnrollment;
import org.springframework.stereotype.Component;

@Component
public class FollowupTaskMapper {

  public FollowupTaskResponse toResponse(FollowupTask entity) {
    if (entity == null) {
      return null;
    }

    Long campaignId = null;
    String campaignCode = null;
    String campaignName = null;
    if (entity.getFollowupCampaign() != null) {
      campaignId = entity.getFollowupCampaign().getId();
      campaignCode = entity.getFollowupCampaign().getCampaignCode();
      campaignName = entity.getFollowupCampaign().getCampaignName();
    }

    Long traineeId = null;
    String traineeReg = null;
    String traineeFirst = null;
    String traineeLast = null;
    if (entity.getTrainee() != null) {
      traineeId = entity.getTrainee().getId();
      traineeReg = entity.getTrainee().getRegistrationNumber();
      traineeFirst = entity.getTrainee().getFirstName();
      traineeLast = entity.getTrainee().getLastName();
    }

    Long enrollmentId = null;
    String enrollmentNumber = null;
    if (entity.getEnrollment() != null) {
      enrollmentId = entity.getEnrollment().getId();
      enrollmentNumber = entity.getEnrollment().getEnrollmentNumber();
    }

    Long placementId = null;
    String placementNumber = null;
    if (entity.getPlacementRecord() != null) {
      placementId = entity.getPlacementRecord().getId();
      placementNumber = entity.getPlacementRecord().getPlacementNumber();
    }

    Long employmentId = null;
    if (entity.getEmploymentRecord() != null) {
      employmentId = entity.getEmploymentRecord().getId();
    }

    Long lastChannelId = null;
    String lastChannelCode = null;
    String lastChannelName = null;
    if (entity.getLastChannel() != null) {
      lastChannelId = entity.getLastChannel().getId();
      lastChannelCode = entity.getLastChannel().getChannelCode();
      lastChannelName = entity.getLastChannel().getChannelName();
    }

    return new FollowupTaskResponse(
        entity.getId(),
        campaignId,
        campaignCode,
        campaignName,
        traineeId,
        traineeReg,
        traineeFirst,
        traineeLast,
        enrollmentId,
        enrollmentNumber,
        placementId,
        placementNumber,
        employmentId,
        entity.getSurveyId(),
        entity.getScheduledDate(),
        entity.getNextFollowupDate(),
        entity.getFollowupStatusId(),
        entity.getFollowupOutcomeId(),
        entity.getNonResponseReasonId(),
        entity.getAssignedUserId(),
        lastChannelId,
        lastChannelCode,
        lastChannelName,
        entity.getRemarks(),
        entity.getCompletedAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public FollowupTask toEntity(
      CreateFollowupTaskRequest request,
      FollowupCampaign campaign,
      Trainee trainee,
      TrainingEnrollment enrollment,
      PlacementRecord placementRecord,
      EmploymentRecord employmentRecord,
      RefCommunicationChannel lastChannel) {
    if (request == null) {
      return null;
    }

    FollowupTask entity = new FollowupTask();
    entity.setFollowupCampaign(campaign);
    entity.setTrainee(trainee);
    entity.setEnrollment(enrollment);
    entity.setPlacementRecord(placementRecord);
    entity.setEmploymentRecord(employmentRecord);
    entity.setSurveyId(request.getSurveyId());
    entity.setScheduledDate(request.getScheduledDate());
    entity.setNextFollowupDate(request.getNextFollowupDate());
    entity.setFollowupStatusId(request.getFollowupStatusId());
    entity.setFollowupOutcomeId(request.getFollowupOutcomeId());
    entity.setNonResponseReasonId(request.getNonResponseReasonId());
    entity.setAssignedUserId(request.getAssignedUserId());
    entity.setLastChannel(lastChannel);
    entity.setRemarks(request.getRemarks());
    entity.setCompletedAt(request.getCompletedAt());
    return entity;
  }
}
