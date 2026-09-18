package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefConsentType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefConsentTypeRepository extends JpaRepository<RefConsentType, Long> {

  Optional<RefConsentType> findByConsentCode(String consentCode);

  boolean existsByConsentCode(String consentCode);
}
