package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.ProgramTrainingProvider;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramTrainingProviderRepository extends JpaRepository<ProgramTrainingProvider, Long> {

  List<ProgramTrainingProvider> findByProgramId(Long programId);

  List<ProgramTrainingProvider> findByTrainingProviderId(Long providerId);

  Optional<ProgramTrainingProvider> findByProgramIdAndTrainingProviderIdAndEmpanelledFrom(
      Long programId, Long providerId, LocalDate empanelledFrom);

  boolean existsByProgramIdAndTrainingProviderIdAndEmpanelledFrom(
      Long programId, Long providerId, LocalDate empanelledFrom);
}
