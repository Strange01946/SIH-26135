package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Industry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IndustryRepository extends JpaRepository<Industry, Long> {

  Optional<Industry> findByIndustryCode(String industryCode);

  boolean existsByIndustryCode(String industryCode);

  Optional<Industry> findByIndustryName(String industryName);

  boolean existsByIndustryName(String industryName);

  List<Industry> findBySectorId(Long sectorId);
}
