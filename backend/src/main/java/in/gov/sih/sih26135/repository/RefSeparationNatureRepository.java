package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefSeparationNature;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSeparationNatureRepository extends JpaRepository<RefSeparationNature, Long> {

  Optional<RefSeparationNature> findByNatureCode(String natureCode);

  boolean existsByNatureCode(String natureCode);
}
