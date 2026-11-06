package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.response.FollowupDashboardResponse;
import in.gov.sih.sih26135.dto.response.FollowupSlaResponse;
import in.gov.sih.sih26135.dto.response.FollowupWavePerformanceResponse;
import in.gov.sih.sih26135.dto.response.SurveyDashboardResponse;
import in.gov.sih.sih26135.dto.response.SurveyPurposePipelineResponse;
import in.gov.sih.sih26135.dto.response.SurveySatisfactionResponse;
import in.gov.sih.sih26135.dto.response.VerificationDashboardResponse;
import in.gov.sih.sih26135.dto.response.VerificationMethodEfficacyResponse;
import in.gov.sih.sih26135.dto.response.VerificationStatusBreakdownResponse;
import in.gov.sih.sih26135.repository.analytics.DataIntegrityDashboardRepository;
import in.gov.sih.sih26135.repository.analytics.projection.FollowupOverallSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.FollowupWaveProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SurveyOverallSummaryProjection;
import in.gov.sih.sih26135.repository.analytics.projection.SurveyResponsePipelineProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationMethodEfficacyProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationOutcomeProjection;
import in.gov.sih.sih26135.repository.analytics.projection.VerificationOverallSummaryProjection;
import in.gov.sih.sih26135.service.DataIntegrityDashboardService;
import in.gov.sih.sih26135.util.AnalyticsCalculationUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Verification Integrity, Longitudinal Follow-up, and Survey Analytics.
 */
@Service
@Transactional(readOnly = true)
public class DataIntegrityDashboardServiceImpl implements DataIntegrityDashboardService {

  private static final String SLA_UNCONFIGURED_MESSAGE =
      "SLA tracking deferred: schema defines scheduled_date and completed_at but lacks explicit target deadlines, due_date columns, or grace period thresholds in ref_followup_type or followup_campaigns.";

  private static final String SATISFACTION_UNCONFIGURED_MESSAGE =
      "Satisfaction rating metrics deferred: survey questions are dynamically defined per template version with no standardized rating question in ref_survey_purpose or vw_survey_response_fact.";

  private final DataIntegrityDashboardRepository repository;

  public DataIntegrityDashboardServiceImpl(DataIntegrityDashboardRepository repository) {
    this.repository = repository;
  }

