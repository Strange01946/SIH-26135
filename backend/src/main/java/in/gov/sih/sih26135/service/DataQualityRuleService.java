package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateDataQualityRuleRequest;
import in.gov.sih.sih26135.dto.request.UpdateDataQualityRuleRequest;
import in.gov.sih.sih26135.dto.response.DataQualityRuleResponse;
import java.util.List;

public interface DataQualityRuleService {

  DataQualityRuleResponse createRule(CreateDataQualityRuleRequest request);

  DataQualityRuleResponse updateRule(Long id, UpdateDataQualityRuleRequest request);

  DataQualityRuleResponse getRuleById(Long id);

  DataQualityRuleResponse getRuleByCode(String ruleCode);

  List<DataQualityRuleResponse> getRulesByTargetEntityType(String targetEntityType);

  List<DataQualityRuleResponse> getRulesByCategoryId(Long categoryId);

  List<DataQualityRuleResponse> getRulesBySeverityId(Long severityId);

  List<DataQualityRuleResponse> getRulesByLifecycleStatusId(Long lifecycleStatusId);

  void deleteRule(Long id);
}
