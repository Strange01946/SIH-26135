package in.gov.sih.sih26135.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested data quality rule cannot be located.
 * Maps to HTTP 404 Not Found.
 */
public class DataQualityRuleNotFoundException extends BaseException {

  public DataQualityRuleNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND, OperationalErrorCodes.DQ_RULE_NOT_FOUND);
  }

  public DataQualityRuleNotFoundException(Long ruleId) {
    super("Data quality rule not found with id: " + ruleId, HttpStatus.NOT_FOUND, OperationalErrorCodes.DQ_RULE_NOT_FOUND);
  }

  public DataQualityRuleNotFoundException(String ruleCode, boolean isCode) {
    super("Data quality rule not found with code: " + ruleCode, HttpStatus.NOT_FOUND, OperationalErrorCodes.DQ_RULE_NOT_FOUND);
  }
}
