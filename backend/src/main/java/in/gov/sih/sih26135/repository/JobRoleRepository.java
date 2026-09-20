package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.JobRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRoleRepository extends JpaRepository<JobRole, Long> {

  Optional<JobRole> findByJobRoleCode(String jobRoleCode);

  boolean existsByJobRoleCode(String jobRoleCode);

  List<JobRole> findBySectorId(Long sectorId);

  List<JobRole> findByIndustryId(Long industryId);

  List<JobRole> findByQualificationLevelId(Long qualificationLevelId);

  List<JobRole> findByLifecycleStatusId(Long lifecycleStatusId);
}
