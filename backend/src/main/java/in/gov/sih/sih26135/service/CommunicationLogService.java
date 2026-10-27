package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.request.UpdateCommunicationLogRequest;
import in.gov.sih.sih26135.dto.response.CommunicationLogResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommunicationLogService {

  CommunicationLogResponse logCommunication(CreateCommunicationLogRequest request);

  CommunicationLogResponse updateCommunicationLog(Long id, UpdateCommunicationLogRequest request);

  CommunicationLogResponse getCommunicationLogById(Long id);

  CommunicationLogResponse getCommunicationLogByProviderMessageId(String providerMessageId);

  Page<CommunicationLogResponse> getAllCommunicationLogs(Pageable pageable);

  List<CommunicationLogResponse> getCommunicationLogsByTrainee(Long traineeId);

  Page<CommunicationLogResponse> getCommunicationLogsByTrainee(Long traineeId, Pageable pageable);

  List<CommunicationLogResponse> getCommunicationLogsByFollowupTask(Long followupTaskId);

  Page<CommunicationLogResponse> getCommunicationLogsByFollowupTask(Long followupTaskId, Pageable pageable);

  List<CommunicationLogResponse> getCommunicationLogsBySurvey(Long surveyId);

  Page<CommunicationLogResponse> getCommunicationLogsBySurvey(Long surveyId, Pageable pageable);

  List<CommunicationLogResponse> getCommunicationLogsBySurveyResponse(Long surveyResponseId);

  Page<CommunicationLogResponse> getCommunicationLogsBySurveyResponse(Long surveyResponseId, Pageable pageable);

  List<CommunicationLogResponse> getCommunicationLogsByChannel(Long channelId);

  Page<CommunicationLogResponse> getCommunicationLogsByChannel(Long channelId, Pageable pageable);

  List<CommunicationLogResponse> getCommunicationLogsByStatus(Long statusId);

  Page<CommunicationLogResponse> getCommunicationLogsByStatus(Long statusId, Pageable pageable);

  List<CommunicationLogResponse> getCommunicationLogsByPurpose(Long purposeId);

  Page<CommunicationLogResponse> getCommunicationLogsByPurpose(Long purposeId, Pageable pageable);

  List<CommunicationLogResponse> getCommunicationLogsByDirection(Long directionId);

  Page<CommunicationLogResponse> getCommunicationLogsByDirection(Long directionId, Pageable pageable);

  void deleteCommunicationLog(Long id);
}
