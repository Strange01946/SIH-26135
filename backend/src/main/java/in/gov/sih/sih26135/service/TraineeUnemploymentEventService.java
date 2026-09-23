package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.request.UpdateTraineeUnemploymentEventRequest;
import in.gov.sih.sih26135.dto.response.TraineeUnemploymentEventResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TraineeUnemploymentEventService {

  TraineeUnemploymentEventResponse createUnemploymentEvent(CreateTraineeUnemploymentEventRequest request);

  TraineeUnemploymentEventResponse getUnemploymentEventById(Long id);

  TraineeUnemploymentEventResponse getUnemploymentEventByTraineeIdAndPeriodNumber(Long traineeId, Integer periodNumber);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsByTraineeId(Long traineeId);

  List<TraineeUnemploymentEventResponse> getCurrentUnemploymentEventsByTraineeId(Long traineeId);

  Optional<TraineeUnemploymentEventResponse> getCurrentPeriodByTraineeId(Long traineeId);

  Optional<TraineeUnemploymentEventResponse> getUnemploymentEventByExitEventId(Long exitEventId);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsByStartDate(LocalDate startDate);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsByReasonId(Long reasonId);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsByLabourStatusId(Long labourStatusId);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsByPrecedingEmploymentId(Long employmentId);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsBySucceedingEmploymentId(Long employmentId);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsByEnrollmentId(Long enrollmentId);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsByPlacementId(Long placementId);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsByFollowupTaskId(Long followupTaskId);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsBySurveyResponseId(Long surveyResponseId);

  List<TraineeUnemploymentEventResponse> getUnemploymentEventsByVerificationStatusId(Long statusId);

  TraineeUnemploymentEventResponse updateUnemploymentEvent(Long id, UpdateTraineeUnemploymentEventRequest request);

  void deleteUnemploymentEvent(Long id);
}
