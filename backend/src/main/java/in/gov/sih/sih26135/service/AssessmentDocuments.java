package in.gov.sih.sih26135.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import in.gov.sih.sih26135.service.impl.AssessorWorkbenchServiceImpl.AssessmentComponentScoresJson;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssessmentDocuments {
  private final JdbcTemplate jdbc;
  private final WorkflowSecurityScopeValidator scope;
  private final ObjectMapper mapper;

  public AssessmentDocuments(JdbcTemplate jdbc, WorkflowSecurityScopeValidator scope, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.scope = scope;
    this.mapper = mapper;
  }

  public Document result(long id) {
    var caller = scope.resolveAuthenticatedUser().orElseThrow(() -> new AccessDeniedException("Sign in to download your document."));
    return jdbc.query("""
        SELECT a.evaluator_user_id,t.user_id,b.provider_id,ar.assessment_result_id,
          CONCAT_WS(' ',t.first_name,NULLIF(t.middle_name,''),NULLIF(t.last_name,'')) candidate,t.registration_number,
          b.batch_code,a.assessment_name,p.provider_name,tc.center_name,b.start_date,b.end_date,
          a.assessment_date,pub.venue,ar.obtained_score,ar.maximum_score,a.pass_score,ar.remarks,ao.outcome_code,
          cert.certificate_number,cert.issuing_body,cert.issue_date,cert.expiry_date,cs.status_name,vs.status_name verification
        FROM assessment_results ar JOIN trainee_assessments ta USING (trainee_assessment_id)
        JOIN assessments a USING (assessment_id)
        JOIN trainees t ON t.trainee_id=ta.trainee_id
        JOIN training_batches b ON b.batch_id=a.batch_id
        JOIN training_providers p ON p.provider_id=b.provider_id
        LEFT JOIN training_centers tc ON tc.center_id=b.center_id
        JOIN ref_assessment_outcome ao USING (assessment_outcome_id)
        LEFT JOIN batch_assessment_publications pub ON pub.assessment_id=a.assessment_id
        LEFT JOIN certifications cert ON cert.certification_id=(SELECT MIN(c.certification_id) FROM certifications c
          WHERE c.assessment_result_id=ar.assessment_result_id AND c.deleted_at IS NULL)
        LEFT JOIN ref_certificate_status cs ON cs.certificate_status_id=cert.certificate_status_id
        LEFT JOIN ref_certificate_verification_status vs ON vs.certificate_verification_status_id=cert.certificate_verification_status_id
        WHERE ar.assessment_result_id=? AND ar.result_declared_at IS NOT NULL AND a.deleted_at IS NULL
        """, (rs, row) -> {
      boolean ownsResult = Objects.equals(caller.getId(), rs.getObject("user_id", Long.class))
          || Objects.equals(caller.getId(), rs.getObject("evaluator_user_id", Long.class))
          || (caller.getTrainingProviderId() != null && caller.getTrainingProviderId().equals(rs.getObject("provider_id", Long.class)));
      if (!ownsResult && !scope.isNationwideAdmin()) throw new AccessDeniedException("This assessment result is outside your scope.");
      return render(rs);
    }, id).stream().findFirst().orElseThrow(() -> new ResourceNotFoundException("No finalized assessment document is available."));
  }

  private Document render(ResultSet rs) throws SQLException {
    String outcome = rs.getString("outcome_code");
    String certificate = rs.getString("certificate_number");
    String issuer = rs.getString("issuing_body");
    boolean competency = "PASS".equals(outcome) && certificate != null && issuer != null && !issuer.isBlank();
    String title = competency ? "Certificate of Competency" : "Assessment Result";
    var components = componentScores(mapper, rs.getString("remarks"));
    String total = score(rs.getBigDecimal("obtained_score"));
    String maximum = score(rs.getBigDecimal("maximum_score"));
    String absent = "ABSENT".equals(outcome) ? "Not assessed" : null;
    String venue = rs.getString("venue") == null ? rs.getString("center_name") : rs.getString("venue");
    String footer = competency
        ? "Certificate " + certificate + "  |  Issued " + rs.getString("issue_date")
            + "  |  Expires " + rs.getString("expiry_date") + "  |  " + rs.getString("status_name")
        : "PASS".equals(outcome) ? "Competency certificate pending eligibility or an eligible issuing body."
            : "This document records the trainee's assessment outcome.";
    byte[] bytes = LandscapeAssessmentPdf.render(title, outcome, rs.getString("candidate"), rs.getString("registration_number"),
        List.of(new LandscapeAssessmentPdf.Field("TRAINING", rs.getString("assessment_name")),
            new LandscapeAssessmentPdf.Field("BATCH", rs.getString("batch_code")),
            new LandscapeAssessmentPdf.Field("TRAINING PROVIDER", rs.getString("provider_name")),
            new LandscapeAssessmentPdf.Field("TRAINING DATES", rs.getString("start_date") + " to " + rs.getString("end_date")),
            new LandscapeAssessmentPdf.Field("ASSESSMENT DATE", rs.getString("assessment_date")),
            new LandscapeAssessmentPdf.Field("ASSESSMENT VENUE", venue)),
        List.of(absent == null ? score(components.getTheory()) : absent,
            absent == null ? score(components.getPractical()) : absent,
            absent == null ? score(components.getViva()) : absent, absent == null ? total + " / " + maximum : absent),
        competency ? "Awarding organization: " + issuer : "Result reference: " + rs.getLong("assessment_result_id"),
        footer, competency && rs.getString("verification") != null ? "Verification: " + rs.getString("verification") : null);
    return new Document((competency ? "Certificate_" : "Assessment_Result_") + rs.getLong("assessment_result_id") + ".pdf", bytes);
  }

  private static String score(BigDecimal number) {
    return number == null ? "Pending" : number.stripTrailingZeros().toPlainString();
  }

  static AssessmentComponentScoresJson componentScores(ObjectMapper mapper, String remarks) {
    if (remarks != null && remarks.stripLeading().startsWith("{")) {
      try {
        var scores = mapper.readValue(remarks, AssessmentComponentScoresJson.class);
        if (scores != null) return scores;
      } catch (JsonProcessingException ignored) {
        // Historical records can contain plain remarks instead of component scores.
      }
    }
    return new AssessmentComponentScoresJson();
  }

  public record Document(String filename, byte[] bytes) {}
}
