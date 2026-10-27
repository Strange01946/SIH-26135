package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmploymentRecordRequest;
import in.gov.sih.sih26135.dto.response.EmploymentRecordResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmploymentRecordService {

  EmploymentRecordResponse createEmploymentRecord(CreateEmploymentRecordRequest request);

  EmploymentRecordResponse updateEmploymentRecord(Long id, UpdateEmploymentRecordRequest request);

  EmploymentRecordResponse getEmploymentRecordById(Long id);

  EmploymentRecordResponse getEmploymentRecordByNumber(String employmentNumber);

  EmploymentRecordResponse getEmploymentRecordByPlacementId(Long placementId);

  List<EmploymentRecordResponse> getAllEmploymentRecords(boolean includeDeleted);

  Page<EmploymentRecordResponse> getAllEmploymentRecords(boolean includeDeleted, Pageable pageable);

  List<EmploymentRecordResponse> getEmploymentRecordsByTrainee(Long traineeId);

  Page<EmploymentRecordResponse> getEmploymentRecordsByTrainee(Long traineeId, Pageable pageable);

  List<EmploymentRecordResponse> getEmploymentRecordsByEmployer(Long employerId);

  Page<EmploymentRecordResponse> getEmploymentRecordsByEmployer(Long employerId, Pageable pageable);

  List<EmploymentRecordResponse> getEmploymentRecordsByStatus(Long statusId);

  Page<EmploymentRecordResponse> getEmploymentRecordsByStatus(Long statusId, Pageable pageable);

  List<EmploymentRecordResponse> getCurrentEmploymentRecordsByTrainee(Long traineeId);

  Page<EmploymentRecordResponse> getCurrentEmploymentRecordsByTrainee(Long traineeId, Pageable pageable);

  void deleteEmploymentRecord(Long id);
}
