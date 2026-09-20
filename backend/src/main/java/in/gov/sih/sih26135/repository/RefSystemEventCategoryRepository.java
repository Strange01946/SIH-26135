package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefSystemEventCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSystemEventCategoryRepository extends
    JpaRepository<RefSystemEventCategory, Long> {

  Optional<RefSystemEventCategory> findByCategoryCode(String categoryCode);

  boolean existsByCategoryCode(String categoryCode);
}
