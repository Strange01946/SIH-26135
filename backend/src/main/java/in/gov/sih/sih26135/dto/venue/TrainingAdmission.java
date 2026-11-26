package in.gov.sih.sih26135.dto.venue;

public record TrainingAdmission(Long minimumEducationLevelId, String minimumEducationLevel, String recommendedPrerequisites) {
  public record Level(long id, String name) {}

  public enum EducationEligibility {
    ELIGIBLE, EDUCATION_REQUIRED, BELOW_MINIMUM
  }
}
