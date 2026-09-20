package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefSalaryFrequency;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefSalaryFrequencyRepository extends JpaRepository<RefSalaryFrequency, Long> {

  Optional<RefSalaryFrequency> findByFrequencyCode(String frequencyCode);

  boolean existsByFrequencyCode(String frequencyCode);
}
