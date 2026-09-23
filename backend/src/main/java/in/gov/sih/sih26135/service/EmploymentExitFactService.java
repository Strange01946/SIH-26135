package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentExitFactResponse;
import java.util.List;

public interface EmploymentExitFactService {

  List<EmploymentExitFactResponse> getAllEmploymentExits();

  EmploymentExitFactResponse getEmploymentExitById(Long employmentExitEventId);

  List<EmploymentExitFactResponse> getEmploymentExitsByTraineeId(Long traineeId);

  List<EmploymentExitFactResponse> getEmploymentExitsByEmploymentId(Long employmentId);

  List<EmploymentExitFactResponse> getEmploymentExitsByEmploymentExitReasonId(Long employmentExitReasonId);

  List<EmploymentExitFactResponse> getEmploymentExitsBySeparationNatureId(Long separationNatureId);

  List<EmploymentExitFactResponse> getEmploymentExitsByIsVoluntaryFlag(Boolean isVoluntaryFlag);

  List<EmploymentExitFactResponse> getEmploymentExitsByIsInvoluntaryFlag(Boolean isInvoluntaryFlag);
}
