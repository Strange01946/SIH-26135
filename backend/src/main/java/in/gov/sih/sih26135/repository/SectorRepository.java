package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Sector;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {

  Optional<Sector> findBySectorCode(String sectorCode);

  boolean existsBySectorCode(String sectorCode);

  Optional<Sector> findBySectorName(String sectorName);

  boolean existsBySectorName(String sectorName);
}
