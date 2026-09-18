package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Trainee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TraineeRepository extends JpaRepository<Trainee, Long> {

  Optional<Trainee> findByRegistrationNumber(String registrationNumber);

  boolean existsByRegistrationNumber(String registrationNumber);

  Optional<Trainee> findByUserId(Long userId);

  boolean existsByUserId(Long userId);
}

