package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.EmployerBranch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployerBranchRepository extends JpaRepository<EmployerBranch, Long> {

  Optional<EmployerBranch> findByEmployerIdAndBranchCode(Long employerId, String branchCode);

  boolean existsByEmployerIdAndBranchCode(Long employerId, String branchCode);

  List<EmployerBranch> findByEmployerId(Long employerId);

  List<EmployerBranch> findByEmployerIdAndIsHeadOfficeTrue(Long employerId);

  List<EmployerBranch> findByStateId(Long stateId);

  List<EmployerBranch> findByDistrictId(Long districtId);

  List<EmployerBranch> findByLifecycleStatusId(Long lifecycleStatusId);

  List<EmployerBranch> findByDeletedAtIsNull();
}
