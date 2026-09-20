package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefCompanySize;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefCompanySizeRepository extends JpaRepository<RefCompanySize, Long> {

  Optional<RefCompanySize> findBySizeCode(String sizeCode);

  boolean existsBySizeCode(String sizeCode);
}
