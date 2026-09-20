package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefEmploymentVerificationEvidenceType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefEmploymentVerificationEvidenceTypeRepository extends
    JpaRepository<RefEmploymentVerificationEvidenceType, Long> {

  Optional<RefEmploymentVerificationEvidenceType> findByTypeCode(String typeCode);

  boolean existsByTypeCode(String typeCode);
}
