package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SkillGapObservation;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillGapObservationRepository extends JpaRepository<SkillGapObservation, Long> {

  Optional<SkillGapObservation> findBySkillGapIdAndObservationNumber(Long skillGapId,
      Integer observationNumber);

  boolean existsBySkillGapIdAndObservationNumber(Long skillGapId, Integer observationNumber);

  List<SkillGapObservation> findBySkillGapId(Long skillGapId);

  List<SkillGapObservation> findBySkillGapSeverityId(Long severityId);

  List<SkillGapObservation> findBySkillGapStatusId(Long statusId);

  List<SkillGapObservation> findBySkillGapSourceId(Long sourceId);

  List<SkillGapObservation> findByObservedOn(LocalDate observedOn);
}
