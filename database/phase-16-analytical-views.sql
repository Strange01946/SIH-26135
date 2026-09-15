-- Phase 16 — Analytical views
-- Dependencies: Phases 00–15
-- MySQL 8.0+
-- Read-only projections. No BASE TABLES. No PII (names, phone, email, address).
-- CREATE OR REPLACE VIEW may implicitly commit. Do not wrap this file in a transaction.
-- Soft-deleted transactional rows (deleted_at IS NOT NULL) are excluded from analytics.

USE SIH26135;

-- ---------------------------------------------------------------------------
-- Enrollment grain: one row per training_enrollments row
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_enrollment_outcome_fact AS
SELECT
  e.enrollment_id,
  e.enrollment_number,
  e.trainee_id,
  e.program_id,
  p.scheme_id,
  e.course_id,
  e.provider_id,
  e.center_id,
  e.batch_id,
  e.enrollment_date,
  e.start_date,
  e.actual_completion_date,
  e.enrollment_status_id,
  st.status_code AS enrollment_status_code,
  st.is_completed_flag,
  t.state_id AS trainee_state_id,
  t.district_id AS trainee_district_id,
  COALESCE(cert.issued_certificate_count, 0) AS issued_certificate_count,
  CASE WHEN COALESCE(cert.issued_certificate_count, 0) > 0 THEN 1 ELSE 0 END AS has_issued_certificate_flag,
  COALESCE(pl.placement_count, 0) AS placement_count,
  COALESCE(pl.joined_placement_count, 0) AS joined_placement_count,
  CASE WHEN COALESCE(pl.joined_placement_count, 0) > 0 THEN 1 ELSE 0 END AS has_joined_placement_flag,
  COALESCE(emp.employment_count, 0) AS employment_count,
  COALESCE(emp.current_employment_count, 0) AS current_employment_count,
  COALESCE(emp.wage_employment_count, 0) AS wage_employment_count,
  COALESCE(emp.self_employment_count, 0) AS self_employment_count,
  COALESCE(emp.apprenticeship_count, 0) AS apprenticeship_count,
  CASE WHEN COALESCE(emp.employment_count, 0) > 0 THEN 1 ELSE 0 END AS has_employment_flag
FROM training_enrollments e
JOIN ref_enrollment_status st
  ON st.enrollment_status_id = e.enrollment_status_id
JOIN trainees t
  ON t.trainee_id = e.trainee_id
JOIN programs p
  ON p.program_id = e.program_id
LEFT JOIN (
  SELECT
    c.enrollment_id,
    COUNT(*) AS issued_certificate_count
  FROM certifications c
  JOIN ref_certificate_status cs
    ON cs.certificate_status_id = c.certificate_status_id
  WHERE cs.status_code = 'ISSUED'
    AND c.deleted_at IS NULL
  GROUP BY c.enrollment_id
) cert ON cert.enrollment_id = e.enrollment_id
LEFT JOIN (
  SELECT
    pr.enrollment_id,
    COUNT(*) AS placement_count,
    SUM(CASE WHEN js.is_joined_flag = 1 OR ps.is_joined_flag = 1 THEN 1 ELSE 0 END) AS joined_placement_count
  FROM placement_records pr
  JOIN ref_joining_status js
    ON js.joining_status_id = pr.joining_status_id
  JOIN ref_placement_status ps
    ON ps.placement_status_id = pr.placement_status_id
  WHERE pr.deleted_at IS NULL
  GROUP BY pr.enrollment_id
) pl ON pl.enrollment_id = e.enrollment_id
LEFT JOIN (
  SELECT
    er.enrollment_id,
    COUNT(*) AS employment_count,
    SUM(CASE WHEN er.is_current = 1 THEN 1 ELSE 0 END) AS current_employment_count,
    SUM(CASE WHEN et.is_wage_employment = 1 THEN 1 ELSE 0 END) AS wage_employment_count,
    SUM(CASE WHEN et.is_self_employment = 1 THEN 1 ELSE 0 END) AS self_employment_count,
    SUM(CASE WHEN et.is_apprenticeship = 1 THEN 1 ELSE 0 END) AS apprenticeship_count
  FROM employment_records er
  JOIN ref_engagement_type et
    ON et.engagement_type_id = er.engagement_type_id
  WHERE er.deleted_at IS NULL
    AND er.enrollment_id IS NOT NULL
  GROUP BY er.enrollment_id
) emp ON emp.enrollment_id = e.enrollment_id
WHERE e.deleted_at IS NULL
  AND t.deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Placement grain: one row per placement_records row
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_placement_fact AS
SELECT
  pr.placement_id,
  pr.placement_number,
  pr.trainee_id,
  pr.enrollment_id,
  pr.course_id,
  pr.program_id,
  pr.provider_id,
  pr.employer_id,
  pr.job_role_id,
  pr.engagement_type_id,
  et.type_code AS engagement_type_code,
  et.is_wage_employment,
  et.is_self_employment,
  et.is_apprenticeship,
  et.is_internship,
  pr.placement_source_id,
  psrc.source_code AS placement_source_code,
  pr.placement_status_id,
  ps.status_code AS placement_status_code,
  ps.is_joined_flag AS placement_status_joined_flag,
  ps.is_unsuccessful_flag,
  pr.joining_status_id,
  js.status_code AS joining_status_code,
  js.is_joined_flag,
  CASE WHEN js.is_joined_flag = 1 OR ps.is_joined_flag = 1 THEN 1 ELSE 0 END AS joined_flag,
  pr.offered_salary,
  pr.joining_salary,
  pr.salary_frequency_id,
  pr.currency_code,
  pr.offer_date,
  pr.actual_joining_date,
  pr.work_state_id,
  pr.work_district_id,
  pr.record_verification_status_id,
  rvs.status_code AS record_verification_status_code,
  rvs.is_verified_flag,
  t.state_id AS trainee_state_id,
  t.district_id AS trainee_district_id
