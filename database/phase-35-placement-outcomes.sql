-- Refresh enrollment placement outcomes in an existing SIH26135 database.
-- Apply to the selected catalog after the foundational analytical views.
-- Keep this definition in sync with phase-16-analytical-views.sql.

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
  -- Placement outcome for MIS, counted once per enrollment across both sources.
  CASE WHEN COALESCE(pl.joined_placement_count, 0) > 0 OR EXISTS (
    SELECT 1 FROM employment_records reported
    JOIN ref_employment_info_source src USING (employment_info_source_id)
    JOIN ref_engagement_type engagement USING (engagement_type_id)
    JOIN ref_record_verification_status verification USING (record_verification_status_id)
    WHERE reported.enrollment_id = e.enrollment_id AND reported.trainee_id = e.trainee_id
      AND reported.deleted_at IS NULL AND src.source_code = 'TRAINEE_DECLARATION'
      AND verification.status_code <> 'REJECTED' AND engagement.is_wage_employment = 1
      AND st.is_completed_flag = 1 AND reported.start_date <= CURDATE()
      AND reported.start_date >= COALESCE(e.actual_completion_date, b.end_date)
  ) THEN 1 ELSE 0 END AS has_joined_placement_flag,
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
LEFT JOIN training_batches b
  ON b.batch_id = e.batch_id
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
