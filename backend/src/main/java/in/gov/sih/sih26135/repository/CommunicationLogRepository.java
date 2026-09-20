package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.CommunicationLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, Long> {

  Optional<CommunicationLog> findByProviderMessageId(String providerMessageId);

  boolean existsByProviderMessageId(String providerMessageId);

  List<CommunicationLog> findByTraineeId(Long traineeId);

  List<CommunicationLog> findByFollowupTaskId(Long followupTaskId);

  List<CommunicationLog> findBySurveyId(Long surveyId);

  List<CommunicationLog> findBySurveyResponseId(Long surveyResponseId);

  List<CommunicationLog> findByCommunicationChannelId(Long communicationChannelId);

  List<CommunicationLog> findByCommunicationStatusId(Long communicationStatusId);

  List<CommunicationLog> findByCommunicationPurposeId(Long communicationPurposeId);

  List<CommunicationLog> findByCommunicationDirectionId(Long communicationDirectionId);
}