FROM placement_records pr
JOIN ref_engagement_type et
  ON et.engagement_type_id = pr.engagement_type_id
JOIN ref_placement_source psrc
  ON psrc.placement_source_id = pr.placement_source_id
JOIN ref_placement_status ps
  ON ps.placement_status_id = pr.placement_status_id
JOIN ref_joining_status js
  ON js.joining_status_id = pr.joining_status_id
JOIN ref_record_verification_status rvs
  ON rvs.record_verification_status_id = pr.record_verification_status_id
JOIN trainees t
  ON t.trainee_id = pr.trainee_id
WHERE pr.deleted_at IS NULL
  AND t.deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Employment spell grain: all historical spells
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_employment_fact AS
SELECT
  er.employment_id,
  er.employment_number,
  er.trainee_id,
  er.enrollment_id,
  er.placement_id,
  er.employer_id,
  er.job_role_id,
  er.engagement_type_id,
  et.type_code AS engagement_type_code,
  et.is_wage_employment,
  et.is_self_employment,
  et.is_apprenticeship,
  et.is_internship,
  er.employment_spell_status_id,
  ess.status_code AS employment_spell_status_code,
  ess.is_active_flag,
  er.start_date,
  er.end_date,
  er.is_current,
  DATEDIFF(IFNULL(er.end_date, CURDATE()), er.start_date) AS duration_days,
  er.starting_salary,
  er.salary_frequency_id,
  sf.frequency_code AS salary_frequency_code,
  er.currency_code,
  er.work_state_id,
  er.work_district_id,
  er.employment_info_source_id,
  src.source_code AS employment_info_source_code,
  src.is_self_reported_flag,
  er.record_verification_status_id,
  rvs.status_code AS record_verification_status_code,
  rvs.is_verified_flag,
  er.employment_exit_reason_id,
  t.state_id AS trainee_state_id,
  t.district_id AS trainee_district_id
FROM employment_records er
JOIN ref_engagement_type et
  ON et.engagement_type_id = er.engagement_type_id
JOIN ref_employment_spell_status ess
  ON ess.employment_spell_status_id = er.employment_spell_status_id
JOIN ref_employment_info_source src
  ON src.employment_info_source_id = er.employment_info_source_id
JOIN ref_record_verification_status rvs
  ON rvs.record_verification_status_id = er.record_verification_status_id
JOIN trainees t
  ON t.trainee_id = er.trainee_id
LEFT JOIN ref_salary_frequency sf
  ON sf.salary_frequency_id = er.salary_frequency_id