  @Override
  public VerificationDashboardResponse getVerificationDashboard() {
    List<VerificationOutcomeProjection> outcomes = repository.getFormalQ21VerificationOutcomes();
    List<VerificationMethodEfficacyProjection> methodProjections = repository.getFormalQ22VerificationMethodEfficacy();
    VerificationOverallSummaryProjection summary = repository.getFormalQ21Q22OverallSummary();

    Long totalVerifications;
    Long totalEmployments;
    Long totalTrainees;
    Long verifiedCount;
    Long officialVerifiedCount;
    Long selfReportedCount;
    Long pendingCount;
    Long rejectedCount;
    Long unverifiedCount;

    if (summary != null) {
      totalVerifications = summary.totalVerifications() != null ? summary.totalVerifications() : 0L;
      totalEmployments = summary.totalEmployments() != null ? summary.totalEmployments() : 0L;
      totalTrainees = summary.totalTrainees() != null ? summary.totalTrainees() : 0L;
      verifiedCount = summary.verifiedCount() != null ? summary.verifiedCount() : 0L;
      officialVerifiedCount = summary.officialVerifiedCount() != null ? summary.officialVerifiedCount() : 0L;
      selfReportedCount = summary.selfReportedCount() != null ? summary.selfReportedCount() : 0L;
      pendingCount = summary.pendingCount() != null ? summary.pendingCount() : 0L;
      rejectedCount = summary.rejectedCount() != null ? summary.rejectedCount() : 0L;
      unverifiedCount = summary.unverifiedCount() != null ? summary.unverifiedCount() : 0L;
    } else {
      totalEmployments = null; // Distinct count cannot be safely summed across statuses
      totalTrainees = null;   // Distinct count cannot be safely summed across statuses
      if (outcomes != null && !outcomes.isEmpty()) {
        long vTotal = 0L;
        long vVerified = 0L;
        long vPending = 0L;
        long vRejected = 0L;
        long vUnverified = 0L;
        long vSelf = 0L;
        for (VerificationOutcomeProjection o : outcomes) {
          if (o.verificationCount() != null) {
            vTotal += o.verificationCount();
            if ("VERIFIED".equalsIgnoreCase(o.recordVerificationStatusCode())) {
              vVerified += o.verificationCount();
            } else if ("PENDING".equalsIgnoreCase(o.recordVerificationStatusCode())) {
              vPending += o.verificationCount();
            } else if ("REJECTED".equalsIgnoreCase(o.recordVerificationStatusCode())) {
              vRejected += o.verificationCount();
            } else if ("UNVERIFIED".equalsIgnoreCase(o.recordVerificationStatusCode())) {
              vUnverified += o.verificationCount();
            } else if ("SELF_REPORTED".equalsIgnoreCase(o.recordVerificationStatusCode())) {
              vSelf += o.verificationCount();
            }
          }
        }
        totalVerifications = vTotal;
        verifiedCount = vVerified;
        officialVerifiedCount = null;
        selfReportedCount = vSelf;
        pendingCount = vPending;
        rejectedCount = vRejected;
        unverifiedCount = vUnverified;
      } else {
        totalVerifications = 0L;
        verifiedCount = 0L;
        officialVerifiedCount = 0L;
        selfReportedCount = 0L;
        pendingCount = 0L;
        rejectedCount = 0L;
        unverifiedCount = 0L;
      }
    }

    BigDecimal verificationCompletionRatePct = AnalyticsCalculationUtils.calculatePercentage(
        verifiedCount != null && rejectedCount != null ? verifiedCount + rejectedCount : verifiedCount,
        totalVerifications
    );
    BigDecimal verificationSuccessRatePct = AnalyticsCalculationUtils.calculatePercentage(
        verifiedCount, totalVerifications
    );
    BigDecimal officialVerificationRatePct = AnalyticsCalculationUtils.calculatePercentage(
        officialVerifiedCount, totalVerifications
    );
    BigDecimal selfReportedRatePct = AnalyticsCalculationUtils.calculatePercentage(
        selfReportedCount, totalVerifications
    );
    BigDecimal pendingRatePct = AnalyticsCalculationUtils.calculatePercentage(
        pendingCount, totalVerifications
    );

    List<VerificationStatusBreakdownResponse> statusBreakdown = new ArrayList<>();
    if (outcomes != null) {
      for (VerificationOutcomeProjection o : outcomes) {
        BigDecimal sharePct = AnalyticsCalculationUtils.calculatePercentage(
            o.verificationCount(), totalVerifications
        );
        statusBreakdown.add(new VerificationStatusBreakdownResponse(
            o.recordVerificationStatusCode(),
            o.statusName(),
            o.isVerifiedFlag(),
            o.verificationCount(),
            o.traineeCount(),
            o.employmentCount(),
            sharePct
        ));
      }
    }

    // Aggregate Q22 by method code across statuses to produce method-level efficacy
    List<VerificationMethodEfficacyResponse> methodEfficacyBreakdown = new ArrayList<>();
    if (methodProjections != null) {
      Map<String, MethodAccumulator> methodMap = new LinkedHashMap<>();
      long totalCurrent = 0L;

      for (VerificationMethodEfficacyProjection mp : methodProjections) {
        MethodAccumulator acc = methodMap.computeIfAbsent(
            mp.verificationMethodCode(),
            k -> new MethodAccumulator(mp.verificationMethodCode(), mp.methodName(), mp.isOfficialFlag(), mp.isTraineeSelfReported())
        );
        long count = mp.currentVerificationCount() != null ? mp.currentVerificationCount() : 0L;
        acc.currentVerificationCount += count;
        totalCurrent += count;

        if (mp.traineeCount() != null) {
          acc.traineeCount = Math.max(acc.traineeCount, mp.traineeCount());
        }

        if ("VERIFIED".equalsIgnoreCase(mp.recordVerificationStatusCode())) {
          acc.verifiedCount += count;
        } else if ("PENDING".equalsIgnoreCase(mp.recordVerificationStatusCode())) {
          acc.pendingCount += count;
        } else if ("REJECTED".equalsIgnoreCase(mp.recordVerificationStatusCode())) {
          acc.rejectedCount += count;
        }
      }

      for (MethodAccumulator acc : methodMap.values()) {
        BigDecimal successRatePct = AnalyticsCalculationUtils.calculatePercentage(
            acc.verifiedCount, acc.currentVerificationCount
        );
        BigDecimal sharePct = AnalyticsCalculationUtils.calculatePercentage(
            acc.currentVerificationCount, totalCurrent
        );
        methodEfficacyBreakdown.add(new VerificationMethodEfficacyResponse(
            acc.methodCode,
            acc.methodName,
            acc.isOfficial,
            acc.isTraineeSelfReported,
            acc.currentVerificationCount,
            acc.traineeCount,
            acc.verifiedCount,
            acc.pendingCount,
            acc.rejectedCount,
            successRatePct,
            sharePct
        ));
      }
    }

    return new VerificationDashboardResponse(
        totalVerifications,
        totalEmployments,
        totalTrainees,
        verifiedCount,
        officialVerifiedCount,
        selfReportedCount,
        pendingCount,
        rejectedCount,
        unverifiedCount,
        verificationCompletionRatePct,
        verificationSuccessRatePct,
        officialVerificationRatePct,
        selfReportedRatePct,
        pendingRatePct,
        statusBreakdown,
        methodEfficacyBreakdown
    );
  }

