package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Program;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {

  Optional<Program> findByProgramCode(String programCode);

  boolean existsByProgramCode(String programCode);

  List<Program> findBySchemeId(Long schemeId);

  List<Program> findByDepartmentId(Long departmentId);
}