WHERE er.deleted_at IS NULL
  AND t.deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Salary: first vs latest recorded amount per employment spell
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_salary_progression_fact AS
SELECT
  e.employment_id,
  e.trainee_id,
  e.enrollment_id,
  en.course_id,
  en.provider_id,
  e.engagement_type_id,
  e.currency_code,
  e.starting_salary,
  first_sh.salary_amount AS first_recorded_salary,
  first_sh.effective_from AS first_salary_effective_from,
  first_sh.observation_month_offset AS first_observation_month_offset,
  latest_sh.salary_amount AS latest_recorded_salary,
  latest_sh.effective_from AS latest_salary_effective_from,
  latest_sh.effective_to AS latest_salary_effective_to,
  latest_sh.observation_month_offset AS latest_observation_month_offset,
  latest_sf.frequency_code AS latest_salary_frequency_code,
  CASE
    WHEN first_sh.salary_amount IS NULL OR latest_sh.salary_amount IS NULL THEN NULL
    ELSE latest_sh.salary_amount - first_sh.salary_amount
  END AS salary_change_amount,
  CASE
    WHEN first_sh.salary_amount IS NULL OR first_sh.salary_amount = 0 OR latest_sh.salary_amount IS NULL THEN NULL
    ELSE ROUND((latest_sh.salary_amount - first_sh.salary_amount) / first_sh.salary_amount * 100, 2)
  END AS salary_change_pct
FROM employment_records e
LEFT JOIN (
  SELECT
    sh.employment_id,
    sh.salary_amount,
    sh.effective_from,
    sh.observation_month_offset,
    ROW_NUMBER() OVER (
      PARTITION BY sh.employment_id
      ORDER BY sh.effective_from ASC, sh.salary_history_id ASC
    ) AS rn_first
  FROM salary_history sh
) first_sh
  ON first_sh.employment_id = e.employment_id
 AND first_sh.rn_first = 1
LEFT JOIN (
  SELECT
    sh.employment_id,
    sh.salary_amount,
    sh.effective_from,
    sh.effective_to,
    sh.observation_month_offset,
    sh.salary_frequency_id,
    ROW_NUMBER() OVER (
      PARTITION BY sh.employment_id
      ORDER BY (sh.effective_to IS NULL) DESC, sh.effective_from DESC, sh.salary_history_id DESC
    ) AS rn_latest
  FROM salary_history sh
) latest_sh
  ON latest_sh.employment_id = e.employment_id
 AND latest_sh.rn_latest = 1
LEFT JOIN ref_salary_frequency latest_sf
  ON latest_sf.salary_frequency_id = latest_sh.salary_frequency_id
LEFT JOIN training_enrollments en
  ON en.enrollment_id = e.enrollment_id
WHERE e.deleted_at IS NULL;

-- Observable retention: NULL until the milestone date has been reached
CREATE OR REPLACE VIEW vw_employment_retention_fact AS
SELECT
  employment_id,
  trainee_id,
  enrollment_id,
  engagement_type_id,
  start_date,
  end_date,
  is_current,
  CASE
    WHEN start_date > DATE_SUB(CURDATE(), INTERVAL 6 MONTH) THEN NULL
    WHEN end_date IS NULL OR end_date >= DATE_ADD(start_date, INTERVAL 6 MONTH) THEN 1
    ELSE 0
  END AS retained_6m_flag,
  CASE
    WHEN start_date > DATE_SUB(CURDATE(), INTERVAL 12 MONTH) THEN NULL
    WHEN end_date IS NULL OR end_date >= DATE_ADD(start_date, INTERVAL 12 MONTH) THEN 1
    ELSE 0
  END AS retained_12m_flag,
  CASE
    WHEN start_date > DATE_SUB(CURDATE(), INTERVAL 24 MONTH) THEN NULL
    WHEN end_date IS NULL OR end_date >= DATE_ADD(start_date, INTERVAL 24 MONTH) THEN 1
    ELSE 0
  END AS retained_24m_flag,
  CASE
    WHEN start_date > DATE_SUB(CURDATE(), INTERVAL 36 MONTH) THEN NULL
    WHEN end_date IS NULL OR end_date >= DATE_ADD(start_date, INTERVAL 36 MONTH) THEN 1
    ELSE 0
  END AS retained_36m_flag
FROM vw_employment_fact;

