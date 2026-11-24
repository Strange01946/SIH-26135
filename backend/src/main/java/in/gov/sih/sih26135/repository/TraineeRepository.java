package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Trainee;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface TraineeRepository extends JpaRepository<Trainee, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Trainee> findLockedById(Long id);

  Optional<Trainee> findByRegistrationNumber(String registrationNumber);

  boolean existsByRegistrationNumber(String registrationNumber);

  Optional<Trainee> findByUserId(Long userId);

  boolean existsByUserId(Long userId);

  Page<Trainee> findByDeletedAtIsNull(Pageable pageable);
}

