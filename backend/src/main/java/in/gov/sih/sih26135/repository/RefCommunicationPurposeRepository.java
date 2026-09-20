package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefCommunicationPurpose;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefCommunicationPurposeRepository extends JpaRepository<RefCommunicationPurpose, Long> {

  Optional<RefCommunicationPurpose> findByPurposeCode(String purposeCode);

  boolean existsByPurposeCode(String purposeCode);

  List<RefCommunicationPurpose> findByRequiredConsentTypeId(Long consentTypeId);
}
