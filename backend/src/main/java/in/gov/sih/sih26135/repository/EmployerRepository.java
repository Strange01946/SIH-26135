package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Employer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployerRepository extends JpaRepository<Employer, Long> {

  Optional<Employer> findByEmployerCode(String employerCode);

  boolean existsByEmployerCode(String employerCode);

  Optional<Employer> findByRegistrationNumber(String registrationNumber);

  Optional<Employer> findByGstin(String gstin);

  List<Employer> findByStateId(Long stateId);

  List<Employer> findByDistrictId(Long districtId);

  List<Employer> findByIndustryId(Long industryId);

  List<Employer> findBySectorId(Long sectorId);

  List<Employer> findByCompanySizeId(Long companySizeId);

  List<Employer> findByRecordVerificationStatusId(Long recordVerificationStatusId);

  List<Employer> findByLifecycleStatusId(Long lifecycleStatusId);

  List<Employer> findByDistrictIdAndIndustryId(Long districtId, Long industryId);

  List<Employer> findByDeletedAtIsNull();
}