-- ---------------------------------------------------------------------------
-- Skill-gap grain
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_skill_gap_fact AS
SELECT
  g.skill_gap_id,
  g.skill_gap_number,
  g.skill_gap_assessment_id,
  g.trainee_id,
  g.skill_id,
  s.skill_code,
  a.enrollment_id,
  a.course_id,
  a.job_role_id,
  a.employment_id,
  a.placement_id,
  g.observed_skill_level_id,
  g.required_skill_level_id,
  g.gap_level_delta,
  g.skill_gap_severity_id,
  sev.severity_code,
  g.skill_gap_status_id,
  st.status_code AS skill_gap_status_code,
  g.skill_gap_source_id,
  src.source_code AS skill_gap_source_code,
  g.is_current,
  g.identified_on,
  g.resolved_on,
  t.state_id AS trainee_state_id,
  t.district_id AS trainee_district_id,
  COALESCE(rec.recommendation_count, 0) AS recommendation_count,
  COALESCE(rec.accepted_recommendation_count, 0) AS accepted_recommendation_count
FROM skill_gaps g
JOIN skills s
  ON s.skill_id = g.skill_id
JOIN skill_gap_assessments a
  ON a.skill_gap_assessment_id = g.skill_gap_assessment_id
JOIN ref_skill_gap_severity sev
  ON sev.skill_gap_severity_id = g.skill_gap_severity_id
JOIN ref_skill_gap_status st
  ON st.skill_gap_status_id = g.skill_gap_status_id
JOIN ref_skill_gap_source src
  ON src.skill_gap_source_id = g.skill_gap_source_id
JOIN trainees t
  ON t.trainee_id = g.trainee_id
LEFT JOIN (
  SELECT
    skill_gap_id,
    COUNT(*) AS recommendation_count,
    SUM(CASE WHEN is_accepted_flag = 1 THEN 1 ELSE 0 END) AS accepted_recommendation_count
  FROM skill_gap_recommendations
  GROUP BY skill_gap_id
) rec ON rec.skill_gap_id = g.skill_gap_id
WHERE t.deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Unemployment and attrition grains
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_unemployment_fact AS
SELECT
  u.unemployment_event_id,
  u.trainee_id,
  u.period_number,
  u.start_date,
  u.end_date,
  u.is_current,
  DATEDIFF(IFNULL(u.end_date, CURDATE()), u.start_date) AS duration_days,
  u.labour_status_id,
  ls.status_code AS labour_status_code,
  u.unemployment_reason_id,
  ur.reason_code AS unemployment_reason_code,
  u.preceding_employment_id,
  u.employment_exit_event_id,
  u.succeeding_employment_id,
  CASE WHEN u.succeeding_employment_id IS NOT NULL THEN 1 ELSE 0 END AS reemployed_flag,
  CASE
    WHEN u.preceding_employment_id IS NULL THEN 1
    ELSE 0
  END AS never_preceded_by_employment_flag,
  u.enrollment_id,
  u.placement_id,
  u.followup_task_id,
  u.survey_response_id,
  u.employment_info_source_id,
  src.source_code AS info_source_code,
  src.is_self_reported_flag,
  u.record_verification_status_id,
  rvs.status_code AS record_verification_status_code,
  rvs.is_verified_flag,
  t.state_id AS trainee_state_id,
  t.district_id AS trainee_district_id
FROM trainee_unemployment_events u
JOIN ref_employment_status ls
  ON ls.employment_status_id = u.labour_status_id
JOIN ref_unemployment_reason ur
  ON ur.unemployment_reason_id = u.unemployment_reason_id
JOIN ref_employment_info_source src
  ON src.employment_info_source_id = u.employment_info_source_id
JOIN ref_record_verification_status rvs
  ON rvs.record_verification_status_id = u.record_verification_status_id
JOIN trainees t
  ON t.trainee_id = u.trainee_id
WHERE t.deleted_at IS NULL;

CREATE OR REPLACE VIEW vw_employment_exit_fact AS
SELECT
  x.employment_exit_event_id,
  x.employment_id,
  x.trainee_id,
  x.enrollment_id,
  x.separation_date,
  x.employment_exit_reason_id,
  xr.reason_code AS exit_reason_code,
  x.separation_nature_id,
  sn.nature_code AS separation_nature_code,
  sn.is_voluntary_flag,
  sn.is_involuntary_flag,
  x.employment_info_source_id,
  src.source_code AS info_source_code,
  x.record_verification_status_id,
  rvs.status_code AS record_verification_status_code,
  t.state_id AS trainee_state_id,
  t.district_id AS trainee_district_id
FROM employment_exit_events x
JOIN ref_employment_exit_reason xr
  ON xr.employment_exit_reason_id = x.employment_exit_reason_id
JOIN ref_separation_nature sn
  ON sn.separation_nature_id = x.separation_nature_id
