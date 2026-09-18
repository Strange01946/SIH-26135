package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByRoleCode(String roleCode);

  boolean existsByRoleCode(String roleCode);
}
