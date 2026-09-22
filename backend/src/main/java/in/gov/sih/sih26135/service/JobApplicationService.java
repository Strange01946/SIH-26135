package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateJobApplicationRequest;
import in.gov.sih.sih26135.dto.request.UpdateJobApplicationRequest;
import in.gov.sih.sih26135.dto.response.JobApplicationResponse;
import java.util.List;

public interface JobApplicationService {

  JobApplicationResponse createJobApplication(CreateJobApplicationRequest request);

  JobApplicationResponse updateJobApplication(Long id, UpdateJobApplicationRequest request);

  JobApplicationResponse getJobApplicationById(Long id);

  JobApplicationResponse getJobApplicationByTraineeAndPosting(Long traineeId, Long jobPostingId);

  List<JobApplicationResponse> getJobApplicationsByTrainee(Long traineeId);

  List<JobApplicationResponse> getJobApplicationsByPosting(Long jobPostingId);

  List<JobApplicationResponse> getJobApplicationsByStatus(Long applicationStatusId);

  void deleteJobApplication(Long id);
}