JOIN ref_employment_info_source src
  ON src.employment_info_source_id = x.employment_info_source_id
JOIN ref_record_verification_status rvs
  ON rvs.record_verification_status_id = x.record_verification_status_id
JOIN trainees t
  ON t.trainee_id = x.trainee_id
WHERE t.deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Follow-up and survey grains
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_followup_fact AS
SELECT
  ft.followup_task_id,
  ft.followup_campaign_id,
  fc.followup_type_id,
  fty.type_code AS followup_type_code,
  fty.followup_offset_months,
  ft.trainee_id,
  ft.enrollment_id,
  ft.placement_id,
  ft.employment_id,
  ft.survey_id,
  ft.scheduled_date,
  ft.next_followup_date,
  ft.followup_status_id,
  fs.status_code AS followup_status_code,
  fs.is_open_flag,
  ft.followup_outcome_id,
  fo.outcome_code AS followup_outcome_code,
  fo.is_success_flag,
  fo.is_unreachable_flag,
  fo.is_no_response_flag,
  ft.non_response_reason_id,
  ft.last_channel_id,
  ch.channel_code AS last_channel_code,
  t.state_id AS trainee_state_id,
  t.district_id AS trainee_district_id
FROM followup_tasks ft
JOIN followup_campaigns fc
  ON fc.followup_campaign_id = ft.followup_campaign_id
JOIN ref_followup_type fty
  ON fty.followup_type_id = fc.followup_type_id
JOIN ref_followup_status fs
  ON fs.followup_status_id = ft.followup_status_id
JOIN trainees t
  ON t.trainee_id = ft.trainee_id
LEFT JOIN ref_followup_outcome fo
  ON fo.followup_outcome_id = ft.followup_outcome_id
LEFT JOIN ref_communication_channel ch
  ON ch.communication_channel_id = ft.last_channel_id
WHERE t.deleted_at IS NULL
  AND fc.deleted_at IS NULL;

CREATE OR REPLACE VIEW vw_survey_response_fact AS
SELECT
  sr.survey_response_id,
  sr.survey_id,
  s.survey_purpose_id,
  sp.purpose_code AS survey_purpose_code,
  s.program_id,
  s.course_id,
  s.batch_id,
  s.followup_offset_months,
  sr.trainee_id,
  sr.enrollment_id,
  sr.followup_task_id,
  sr.attempt_number,
  sr.survey_response_status_id,
  rs.status_code AS response_status_code,
  rs.is_submitted_flag,
  sr.started_at,
  sr.submitted_at,
  t.state_id AS trainee_state_id,
  t.district_id AS trainee_district_id
FROM survey_responses sr
JOIN surveys s
  ON s.survey_id = sr.survey_id
JOIN ref_survey_purpose sp
  ON sp.survey_purpose_id = s.survey_purpose_id
JOIN ref_survey_response_status rs
  ON rs.survey_response_status_id = sr.survey_response_status_id
JOIN trainees t
  ON t.trainee_id = sr.trainee_id
WHERE t.deleted_at IS NULL
  AND s.deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Employment verification grain (history preserved; is_current marks latest per spell)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_employment_verification_fact AS
SELECT
  v.employment_verification_id,
  v.verification_number,
  v.employment_id,
  v.trainee_id,
  v.placement_id,
  v.employer_id,
  v.cycle_number,
  v.is_reverification,
  v.is_current,
  v.record_verification_status_id,
  rvs.status_code AS record_verification_status_code,
  rvs.is_verified_flag,
  v.employment_verification_method_id,
  m.method_code AS verification_method_code,
  m.is_trainee_self_reported,
  m.is_official_flag,
  v.employment_info_source_id,
  v.requested_at,
  v.verified_at,
  t.state_id AS trainee_state_id,
  t.district_id AS trainee_district_id
FROM employment_verifications v
JOIN ref_record_verification_status rvs
  ON rvs.record_verification_status_id = v.record_verification_status_id
JOIN ref_employment_verification_method m
  ON m.employment_verification_method_id = v.employment_verification_method_id
JOIN trainees t
  ON t.trainee_id = v.trainee_id
