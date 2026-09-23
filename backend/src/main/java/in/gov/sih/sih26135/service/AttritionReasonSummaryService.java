package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.AttritionReasonSummaryResponse;
import java.util.List;

public interface AttritionReasonSummaryService {

  List<AttritionReasonSummaryResponse> getAllAttritionReasonSummaries();

  List<AttritionReasonSummaryResponse> getAllAttritionReasonSummariesOrderByExitCountDesc();

  AttritionReasonSummaryResponse getAttritionReasonSummaryById(Long employmentExitReasonId, Long separationNatureId);

  List<AttritionReasonSummaryResponse> getAttritionReasonSummariesByExitReasonId(Long employmentExitReasonId);

  List<AttritionReasonSummaryResponse> getAttritionReasonSummariesBySeparationNatureId(Long separationNatureId);

  List<AttritionReasonSummaryResponse> getAttritionReasonSummariesByIsVoluntaryFlag(Boolean isVoluntaryFlag);

  List<AttritionReasonSummaryResponse> getAttritionReasonSummariesByIsInvoluntaryFlag(Boolean isInvoluntaryFlag);
}
