package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RolePermission;
import in.gov.sih.sih26135.entity.RolePermissionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

  List<RolePermission> findByRoleId(Long roleId);

  List<RolePermission> findByPermissionId(Long permissionId);

  boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
}

