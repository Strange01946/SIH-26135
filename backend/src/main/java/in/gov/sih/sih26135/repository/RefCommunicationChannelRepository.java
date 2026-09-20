package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefCommunicationChannel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefCommunicationChannelRepository extends JpaRepository<RefCommunicationChannel, Long> {

  Optional<RefCommunicationChannel> findByChannelCode(String channelCode);

  boolean existsByChannelCode(String channelCode);

  List<RefCommunicationChannel> findByRequiredConsentTypeId(Long consentTypeId);
}
