package in.gov.sih.sih26135.dto.request;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateSurveyResponseAnswerRequest {

  private Long selectedOptionId;
  @Size(max = 1000, message = "answerText cannot exceed 1000 characters")
  private String answerText;
  @PositiveOrZero(message = "numericValue must be greater than or equal to 0")
  @Digits(integer = 10, fraction = 2, message = "numericValue numeric value out of bounds (<10 digits>.<2 digits> expected)")
  private BigDecimal numericValue;
  private Boolean booleanValue;
  private LocalDate dateValue;

  public UpdateSurveyResponseAnswerRequest() {
  }

  public Long getSelectedOptionId() {
    return selectedOptionId;
  }

  public void setSelectedOptionId(Long selectedOptionId) {
    this.selectedOptionId = selectedOptionId;
  }

  public String getAnswerText() {
    return answerText;
  }

  public void setAnswerText(String answerText) {
    this.answerText = answerText;
  }

  public BigDecimal getNumericValue() {
    return numericValue;
  }

  public void setNumericValue(BigDecimal numericValue) {
    this.numericValue = numericValue;
  }

  public Boolean getBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(Boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  public LocalDate getDateValue() {
    return dateValue;
  }

  public void setDateValue(LocalDate dateValue) {
    this.dateValue = dateValue;
  }
}
