package in.gov.sih.sih26135.dto.trainee;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import in.gov.sih.sih26135.dto.response.TraineeResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record TraineeSelfProfile(
    TraineeResponse profile,
    String profileStatus,
    String employmentStatus,
    String blockName,
    String locationName,
    List<EducationOption> educationLevels,
    @JsonInclude(JsonInclude.Include.NON_NULL) TraineeSurvey.CareerUpdate reportedUpdate) {

  public record EducationOption(long id, String name) {}

  public record Update(
      @NotBlank @Size(max = 80) String firstName,
      @Size(max = 80) String middleName,
      @Size(max = 80) String lastName,
      @PastOrPresent(message = "Date of birth cannot be in the future") LocalDate dateOfBirth,
      @NotNull @Positive Long genderId,
      @Email @Size(max = 255) String email,
      @Size(max = 15) String phone,
      @Size(max = 200) String addressLine1,
      @Size(max = 200) String addressLine2,
      @NotNull @Positive Long stateId,
      @NotNull @Positive Long districtId,
      @NotBlank(message = "PIN code is required")
      @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Enter a valid six-digit PIN code") String pincode,
      @Positive Long educationLevelId) {

    @JsonAnySetter
    public void rejectUnsupportedField(String field, JsonNode value) {
      throw new IllegalArgumentException("Unsupported trainee profile field");
    }
  }
}
