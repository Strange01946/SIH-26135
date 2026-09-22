package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import java.util.List;

public interface EmploymentRecordService {

  EmploymentRecordResponse createEmploymentRecord(CreateEmploymentRecordRequest request);

  EmploymentRecordResponse updateEmploymentRecord(Long id, UpdateEmploymentRecordRequest request);

  EmploymentRecordResponse getEmploymentRecordById(Long id);

  EmploymentRecordResponse getEmploymentRecordByNumber(String employmentNumber);

  EmploymentRecordResponse getEmploymentRecordByPlacementId(Long placementId);

  List<EmploymentRecordResponse> getAllEmploymentRecords(boolean includeDeleted);

  List<EmploymentRecordResponse> getEmploymentRecordsByTrainee(Long traineeId);

  List<EmploymentRecordResponse> getEmploymentRecordsByEmployer(Long employerId);

  List<EmploymentRecordResponse> getEmploymentRecordsByStatus(Long statusId);

  List<EmploymentRecordResponse> getCurrentEmploymentRecordsByTrainee(Long traineeId);

  void deleteEmploymentRecord(Long id);
}
