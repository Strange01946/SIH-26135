package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.request.UpdateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.response.CommunicationLogResponse;
import java.util.List;

public interface CommunicationLogService {

  CommunicationLogResponse logCommunication(CreateCommunicationLogRequest request);

  CommunicationLogResponse updateCommunicationLog(Long id, UpdateCommunicationLogRequest request);

  CommunicationLogResponse getCommunicationLogById(Long id);

  CommunicationLogResponse getCommunicationLogByProviderMessageId(String providerMessageId);

  List<CommunicationLogResponse> getCommunicationLogsByTrainee(Long traineeId);

  List<CommunicationLogResponse> getCommunicationLogsByFollowupTask(Long followupTaskId);

  List<CommunicationLogResponse> getCommunicationLogsBySurvey(Long surveyId);

  List<CommunicationLogResponse> getCommunicationLogsBySurveyResponse(Long surveyResponseId);

  List<CommunicationLogResponse> getCommunicationLogsByChannel(Long channelId);

  List<CommunicationLogResponse> getCommunicationLogsByStatus(Long statusId);

  List<CommunicationLogResponse> getCommunicationLogsByPurpose(Long purposeId);

  List<CommunicationLogResponse> getCommunicationLogsByDirection(Long directionId);

  void deleteCommunicationLog(Long id);
}
