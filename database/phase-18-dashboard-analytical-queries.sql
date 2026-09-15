-- Phase 18 — Dashboard analytical queries
-- Dependencies: Phases 00–16 (Phase 17 skipped; no demo data)
-- MySQL 8.0+
-- Read-only SELECT statements against Phase 16 views.
-- Empty transactional tables yield empty rows or NULL aggregates; that is expected.
-- No names, phones, emails, addresses, Aadhaar, passwords, or tokens.

USE SIH26135;

-- ===========================================================================
-- Overall KPI queries
-- ===========================================================================

-- Q01 Dashboard KPI strip: trainee, enrollment, completion, certification, placement, employment
SELECT
  (SELECT COUNT(*) FROM vw_trainee_outcome_summary) AS trainee_count,
  (SELECT COUNT(*) FROM vw_enrollment_outcome_fact) AS enrollment_count,
  (SELECT COUNT(DISTINCT trainee_id) FROM vw_enrollment_outcome_fact) AS enrolled_trainee_count,
  (SELECT SUM(is_completed_flag) FROM vw_enrollment_outcome_fact) AS completed_enrollment_count,
  (SELECT SUM(has_issued_certificate_flag) FROM vw_enrollment_outcome_fact) AS certified_enrollment_count,
  (SELECT COUNT(*) FROM vw_placement_fact) AS placement_count,
  (SELECT SUM(joined_flag) FROM vw_placement_fact) AS joined_placement_count,
  (SELECT COUNT(*) FROM vw_employment_fact) AS employment_spell_count,
  (SELECT COUNT(DISTINCT trainee_id) FROM vw_employment_fact) AS employed_trainee_count,
  (SELECT SUM(CASE WHEN is_current = 1 THEN 1 ELSE 0 END) FROM vw_employment_fact) AS current_employment_spell_count,
  (SELECT SUM(CASE WHEN is_current = 1 AND is_self_employment = 1 THEN 1 ELSE 0 END) FROM vw_employment_fact) AS current_self_employment_count,
  (SELECT SUM(CASE WHEN is_current = 1 AND is_apprenticeship = 1 THEN 1 ELSE 0 END) FROM vw_employment_fact) AS current_apprenticeship_count;

-- ===========================================================================
-- Training pipeline
-- ===========================================================================

