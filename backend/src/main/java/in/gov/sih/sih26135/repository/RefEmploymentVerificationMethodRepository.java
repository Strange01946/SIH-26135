package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefEmploymentVerificationMethod;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefEmploymentVerificationMethodRepository extends
    JpaRepository<RefEmploymentVerificationMethod, Long> {

  Optional<RefEmploymentVerificationMethod> findByMethodCode(String methodCode);

  boolean existsByMethodCode(String methodCode);
}