  @Override
  public FollowupDashboardResponse getFollowupDashboard() {
    List<FollowupWaveProjection> waves = repository.getFormalQ19FollowupSuccessByWave();
    FollowupOverallSummaryProjection summary = repository.getFormalQ19OverallSummary();

    Long totalTasks;
    Long totalTrainees;
    Long completedTasks;
    Long pendingTasks;
    Long successfulContacts;
    Long noResponseCount;
    Long unreachableCount;

    if (summary != null) {
      totalTasks = summary.totalTasks() != null ? summary.totalTasks() : 0L;
      totalTrainees = summary.totalTrainees() != null ? summary.totalTrainees() : 0L;
      completedTasks = summary.completedTasks() != null ? summary.completedTasks() : 0L;
      pendingTasks = summary.pendingTasks() != null ? summary.pendingTasks() : 0L;
      successfulContacts = summary.successfulContacts() != null ? summary.successfulContacts() : 0L;
      noResponseCount = summary.noResponseCount() != null ? summary.noResponseCount() : 0L;
      unreachableCount = summary.unreachableCount() != null ? summary.unreachableCount() : 0L;
    } else {
      totalTrainees = null; // Distinct count cannot be safely summed across waves
      if (waves != null && !waves.isEmpty()) {
        long tTasks = 0L;
        long tSuccess = 0L;
        long tNoResp = 0L;
        long tUnreach = 0L;
        for (FollowupWaveProjection w : waves) {
          if (w.taskCount() != null) {
            tTasks += w.taskCount();
          }
          if (w.successCount() != null) {
            tSuccess += w.successCount();
          }
          if (w.noResponseCount() != null) {
            tNoResp += w.noResponseCount();
          }
          if (w.unreachableCount() != null) {
            tUnreach += w.unreachableCount();
          }
        }
        totalTasks = tTasks;
        completedTasks = tSuccess + tNoResp + tUnreach;
        pendingTasks = Math.max(0L, tTasks - completedTasks);
        successfulContacts = tSuccess;
        noResponseCount = tNoResp;
        unreachableCount = tUnreach;
      } else {
        totalTasks = 0L;
        completedTasks = 0L;
        pendingTasks = 0L;
        successfulContacts = 0L;
        noResponseCount = 0L;
        unreachableCount = 0L;
      }
    }

    BigDecimal contactSuccessRatePct = AnalyticsCalculationUtils.calculatePercentage(
        successfulContacts, totalTasks
    );
    BigDecimal unreachableRatePct = AnalyticsCalculationUtils.calculatePercentage(
        unreachableCount, totalTasks
    );
    BigDecimal taskCompletionRatePct = AnalyticsCalculationUtils.calculatePercentage(
        completedTasks, totalTasks
    );

    List<FollowupWavePerformanceResponse> wavePerformance = new ArrayList<>();
    if (waves != null) {
      for (FollowupWaveProjection w : waves) {
        long tCount = w.taskCount() != null ? w.taskCount() : 0L;
        long sCount = w.successCount() != null ? w.successCount() : 0L;
        long nrCount = w.noResponseCount() != null ? w.noResponseCount() : 0L;
        long uCount = w.unreachableCount() != null ? w.unreachableCount() : 0L;
        long pCount = Math.max(0L, tCount - (sCount + nrCount + uCount));

        BigDecimal unreachablePct = AnalyticsCalculationUtils.calculatePercentage(uCount, tCount);
        BigDecimal sharePct = AnalyticsCalculationUtils.calculatePercentage(tCount, totalTasks);

        wavePerformance.add(new FollowupWavePerformanceResponse(
            w.followupTypeCode(),
            w.typeName(),
            w.followupOffsetMonths(),
            tCount,
            w.traineeCount(),
            sCount,
            nrCount,
            uCount,
            pCount,
            w.successRatePct(),
            unreachablePct,
            sharePct
        ));
      }
    }

    FollowupSlaResponse slaPerformance = new FollowupSlaResponse(
        false,
        SLA_UNCONFIGURED_MESSAGE,
        null,
        null,
        null,
        null
    );

    return new FollowupDashboardResponse(
        totalTasks,
        totalTrainees,
        completedTasks,
        pendingTasks,
        successfulContacts,
        noResponseCount,
        unreachableCount,
        contactSuccessRatePct,
        unreachableRatePct,
        taskCompletionRatePct,
        wavePerformance,
        slaPerformance
    );
  }