-- Q02 Pipeline rates from enrollment grain (NULL rate when no enrollments)
SELECT
  COUNT(*) AS enrollment_count,
  SUM(is_completed_flag) AS completed_count,
  SUM(has_issued_certificate_flag) AS certified_count,
  SUM(has_joined_placement_flag) AS joined_placement_count,
  SUM(has_employment_flag) AS employed_count,
  ROUND(SUM(is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completion_rate_pct,
  ROUND(SUM(has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certification_rate_pct,
  ROUND(SUM(has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placement_rate_pct,
  ROUND(SUM(has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employment_rate_pct
FROM vw_enrollment_outcome_fact;

-- ===========================================================================
-- Program analytics
-- ===========================================================================

-- Q03 Program performance with scheme codes (no trainee PII)
SELECT
  p.program_id,
  p.program_code,
  p.program_name,
  s.scheme_id,
  s.scheme_code,
  s.scheme_name,
  v.enrollment_count,
  v.trainee_count,
  v.completed_count,
  v.certified_count,
  v.joined_placement_count,
  v.employed_count,
  v.self_employment_spell_count,
  v.apprenticeship_spell_count,
  v.completion_rate_pct,
  v.certification_rate_pct,
  v.placement_rate_pct,
  v.employment_rate_pct
FROM vw_program_outcome_summary v
JOIN programs p ON p.program_id = v.program_id
JOIN schemes s ON s.scheme_id = v.scheme_id
ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC;

-- ===========================================================================
-- Provider analytics
-- ===========================================================================

-- Q04 Training provider leaderboard
SELECT
  tp.provider_id,
  tp.provider_code,
  tp.provider_name,
  v.enrollment_count,
  v.trainee_count,
  v.completed_count,
  v.certified_count,
  v.joined_placement_count,
  v.employed_count,
  v.completion_rate_pct,
  v.certification_rate_pct,
  v.placement_rate_pct,
  v.employment_rate_pct
FROM vw_provider_outcome_summary v
JOIN training_providers tp ON tp.provider_id = v.provider_id
ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC;

-- ===========================================================================
-- Course analytics
-- ===========================================================================

-- Q05 Course outcome table
SELECT
  c.course_id,
  c.course_code,
  c.course_name,
  v.enrollment_count,
  v.trainee_count,
  v.completed_count,
  v.certified_count,
  v.joined_placement_count,
  v.employed_count,
  v.completion_rate_pct,
  v.certification_rate_pct,
  v.placement_rate_pct,
  v.employment_rate_pct
FROM vw_course_outcome_summary v
JOIN courses c ON c.course_id = v.course_id
ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct DESC, v.enrollment_count DESC;

-- ===========================================================================
-- District analytics
-- ===========================================================================

-- Q06 District outcome map/table
SELECT
  d.district_id,
  d.district_code,
  d.district_name,
  st.state_id,
  st.state_code,
  st.state_name,
  v.enrollment_count,
  v.trainee_count,
  v.completed_count,
  v.certified_count,
  v.joined_placement_count,
  v.employed_count,
  v.completion_rate_pct,
  v.certification_rate_pct,
  v.placement_rate_pct,
  v.employment_rate_pct
FROM vw_district_outcome_summary v
JOIN districts d ON d.district_id = v.district_id
JOIN states st ON st.state_id = d.state_id
ORDER BY v.employment_rate_pct IS NULL, v.employment_rate_pct ASC, v.enrollment_count DESC;

-- ===========================================================================
-- Employer analytics
-- ===========================================================================

-- Q07 Employer hiring dashboard
SELECT
  e.employer_id,
  e.employer_code,
  e.employer_name,
  v.placement_count,
  v.trainee_count,
  v.joined_count,
  v.join_rate_pct,
  v.avg_joining_salary
FROM vw_employer_hiring_summary v
JOIN employers e ON e.employer_id = v.employer_id
ORDER BY v.joined_count DESC, v.placement_count DESC;

-- ===========================================================================
-- Employment analytics
-- ===========================================================================

-- Q08 Employment mix: current vs historical, wage vs self vs apprenticeship
SELECT
  COUNT(*) AS employment_spell_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(CASE WHEN is_current = 1 THEN 1 ELSE 0 END) AS current_spell_count,
  SUM(CASE WHEN is_wage_employment = 1 THEN 1 ELSE 0 END) AS wage_spell_count,
  SUM(CASE WHEN is_self_employment = 1 THEN 1 ELSE 0 END) AS self_employment_spell_count,
  SUM(CASE WHEN is_apprenticeship = 1 THEN 1 ELSE 0 END) AS apprenticeship_spell_count,
  SUM(CASE WHEN is_internship = 1 THEN 1 ELSE 0 END) AS internship_spell_count,
  SUM(CASE WHEN is_current = 1 AND is_wage_employment = 1 THEN 1 ELSE 0 END) AS current_wage_count,
  SUM(CASE WHEN is_current = 1 AND is_self_employment = 1 THEN 1 ELSE 0 END) AS current_self_employment_count,
  AVG(duration_days) AS avg_duration_days,
  AVG(CASE WHEN is_current = 1 THEN duration_days END) AS avg_current_spell_duration_days
FROM vw_employment_fact;

-- Q09 Engagement type breakdown
SELECT
  engagement_type_code,
  is_wage_employment,
  is_self_employment,
  is_apprenticeship,
  is_internship,
  COUNT(*) AS spell_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_current) AS current_spell_count,
  AVG(duration_days) AS avg_duration_days
FROM vw_employment_fact
GROUP BY
  engagement_type_code,
  is_wage_employment,
  is_self_employment,
  is_apprenticeship,
  is_internship
ORDER BY spell_count DESC;

-- ===========================================================================
-- Salary analytics
-- ===========================================================================

-- Q10 Salary progression KPI (change_pct already NULL-safe in the view)
SELECT
  COUNT(*) AS employment_with_salary_row_count,
  COUNT(first_recorded_salary) AS first_salary_count,
  COUNT(latest_recorded_salary) AS latest_salary_count,
  AVG(first_recorded_salary) AS avg_first_recorded_salary,
  AVG(latest_recorded_salary) AS avg_latest_recorded_salary,
  AVG(salary_change_amount) AS avg_salary_change_amount,
  AVG(salary_change_pct) AS avg_salary_change_pct
FROM vw_salary_progression_fact;

-- Q11 Salary progression by course (identifiers only)
SELECT
  sp.course_id,
  c.course_code,
  c.course_name,
  COUNT(*) AS spell_count,
  AVG(sp.first_recorded_salary) AS avg_first_recorded_salary,
  AVG(sp.latest_recorded_salary) AS avg_latest_recorded_salary,
  AVG(sp.salary_change_pct) AS avg_salary_change_pct
FROM vw_salary_progression_fact sp
LEFT JOIN courses c ON c.course_id = sp.course_id
WHERE sp.course_id IS NOT NULL
GROUP BY sp.course_id, c.course_code, c.course_name
ORDER BY avg_salary_change_pct IS NULL, avg_salary_change_pct DESC;

-- ===========================================================================
-- Retention analytics
-- ===========================================================================

-- Q12 Retention rates; NULL flags (milestone not reached) are excluded from COUNT
SELECT
  COUNT(*) AS employment_spell_count,
  COUNT(retained_6m_flag) AS observable_6m_count,
  SUM(retained_6m_flag) AS retained_6m_count,
  ROUND(SUM(retained_6m_flag) / NULLIF(COUNT(retained_6m_flag), 0) * 100, 2) AS retention_6m_pct,
  COUNT(retained_12m_flag) AS observable_12m_count,
  SUM(retained_12m_flag) AS retained_12m_count,
  ROUND(SUM(retained_12m_flag) / NULLIF(COUNT(retained_12m_flag), 0) * 100, 2) AS retention_12m_pct,
  COUNT(retained_24m_flag) AS observable_24m_count,
  SUM(retained_24m_flag) AS retained_24m_count,
  ROUND(SUM(retained_24m_flag) / NULLIF(COUNT(retained_24m_flag), 0) * 100, 2) AS retention_24m_pct,
  COUNT(retained_36m_flag) AS observable_36m_count,
  SUM(retained_36m_flag) AS retained_36m_count,
  ROUND(SUM(retained_36m_flag) / NULLIF(COUNT(retained_36m_flag), 0) * 100, 2) AS retention_36m_pct
FROM vw_employment_retention_fact;

-- ===========================================================================
-- Skill-gap analytics
-- ===========================================================================

-- Q13 Top current skill gaps
SELECT
  skill_id,
  skill_code,
  skill_gap_severity_id,
  severity_code,
  gap_count,
  trainee_count,
  current_gap_count,
  avg_gap_level_delta
FROM vw_skill_gap_summary
ORDER BY current_gap_count DESC, gap_count DESC
LIMIT 20;

-- Q14 Current skill-gap severity mix and recommendation uptake
SELECT
  severity_code,
  COUNT(*) AS current_gap_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(recommendation_count) AS recommendation_count,
  SUM(accepted_recommendation_count) AS accepted_recommendation_count
FROM vw_skill_gap_fact
WHERE is_current = 1
GROUP BY severity_code
ORDER BY current_gap_count DESC;

-- ===========================================================================
-- Unemployment analytics
-- ===========================================================================

-- Q15 Unemployment period KPI
SELECT
  COUNT(*) AS unemployment_period_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_current) AS current_unemployment_count,
  SUM(reemployed_flag) AS reemployed_count,
  SUM(never_preceded_by_employment_flag) AS never_preceded_by_employment_count,
  AVG(duration_days) AS avg_duration_days,
  AVG(CASE WHEN is_current = 1 THEN duration_days END) AS avg_open_period_duration_days
FROM vw_unemployment_fact;

-- Q16 Unemployment reasons
SELECT
  unemployment_reason_id,
  unemployment_reason_code,
  period_count,
  trainee_count,
  current_period_count,
  reemployed_count,
  avg_duration_days
FROM vw_unemployment_reason_summary
ORDER BY period_count DESC;

-- ===========================================================================
-- Attrition analytics
-- ===========================================================================

-- Q17 Exit reasons and voluntary vs involuntary mix
SELECT
  employment_exit_reason_id,
  exit_reason_code,
  separation_nature_code,
  is_voluntary_flag,
  is_involuntary_flag,
  exit_count,
  trainee_count
FROM vw_attrition_reason_summary
ORDER BY exit_count DESC;

-- Q18 Voluntary vs involuntary totals
SELECT
  SUM(CASE WHEN is_voluntary_flag = 1 THEN exit_count ELSE 0 END) AS voluntary_exit_count,
  SUM(CASE WHEN is_involuntary_flag = 1 THEN exit_count ELSE 0 END) AS involuntary_exit_count,
  SUM(CASE WHEN is_voluntary_flag = 0 AND is_involuntary_flag = 0 THEN exit_count ELSE 0 END) AS other_exit_count,
  SUM(exit_count) AS total_exit_count
FROM vw_attrition_reason_summary;

-- ===========================================================================
-- Follow-up and survey analytics
-- ===========================================================================

-- Q19 Follow-up success by wave
SELECT
  followup_type_code,
  followup_offset_months,
  task_count,
  trainee_count,
  success_count,
  no_response_count,
  unreachable_count,
  success_rate_pct
FROM vw_followup_outcome_summary
ORDER BY followup_offset_months IS NULL, followup_offset_months, followup_type_code;

-- Q20 Survey response pipeline
SELECT
  survey_purpose_code,
  followup_offset_months,
  COUNT(*) AS response_row_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_submitted_flag) AS submitted_count,
  ROUND(SUM(is_submitted_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS submission_rate_pct
FROM vw_survey_response_fact
GROUP BY survey_purpose_code, followup_offset_months
ORDER BY followup_offset_months IS NULL, followup_offset_months, survey_purpose_code;

-- ===========================================================================
-- Employment verification analytics
-- ===========================================================================

-- Q21 Verification outcomes (all cycles)
SELECT
  record_verification_status_code,
  is_verified_flag,
  COUNT(*) AS verification_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  COUNT(DISTINCT employment_id) AS employment_count
FROM vw_employment_verification_fact
GROUP BY record_verification_status_code, is_verified_flag
ORDER BY verification_count DESC;

-- Q22 Current/latest verification per employment spell, by method
SELECT
  verification_method_code,
  is_trainee_self_reported,
  is_official_flag,
  record_verification_status_code,
  COUNT(*) AS current_verification_count,
  COUNT(DISTINCT trainee_id) AS trainee_count
FROM vw_employment_verification_fact
WHERE is_current = 1
GROUP BY
  verification_method_code,
  is_trainee_self_reported,
  is_official_flag,
  record_verification_status_code
ORDER BY current_verification_count DESC;
