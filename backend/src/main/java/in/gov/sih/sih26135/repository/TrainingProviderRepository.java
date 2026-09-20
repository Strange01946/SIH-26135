package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.TrainingProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingProviderRepository extends JpaRepository<TrainingProvider, Long> {

  Optional<TrainingProvider> findByProviderCode(String providerCode);

  boolean existsByProviderCode(String providerCode);

  Optional<TrainingProvider> findByRegistrationNumber(String registrationNumber);

  boolean existsByRegistrationNumber(String registrationNumber);

  List<TrainingProvider> findByStateId(Long stateId);

  List<TrainingProvider> findByDistrictId(Long districtId);
}
