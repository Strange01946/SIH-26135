package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.JobApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

  Optional<JobApplication> findByTraineeIdAndJobPostingId(Long traineeId, Long jobPostingId);

  boolean existsByTraineeIdAndJobPostingId(Long traineeId, Long jobPostingId);

  List<JobApplication> findByTraineeId(Long traineeId);

  List<JobApplication> findByJobPostingId(Long jobPostingId);

  List<JobApplication> findByEnrollmentId(Long enrollmentId);

  List<JobApplication> findByApplicationStatusId(Long applicationStatusId);

  List<JobApplication> findByJobPostingIdAndApplicationStatusId(Long jobPostingId, Long applicationStatusId);
}
