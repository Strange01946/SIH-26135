package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentExitEventRequest;
import in.gov.sih.sih26135.dto.response.EmploymentExitEventResponse;
import java.time.LocalDate;
import java.util.List;

public interface EmploymentExitEventService {

  EmploymentExitEventResponse createExitEvent(CreateEmploymentExitEventRequest request);

  EmploymentExitEventResponse getExitEventById(Long id);

  EmploymentExitEventResponse getExitEventByEmploymentId(Long employmentId);

  List<EmploymentExitEventResponse> getExitEventsByTraineeId(Long traineeId);

  List<EmploymentExitEventResponse> getExitEventsByEnrollmentId(Long enrollmentId);

  List<EmploymentExitEventResponse> getExitEventsBySeparationDate(LocalDate separationDate);

  List<EmploymentExitEventResponse> getExitEventsByExitReasonId(Long exitReasonId);

  List<EmploymentExitEventResponse> getExitEventsBySeparationNatureId(Long separationNatureId);

  List<EmploymentExitEventResponse> getExitEventsByVerificationStatusId(Long statusId);

  EmploymentExitEventResponse updateExitEvent(Long id, UpdateEmploymentExitEventRequest request);

  void deleteExitEvent(Long id);
}
