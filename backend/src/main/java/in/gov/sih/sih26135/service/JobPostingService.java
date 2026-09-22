package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateJobPostingRequest;
import in.gov.sih.sih26135.dto.request.UpdateJobPostingRequest;
import in.gov.sih.sih26135.dto.response.JobPostingResponse;
import java.util.List;

public interface JobPostingService {

  JobPostingResponse createJobPosting(CreateJobPostingRequest request);

  JobPostingResponse updateJobPosting(Long id, UpdateJobPostingRequest request);

  JobPostingResponse getJobPostingById(Long id);

  JobPostingResponse getJobPostingByCode(String postingCode);

  List<JobPostingResponse> getAllJobPostings(boolean includeDeleted);

  List<JobPostingResponse> getJobPostingsByEmployer(Long employerId);

  List<JobPostingResponse> getJobPostingsByJobRole(Long jobRoleId);

  List<JobPostingResponse> getJobPostingsByDistrict(Long districtId);

  void deleteJobPosting(Long id);
}