WHERE t.deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Trainee grain: counts only (no 1:N fan-out)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_trainee_outcome_summary AS
SELECT
  t.trainee_id,
  t.state_id,
  t.district_id,
  t.current_employment_status_id,
  es.status_code AS snapshot_employment_status_code,
  es.is_employed_flag AS snapshot_is_employed_flag,
  COALESCE(en.enrollment_count, 0) AS enrollment_count,
  COALESCE(en.completed_enrollment_count, 0) AS completed_enrollment_count,
  COALESCE(en.certified_enrollment_count, 0) AS certified_enrollment_count,
  COALESCE(en.placed_enrollment_count, 0) AS placed_enrollment_count,
  COALESCE(en.employed_enrollment_count, 0) AS employed_enrollment_count,
  COALESCE(emp.employment_spell_count, 0) AS employment_spell_count,
  COALESCE(emp.current_employment_count, 0) AS current_employment_count,
  COALESCE(emp.current_wage_count, 0) AS current_wage_employment_count,
  COALESCE(emp.current_self_count, 0) AS current_self_employment_count,
  COALESCE(unemp.unemployment_period_count, 0) AS unemployment_period_count,
  COALESCE(unemp.current_unemployment_count, 0) AS current_unemployment_count,
  COALESCE(gap.current_skill_gap_count, 0) AS current_skill_gap_count,
  CASE
    WHEN COALESCE(en.enrollment_count, 0) = 0 THEN NULL
    ELSE ROUND(en.completed_enrollment_count / en.enrollment_count * 100, 2)
  END AS completion_rate_pct
FROM trainees t
JOIN ref_employment_status es
  ON es.employment_status_id = t.current_employment_status_id
