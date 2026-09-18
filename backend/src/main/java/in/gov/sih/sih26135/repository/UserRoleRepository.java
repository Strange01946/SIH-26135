package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.UserRole;
import in.gov.sih.sih26135.entity.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

  List<UserRole> findByUserId(Long userId);

  List<UserRole> findByRoleId(Long roleId);

  boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}

