package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefAssessmentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefAssessmentTypeRepository extends JpaRepository<RefAssessmentType, Long> {

  Optional<RefAssessmentType> findByTypeCode(String typeCode);

  boolean existsByTypeCode(String typeCode);

  List<RefAssessmentType> findAllByOrderBySortOrderAsc();
}
