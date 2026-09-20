package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefEngagementType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefEngagementTypeRepository extends JpaRepository<RefEngagementType, Long> {

  Optional<RefEngagementType> findByTypeCode(String typeCode);

  boolean existsByTypeCode(String typeCode);

  List<RefEngagementType> findByRequiresEmployer(Boolean requiresEmployer);
}