LEFT JOIN (
  SELECT
    trainee_id,
    COUNT(*) AS enrollment_count,
    SUM(is_completed_flag) AS completed_enrollment_count,
    SUM(has_issued_certificate_flag) AS certified_enrollment_count,
    SUM(has_joined_placement_flag) AS placed_enrollment_count,
    SUM(has_employment_flag) AS employed_enrollment_count
  FROM vw_enrollment_outcome_fact
  GROUP BY trainee_id
) en ON en.trainee_id = t.trainee_id
LEFT JOIN (
  SELECT
    trainee_id,
    COUNT(*) AS employment_spell_count,
    SUM(CASE WHEN is_current = 1 THEN 1 ELSE 0 END) AS current_employment_count,
    SUM(CASE WHEN is_current = 1 AND is_wage_employment = 1 THEN 1 ELSE 0 END) AS current_wage_count,
    SUM(CASE WHEN is_current = 1 AND is_self_employment = 1 THEN 1 ELSE 0 END) AS current_self_count
  FROM vw_employment_fact
  GROUP BY trainee_id
) emp ON emp.trainee_id = t.trainee_id
LEFT JOIN (
  SELECT
    trainee_id,
    COUNT(*) AS unemployment_period_count,
    SUM(CASE WHEN is_current = 1 THEN 1 ELSE 0 END) AS current_unemployment_count
  FROM vw_unemployment_fact
  GROUP BY trainee_id
) unemp ON unemp.trainee_id = t.trainee_id
LEFT JOIN (
  SELECT
    trainee_id,
    COUNT(*) AS current_skill_gap_count
  FROM vw_skill_gap_fact
  WHERE is_current = 1
  GROUP BY trainee_id
) gap ON gap.trainee_id = t.trainee_id
WHERE t.deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Aggregate outcome views (rates use NULL when denominator is 0)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE VIEW vw_program_outcome_summary AS
SELECT
  program_id,
  scheme_id,
  COUNT(*) AS enrollment_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_completed_flag) AS completed_count,
  SUM(has_issued_certificate_flag) AS certified_count,
  SUM(has_joined_placement_flag) AS joined_placement_count,
  SUM(has_employment_flag) AS employed_count,
  SUM(self_employment_count) AS self_employment_spell_count,
  SUM(apprenticeship_count) AS apprenticeship_spell_count,
  ROUND(SUM(is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completion_rate_pct,
  ROUND(SUM(has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certification_rate_pct,
  ROUND(SUM(has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placement_rate_pct,
  ROUND(SUM(has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employment_rate_pct
FROM vw_enrollment_outcome_fact
GROUP BY program_id, scheme_id;

CREATE OR REPLACE VIEW vw_provider_outcome_summary AS
SELECT
  provider_id,
  COUNT(*) AS enrollment_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_completed_flag) AS completed_count,
  SUM(has_issued_certificate_flag) AS certified_count,
  SUM(has_joined_placement_flag) AS joined_placement_count,
  SUM(has_employment_flag) AS employed_count,
  ROUND(SUM(is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completion_rate_pct,
  ROUND(SUM(has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certification_rate_pct,
  ROUND(SUM(has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placement_rate_pct,
  ROUND(SUM(has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employment_rate_pct
FROM vw_enrollment_outcome_fact
GROUP BY provider_id;

CREATE OR REPLACE VIEW vw_course_outcome_summary AS
SELECT
  course_id,
  COUNT(*) AS enrollment_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_completed_flag) AS completed_count,
  SUM(has_issued_certificate_flag) AS certified_count,
  SUM(has_joined_placement_flag) AS joined_placement_count,
  SUM(has_employment_flag) AS employed_count,
  ROUND(SUM(is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completion_rate_pct,
  ROUND(SUM(has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certification_rate_pct,
  ROUND(SUM(has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placement_rate_pct,
  ROUND(SUM(has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employment_rate_pct
FROM vw_enrollment_outcome_fact
GROUP BY course_id;

CREATE OR REPLACE VIEW vw_district_outcome_summary AS
SELECT
  trainee_district_id AS district_id,
  COUNT(*) AS enrollment_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_completed_flag) AS completed_count,
  SUM(has_issued_certificate_flag) AS certified_count,
  SUM(has_joined_placement_flag) AS joined_placement_count,
  SUM(has_employment_flag) AS employed_count,
  ROUND(SUM(is_completed_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS completion_rate_pct,
  ROUND(SUM(has_issued_certificate_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS certification_rate_pct,
  ROUND(SUM(has_joined_placement_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS placement_rate_pct,
  ROUND(SUM(has_employment_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS employment_rate_pct
FROM vw_enrollment_outcome_fact
GROUP BY trainee_district_id;

CREATE OR REPLACE VIEW vw_employer_hiring_summary AS
SELECT
  employer_id,
  COUNT(*) AS placement_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(joined_flag) AS joined_count,
  ROUND(SUM(joined_flag) / NULLIF(COUNT(*), 0) * 100, 2) AS join_rate_pct,
  AVG(joining_salary) AS avg_joining_salary
FROM vw_placement_fact
WHERE employer_id IS NOT NULL
GROUP BY employer_id;

CREATE OR REPLACE VIEW vw_unemployment_reason_summary AS
SELECT
  unemployment_reason_id,
  unemployment_reason_code,
  COUNT(*) AS period_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_current) AS current_period_count,
  SUM(reemployed_flag) AS reemployed_count,
  AVG(duration_days) AS avg_duration_days
FROM vw_unemployment_fact
GROUP BY unemployment_reason_id, unemployment_reason_code;

CREATE OR REPLACE VIEW vw_attrition_reason_summary AS
SELECT
  employment_exit_reason_id,
  exit_reason_code,
  separation_nature_id,
  separation_nature_code,
  is_voluntary_flag,
  is_involuntary_flag,
  COUNT(*) AS exit_count,
  COUNT(DISTINCT trainee_id) AS trainee_count
FROM vw_employment_exit_fact
GROUP BY
  employment_exit_reason_id,
  exit_reason_code,
  separation_nature_id,
  separation_nature_code,
  is_voluntary_flag,
  is_involuntary_flag;

CREATE OR REPLACE VIEW vw_skill_gap_summary AS
SELECT
  skill_id,
  skill_code,
  skill_gap_severity_id,
  severity_code,
  COUNT(*) AS gap_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(is_current) AS current_gap_count,
  AVG(gap_level_delta) AS avg_gap_level_delta
FROM vw_skill_gap_fact
GROUP BY skill_id, skill_code, skill_gap_severity_id, severity_code;

CREATE OR REPLACE VIEW vw_followup_outcome_summary AS
SELECT
  followup_type_code,
  followup_offset_months,
  COUNT(*) AS task_count,
  COUNT(DISTINCT trainee_id) AS trainee_count,
  SUM(CASE WHEN is_success_flag = 1 THEN 1 ELSE 0 END) AS success_count,
  SUM(CASE WHEN is_no_response_flag = 1 THEN 1 ELSE 0 END) AS no_response_count,
  SUM(CASE WHEN is_unreachable_flag = 1 THEN 1 ELSE 0 END) AS unreachable_count,
  ROUND(SUM(CASE WHEN is_success_flag = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0) * 100, 2) AS success_rate_pct
FROM vw_followup_fact
GROUP BY followup_type_code, followup_offset_months;
