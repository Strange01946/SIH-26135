package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.request.UpdateSalaryHistoryRequest;
import in.gov.sih.sih26135.dto.response.SalaryHistoryResponse;
import java.time.LocalDate;
import java.util.List;

public interface SalaryHistoryService {

  SalaryHistoryResponse createSalaryHistory(CreateSalaryHistoryRequest request);

  SalaryHistoryResponse updateSalaryHistory(Long id, UpdateSalaryHistoryRequest request);

  SalaryHistoryResponse getSalaryHistoryById(Long id);

  List<SalaryHistoryResponse> getSalaryHistoryByEmploymentRecord(Long employmentRecordId);

  List<SalaryHistoryResponse> getSalaryHistoryByTrainee(Long traineeId);

  List<SalaryHistoryResponse> getSalaryHistoryByEffectiveFrom(LocalDate effectiveFrom);

  List<SalaryHistoryResponse> getSalaryHistoryByObservationMilestone(Integer observationMonthOffset);

  void deleteSalaryHistory(Long id);
}
