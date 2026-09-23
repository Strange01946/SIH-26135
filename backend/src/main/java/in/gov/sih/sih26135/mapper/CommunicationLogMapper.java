package in.gov.sih.sih26135.mapper;

import in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.response.CommunicationLogResponse;
import in.gov.sih.sih26135.entity.CommunicationLog;
import in.gov.sih.sih26135.entity.FollowupTask;
import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import in.gov.sih.sih26135.entity.RefCommunicationDirection;
import in.gov.sih.sih26135.entity.RefCommunicationPurpose;
import in.gov.sih.sih26135.entity.RefCommunicationStatus;
import in.gov.sih.sih26135.entity.RefConsentType;
import in.gov.sih.sih26135.entity.SurveyResponse;
import in.gov.sih.sih26135.entity.Trainee;
import org.springframework.stereotype.Component;

@Component
public class CommunicationLogMapper {

  public CommunicationLogResponse toResponse(CommunicationLog entity) {
    if (entity == null) {
      return null;
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

    Long followupTaskId = null;
    if (entity.getFollowupTask() != null) {
      followupTaskId = entity.getFollowupTask().getId();
    }

    Long surveyResponseId = null;
    if (entity.getSurveyResponse() != null) {
      surveyResponseId = entity.getSurveyResponse().getId();
    }

    Long channelId = null;
    String channelCode = null;
    String channelName = null;
    if (entity.getCommunicationChannel() != null) {
      channelId = entity.getCommunicationChannel().getId();
      channelCode = entity.getCommunicationChannel().getChannelCode();
      channelName = entity.getCommunicationChannel().getChannelName();
    }

    Long directionId = null;
    String directionCode = null;
    String directionName = null;
    if (entity.getCommunicationDirection() != null) {
      directionId = entity.getCommunicationDirection().getId();
      directionCode = entity.getCommunicationDirection().getDirectionCode();
      directionName = entity.getCommunicationDirection().getDirectionName();
    }

    Long purposeId = null;
    String purposeCode = null;
    String purposeName = null;
    if (entity.getCommunicationPurpose() != null) {
      purposeId = entity.getCommunicationPurpose().getId();
      purposeCode = entity.getCommunicationPurpose().getPurposeCode();
      purposeName = entity.getCommunicationPurpose().getPurposeName();
    }

    Long statusId = null;
    String statusCode = null;
    String statusName = null;
    Boolean isSuccess = null;
    Boolean isFailure = null;
    if (entity.getCommunicationStatus() != null) {
      statusId = entity.getCommunicationStatus().getId();
      statusCode = entity.getCommunicationStatus().getStatusCode();
      statusName = entity.getCommunicationStatus().getStatusName();
      isSuccess = entity.getCommunicationStatus().getIsSuccessFlag();
      isFailure = entity.getCommunicationStatus().getIsFailureFlag();
    }

    Long consentTypeId = null;
    String consentTypeCode = null;
    String consentTypeName = null;
    if (entity.getConsentType() != null) {
      consentTypeId = entity.getConsentType().getId();
      consentTypeCode = entity.getConsentType().getConsentCode();
      consentTypeName = entity.getConsentType().getConsentName();
    }

    return new CommunicationLogResponse(
        entity.getId(),
        traineeId,
        traineeReg,
        traineeFirst,
        traineeLast,
        followupTaskId,
        entity.getSurveyId(),
        surveyResponseId,
        channelId,
        channelCode,
        channelName,
        directionId,
        directionCode,
        directionName,
        purposeId,
        purposeCode,
        purposeName,
        statusId,
        statusCode,
        statusName,
        isSuccess,
        isFailure,
        consentTypeId,
        consentTypeCode,
        consentTypeName,
        entity.getConsentCheckedAt(),
        entity.getInitiatedByUserId(),
        entity.getProviderMessageId(),
        entity.getMessageTemplateCode(),
        entity.getFailureReason(),
        entity.getQueuedAt(),
        entity.getSentAt(),
        entity.getDeliveredAt(),
        entity.getReadAt(),
        entity.getFailedAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  public CommunicationLog toEntity(
      CreateCommunicationLogRequest request,
      Trainee trainee,
      FollowupTask task,
      SurveyResponse response,
      RefCommunicationChannel channel,
      RefCommunicationDirection direction,
      RefCommunicationPurpose purpose,
      RefCommunicationStatus status,
      RefConsentType consentType) {
    if (request == null) {
      return null;
    }

    CommunicationLog entity = new CommunicationLog();
    entity.setTrainee(trainee);
    entity.setFollowupTask(task);
    entity.setSurveyId(request.getSurveyId());
    entity.setSurveyResponse(response);
    entity.setCommunicationChannel(channel);
    entity.setCommunicationDirection(direction);
    entity.setCommunicationPurpose(purpose);
    entity.setCommunicationStatus(status);
    entity.setConsentType(consentType);
    entity.setConsentCheckedAt(request.getConsentCheckedAt());
    entity.setInitiatedByUserId(request.getInitiatedByUserId());
    entity.setProviderMessageId(request.getProviderMessageId());
    entity.setMessageTemplateCode(request.getMessageTemplateCode());
    entity.setFailureReason(request.getFailureReason());
    entity.setQueuedAt(request.getQueuedAt());
    entity.setSentAt(request.getSentAt());
    entity.setDeliveredAt(request.getDeliveredAt());
    entity.setReadAt(request.getReadAt());
    entity.setFailedAt(request.getFailedAt());
    return entity;
  }
}
