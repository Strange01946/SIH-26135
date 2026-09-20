package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefPlacementSource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefPlacementSourceRepository extends JpaRepository<RefPlacementSource, Long> {

  Optional<RefPlacementSource> findBySourceCode(String sourceCode);

  boolean existsBySourceCode(String sourceCode);
}
