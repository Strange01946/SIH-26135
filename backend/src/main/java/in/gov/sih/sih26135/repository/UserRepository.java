package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  Optional<User> findByUsernameOrEmail(String username, String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email")
  Optional<User> findByUsernameOrEmailForUpdate(@Param("username") String username, @Param("email") String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);
}

