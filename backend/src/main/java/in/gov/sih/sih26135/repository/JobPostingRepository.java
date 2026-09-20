package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.JobPosting;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

  Optional<JobPosting> findByPostingCode(String postingCode);

  boolean existsByPostingCode(String postingCode);

  List<JobPosting> findByEmployerId(Long employerId);

  List<JobPosting> findByEmployerBranchId(Long employerBranchId);

  List<JobPosting> findByJobRoleId(Long jobRoleId);

  List<JobPosting> findByEngagementTypeId(Long engagementTypeId);

  List<JobPosting> findByJobPostingStatusId(Long jobPostingStatusId);

  List<JobPosting> findByStateId(Long stateId);

  List<JobPosting> findByDistrictId(Long districtId);

  List<JobPosting> findByJobRoleIdAndJobPostingStatusId(Long jobRoleId, Long jobPostingStatusId);

  List<JobPosting> findByEmployerIdAndJobPostingStatusId(Long employerId, Long jobPostingStatusId);

  List<JobPosting> findByDeletedAtIsNull();
}
