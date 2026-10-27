package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.CommunicationLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  Page<CommunicationLog> findByTraineeId(Long traineeId, Pageable pageable);

  Page<CommunicationLog> findByFollowupTaskId(Long followupTaskId, Pageable pageable);

  Page<CommunicationLog> findBySurveyId(Long surveyId, Pageable pageable);

  Page<CommunicationLog> findBySurveyResponseId(Long surveyResponseId, Pageable pageable);

  Page<CommunicationLog> findByCommunicationChannelId(Long communicationChannelId, Pageable pageable);

  Page<CommunicationLog> findByCommunicationStatusId(Long communicationStatusId, Pageable pageable);

  Page<CommunicationLog> findByCommunicationPurposeId(Long communicationPurposeId, Pageable pageable);

  Page<CommunicationLog> findByCommunicationDirectionId(Long communicationDirectionId, Pageable pageable);
}
