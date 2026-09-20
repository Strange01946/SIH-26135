package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefDataQualityCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefDataQualityCategoryRepository extends
    JpaRepository<RefDataQualityCategory, Long> {

  Optional<RefDataQualityCategory> findByCategoryCode(String categoryCode);

  boolean existsByCategoryCode(String categoryCode);
}
