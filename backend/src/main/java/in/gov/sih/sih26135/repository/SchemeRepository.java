package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Scheme;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {

  Optional<Scheme> findBySchemeCode(String schemeCode);

  boolean existsBySchemeCode(String schemeCode);

  List<Scheme> findByDepartmentId(Long departmentId);
}
