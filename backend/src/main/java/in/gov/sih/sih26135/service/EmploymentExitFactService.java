package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.EmploymentExitFactResponse;
import in.gov.sih.sih26135.response.PagedResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface EmploymentExitFactService {

  PagedResponse<EmploymentExitFactResponse> getAllEmploymentExits(Pageable pageable);

  List<EmploymentExitFactResponse> getAllEmploymentExits();

  EmploymentExitFactResponse getEmploymentExitById(Long employmentExitEventId);

  List<EmploymentExitFactResponse> getEmploymentExitsByTraineeId(Long traineeId);

  PagedResponse<EmploymentExitFactResponse> getEmploymentExitsByTraineeId(Long traineeId, Pageable pageable);

  List<EmploymentExitFactResponse> getEmploymentExitsByEmploymentId(Long employmentId);

  PagedResponse<EmploymentExitFactResponse> getEmploymentExitsByEmploymentId(Long employmentId, Pageable pageable);

  List<EmploymentExitFactResponse> getEmploymentExitsByEmploymentExitReasonId(Long employmentExitReasonId);

  PagedResponse<EmploymentExitFactResponse> getEmploymentExitsByEmploymentExitReasonId(
      Long employmentExitReasonId, Pageable pageable);

  List<EmploymentExitFactResponse> getEmploymentExitsBySeparationNatureId(Long separationNatureId);

  PagedResponse<EmploymentExitFactResponse> getEmploymentExitsBySeparationNatureId(
      Long separationNatureId, Pageable pageable);

  List<EmploymentExitFactResponse> getEmploymentExitsByIsVoluntaryFlag(Boolean isVoluntaryFlag);

  PagedResponse<EmploymentExitFactResponse> getEmploymentExitsByIsVoluntaryFlag(
      Boolean isVoluntaryFlag, Pageable pageable);

  List<EmploymentExitFactResponse> getEmploymentExitsByIsInvoluntaryFlag(Boolean isInvoluntaryFlag);

  PagedResponse<EmploymentExitFactResponse> getEmploymentExitsByIsInvoluntaryFlag(
      Boolean isInvoluntaryFlag, Pageable pageable);
}
