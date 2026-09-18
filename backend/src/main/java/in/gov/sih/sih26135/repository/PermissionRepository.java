package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Permission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  Optional<Permission> findByPermissionCode(String permissionCode);

  boolean existsByPermissionCode(String permissionCode);

  List<Permission> findByModuleCode(String moduleCode);
}