  @Override
  public SurveyDashboardResponse getSurveyDashboard() {
    List<SurveyResponsePipelineProjection> pipeline = repository.getFormalQ20SurveyResponsePipeline();
    SurveyOverallSummaryProjection summary = repository.getFormalQ20OverallSummary();

    Long totalResponseRows;
    Long totalTrainees;
    Long submittedCount;
    Long pendingCount;

    if (summary != null) {
      totalResponseRows = summary.totalResponseRows() != null ? summary.totalResponseRows() : 0L;
      totalTrainees = summary.totalTrainees() != null ? summary.totalTrainees() : 0L;
      submittedCount = summary.submittedCount() != null ? summary.submittedCount() : 0L;
      pendingCount = summary.pendingCount() != null ? summary.pendingCount() : 0L;
    } else {
      totalTrainees = null; // Distinct count cannot be safely summed across purposes
      if (pipeline != null && !pipeline.isEmpty()) {
        long tRows = 0L;
        long sCount = 0L;
        for (SurveyResponsePipelineProjection p : pipeline) {
          if (p.responseRowCount() != null) {
            tRows += p.responseRowCount();
          }
          if (p.submittedCount() != null) {
            sCount += p.submittedCount();
          }
        }
        totalResponseRows = tRows;
        submittedCount = sCount;
        pendingCount = Math.max(0L, tRows - sCount);
      } else {
        totalResponseRows = 0L;
        submittedCount = 0L;
        pendingCount = 0L;
      }
    }

    BigDecimal overallSubmissionRatePct = AnalyticsCalculationUtils.calculatePercentage(
        submittedCount, totalResponseRows
    );

    List<SurveyPurposePipelineResponse> purposePipeline = new ArrayList<>();
    if (pipeline != null) {
      for (SurveyResponsePipelineProjection p : pipeline) {
        long rRows = p.responseRowCount() != null ? p.responseRowCount() : 0L;
        long sCount = p.submittedCount() != null ? p.submittedCount() : 0L;
        long pCount = Math.max(0L, rRows - sCount);
        BigDecimal sharePct = AnalyticsCalculationUtils.calculatePercentage(rRows, totalResponseRows);

        purposePipeline.add(new SurveyPurposePipelineResponse(
            p.surveyPurposeCode(),
            p.purposeName(),
            p.followupOffsetMonths(),
            rRows,
            p.traineeCount(),
            sCount,
            pCount,
            p.submissionRatePct(),
            sharePct
        ));
      }
    }

    SurveySatisfactionResponse satisfactionPerformance = new SurveySatisfactionResponse(
        false,
        SATISFACTION_UNCONFIGURED_MESSAGE,
        null,
        null
    );

    return new SurveyDashboardResponse(
        totalResponseRows,
        totalTrainees,
        submittedCount,
        pendingCount,
        overallSubmissionRatePct,
        purposePipeline,
        satisfactionPerformance
    );
  }

  private static class MethodAccumulator {
    final String methodCode;
    final String methodName;
    final Boolean isOfficial;
    final Boolean isTraineeSelfReported;
    long currentVerificationCount = 0L;
    long traineeCount = 0L;
    long verifiedCount = 0L;
    long pendingCount = 0L;
    long rejectedCount = 0L;

    MethodAccumulator(String methodCode, String methodName, Boolean isOfficial, Boolean isTraineeSelfReported) {
      this.methodCode = methodCode;
      this.methodName = methodName;
      this.isOfficial = isOfficial;
      this.isTraineeSelfReported = isTraineeSelfReported;
    }
  }
}
