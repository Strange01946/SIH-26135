package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSkillGapObservationRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapObservationRequest;
import in.gov.sih.sih26135.dto.response.SkillGapObservationResponse;
import java.time.LocalDate;
import java.util.List;

public interface SkillGapObservationService {

  SkillGapObservationResponse createObservation(CreateSkillGapObservationRequest request);

  SkillGapObservationResponse updateObservation(Long id, UpdateSkillGapObservationRequest request);

  SkillGapObservationResponse getObservationById(Long id);

  SkillGapObservationResponse getObservationBySkillGapAndNumber(Long skillGapId, Integer observationNumber);

  List<SkillGapObservationResponse> getObservationsBySkillGap(Long skillGapId);

  List<SkillGapObservationResponse> getObservationsBySeverity(Long severityId);

  List<SkillGapObservationResponse> getObservationsByStatus(Long statusId);

  List<SkillGapObservationResponse> getObservationsBySource(Long sourceId);

  List<SkillGapObservationResponse> getObservationsByDate(LocalDate observedOn);

  void deleteObservation(Long id);
}
