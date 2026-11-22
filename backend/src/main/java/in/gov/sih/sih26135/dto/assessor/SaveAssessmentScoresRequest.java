package in.gov.sih.sih26135.dto.assessor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request payload for saving drafts or finalizing assessment scores for candidate roster.
 */
public class SaveAssessmentScoresRequest {

  @NotEmpty(message = "Candidate scores list cannot be empty")
  @Valid
  private List<CandidateScoreInput> candidates;

  private boolean isDraft = true;

  public SaveAssessmentScoresRequest() {
  }

  public SaveAssessmentScoresRequest(List<CandidateScoreInput> candidates, boolean isDraft) {
    this.candidates = candidates;
    this.isDraft = isDraft;
  }

  public List<CandidateScoreInput> getCandidates() {
    return candidates;
  }

  public void setCandidates(List<CandidateScoreInput> candidates) {
    this.candidates = candidates;
  }

  public boolean isDraft() {
    return isDraft;
  }

  public void setDraft(boolean draft) {
    isDraft = draft;
  }
}
