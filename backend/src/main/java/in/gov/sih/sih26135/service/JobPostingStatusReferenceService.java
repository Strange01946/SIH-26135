package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.JobPostingStatusResponse;
import java.util.List;

public interface JobPostingStatusReferenceService {

  List<JobPostingStatusResponse> getAllJobPostingStatuses();

  JobPostingStatusResponse getById(Long id);

  JobPostingStatusResponse getByCode(String statusCode);

  List<JobPostingStatusResponse> getByIsOpenFlag(Boolean isOpenFlag);
}
