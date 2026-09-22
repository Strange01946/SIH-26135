package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateEmployerRequest;
import in.gov.sih.sih26135.dto.request.UpdateEmployerRequest;
import in.gov.sih.sih26135.dto.response.EmployerResponse;
import java.util.List;

public interface EmployerService {

  EmployerResponse createEmployer(CreateEmployerRequest request);

  EmployerResponse updateEmployer(Long id, UpdateEmployerRequest request);

  EmployerResponse getEmployerById(Long id);

  EmployerResponse getEmployerByCode(String employerCode);

  List<EmployerResponse> getAllEmployers(boolean includeDeleted);

  List<EmployerResponse> getEmployersByDistrict(Long districtId);

  List<EmployerResponse> getEmployersByIndustry(Long industryId);

  List<EmployerResponse> getEmployersBySector(Long sectorId);

  void deleteEmployer(Long id);
}
