-- Phase 19 — Final structural and integrity validation
-- Database: SIH26135
-- Dependencies: Phases 00–18 (Phase 17 skipped; no demo/seed data)
-- MySQL 8.0+
-- READ-ONLY. Empty result sets on transactional checks are expected.
-- Do not INSERT/UPDATE/DELETE/ALTER/DROP. Do not wrap in a mutating transaction.

USE SIH26135;

SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- ===========================================================================
-- A. DATABASE OBJECT INVENTORY
-- ===========================================================================

-- A01 Schema exists
SELECT
  SCHEMA_NAME,
  DEFAULT_CHARACTER_SET_NAME,
  DEFAULT_COLLATION_NAME
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME = 'SIH26135';

-- A02 Object counts by TABLE_TYPE
SELECT
  TABLE_TYPE,
  COUNT(*) AS object_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'SIH26135'
GROUP BY TABLE_TYPE
ORDER BY TABLE_TYPE;

-- A03 Unexpected object types (expect BASE TABLE and VIEW only)
SELECT
  TABLE_NAME,
  TABLE_TYPE,
  ENGINE,
  TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'SIH26135'
  AND TABLE_TYPE NOT IN ('BASE TABLE', 'VIEW');

-- A04 BASE TABLE inventory
SELECT
  TABLE_NAME,
  ENGINE,
  TABLE_COLLATION,
  TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'SIH26135'
  AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;

-- A05 VIEW inventory
SELECT
  TABLE_NAME AS view_name
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA = 'SIH26135'
ORDER BY TABLE_NAME;

-- A06 Expected Phase 00–15 base tables missing from the live schema
-- Names derived from CREATE TABLE statements in phases 01–15 (144 tables).
WITH expected_tables AS (
  SELECT 'ref_lifecycle_status' AS table_name UNION ALL SELECT 'ref_location_type' UNION ALL
  SELECT 'ref_organization_type' UNION ALL SELECT 'states' UNION ALL SELECT 'districts' UNION ALL
  SELECT 'blocks' UNION ALL SELECT 'locations' UNION ALL SELECT 'organizations' UNION ALL
  SELECT 'departments' UNION ALL SELECT 'schemes' UNION ALL SELECT 'programs' UNION ALL
  SELECT 'ref_user_status' UNION ALL SELECT 'roles' UNION ALL SELECT 'permissions' UNION ALL
  SELECT 'users' UNION ALL SELECT 'user_roles' UNION ALL SELECT 'role_permissions' UNION ALL
  SELECT 'ref_gender' UNION ALL SELECT 'ref_education_level' UNION ALL
  SELECT 'ref_employment_status' UNION ALL SELECT 'ref_profile_status' UNION ALL
  SELECT 'ref_identity_document_type' UNION ALL SELECT 'ref_consent_type' UNION ALL
  SELECT 'ref_consent_status' UNION ALL SELECT 'trainees' UNION ALL
  SELECT 'trainee_identity_hashes' UNION ALL SELECT 'trainee_consents' UNION ALL
  SELECT 'ref_accreditation_status' UNION ALL SELECT 'training_providers' UNION ALL
  SELECT 'program_training_providers' UNION ALL SELECT 'sectors' UNION ALL SELECT 'industries' UNION ALL
  SELECT 'ref_qualification_level' UNION ALL SELECT 'ref_delivery_mode' UNION ALL
  SELECT 'ref_batch_status' UNION ALL SELECT 'training_centers' UNION ALL SELECT 'courses' UNION ALL
  SELECT 'course_programs' UNION ALL SELECT 'training_batches' UNION ALL
  SELECT 'skill_categories' UNION ALL SELECT 'skill_levels' UNION ALL
  SELECT 'ref_skill_importance' UNION ALL SELECT 'skills' UNION ALL SELECT 'course_skills' UNION ALL
  SELECT 'job_roles' UNION ALL SELECT 'job_role_skills' UNION ALL
  SELECT 'ref_enrollment_status' UNION ALL SELECT 'ref_training_dropout_reason' UNION ALL
  SELECT 'ref_attendance_status' UNION ALL SELECT 'training_enrollments' UNION ALL
  SELECT 'attendance_records' UNION ALL SELECT 'ref_assessment_type' UNION ALL
  SELECT 'ref_assessment_outcome' UNION ALL SELECT 'ref_certificate_status' UNION ALL
  SELECT 'ref_certificate_verification_status' UNION ALL SELECT 'assessments' UNION ALL
  SELECT 'trainee_assessments' UNION ALL SELECT 'assessment_results' UNION ALL
  SELECT 'certifications' UNION ALL SELECT 'ref_company_size' UNION ALL
  SELECT 'ref_engagement_type' UNION ALL SELECT 'ref_salary_frequency' UNION ALL
  SELECT 'ref_job_posting_status' UNION ALL SELECT 'ref_application_status' UNION ALL
  SELECT 'ref_placement_status' UNION ALL SELECT 'ref_placement_source' UNION ALL
  SELECT 'ref_non_selection_reason' UNION ALL SELECT 'ref_offer_status' UNION ALL
  SELECT 'ref_joining_status' UNION ALL SELECT 'ref_record_verification_status' UNION ALL
  SELECT 'employers' UNION ALL SELECT 'employer_branches' UNION ALL SELECT 'job_postings' UNION ALL
  SELECT 'job_posting_skills' UNION ALL SELECT 'job_applications' UNION ALL
  SELECT 'placement_records' UNION ALL SELECT 'placement_offers' UNION ALL
  SELECT 'ref_employment_spell_status' UNION ALL SELECT 'ref_employment_info_source' UNION ALL
  SELECT 'ref_employment_exit_reason' UNION ALL SELECT 'employment_records' UNION ALL
  SELECT 'salary_history' UNION ALL SELECT 'ref_question_type' UNION ALL
  SELECT 'ref_survey_purpose' UNION ALL SELECT 'ref_survey_instance_status' UNION ALL
  SELECT 'ref_survey_response_status' UNION ALL SELECT 'ref_followup_type' UNION ALL
  SELECT 'ref_followup_status' UNION ALL SELECT 'ref_followup_outcome' UNION ALL
  SELECT 'ref_non_response_reason' UNION ALL SELECT 'ref_communication_channel' UNION ALL
  SELECT 'ref_communication_direction' UNION ALL SELECT 'ref_communication_purpose' UNION ALL
  SELECT 'ref_communication_status' UNION ALL SELECT 'trainee_channel_preferences' UNION ALL
  SELECT 'survey_templates' UNION ALL SELECT 'survey_template_versions' UNION ALL
  SELECT 'survey_questions' UNION ALL SELECT 'survey_question_options' UNION ALL
  SELECT 'surveys' UNION ALL SELECT 'followup_campaigns' UNION ALL SELECT 'followup_tasks' UNION ALL
  SELECT 'survey_responses' UNION ALL SELECT 'survey_response_answers' UNION ALL
  SELECT 'survey_response_answer_options' UNION ALL SELECT 'communication_logs' UNION ALL
  SELECT 'ref_employment_verification_method' UNION ALL
  SELECT 'ref_employment_verification_request_status' UNION ALL
  SELECT 'ref_employment_verification_attempt_status' UNION ALL
  SELECT 'ref_employment_verification_rejection_reason' UNION ALL
  SELECT 'ref_employment_verification_evidence_type' UNION ALL
  SELECT 'employment_verification_requests' UNION ALL
  SELECT 'employment_verification_attempts' UNION ALL
  SELECT 'employment_verifications' UNION ALL
  SELECT 'employment_verification_evidence' UNION ALL
  SELECT 'ref_skill_gap_source' UNION ALL SELECT 'ref_skill_gap_severity' UNION ALL
  SELECT 'ref_skill_gap_status' UNION ALL SELECT 'ref_skill_gap_assessment_status' UNION ALL
  SELECT 'ref_skill_gap_action_type' UNION ALL SELECT 'skill_gap_assessments' UNION ALL
  SELECT 'skill_gaps' UNION ALL SELECT 'skill_gap_observations' UNION ALL
  SELECT 'skill_gap_recommendations' UNION ALL SELECT 'ref_unemployment_reason' UNION ALL
  SELECT 'ref_separation_nature' UNION ALL SELECT 'employment_exit_events' UNION ALL
  SELECT 'trainee_unemployment_events' UNION ALL SELECT 'ref_audit_action' UNION ALL
  SELECT 'ref_data_quality_category' UNION ALL SELECT 'ref_data_quality_severity' UNION ALL
  SELECT 'ref_data_quality_issue_status' UNION ALL SELECT 'ref_data_quality_detection_source' UNION ALL
  SELECT 'ref_system_event_category' UNION ALL SELECT 'ref_system_event_severity' UNION ALL
  SELECT 'ref_import_batch_status' UNION ALL SELECT 'ref_import_record_status' UNION ALL
  SELECT 'audit_logs' UNION ALL SELECT 'data_quality_rules' UNION ALL
  SELECT 'data_quality_issues' UNION ALL SELECT 'data_quality_issue_events' UNION ALL
  SELECT 'system_event_logs' UNION ALL SELECT 'import_batches' UNION ALL
  SELECT 'import_records'
)
SELECT e.table_name AS missing_expected_table
FROM expected_tables e
LEFT JOIN information_schema.TABLES t
  ON t.TABLE_SCHEMA = 'SIH26135'
 AND t.TABLE_NAME = e.table_name
 AND t.TABLE_TYPE = 'BASE TABLE'
WHERE t.TABLE_NAME IS NULL
ORDER BY e.table_name;

-- A07 Extra base tables not created by Phases 00–15
WITH expected_tables AS (
  SELECT 'ref_lifecycle_status' AS table_name UNION ALL SELECT 'ref_location_type' UNION ALL
  SELECT 'ref_organization_type' UNION ALL SELECT 'states' UNION ALL SELECT 'districts' UNION ALL
  SELECT 'blocks' UNION ALL SELECT 'locations' UNION ALL SELECT 'organizations' UNION ALL
  SELECT 'departments' UNION ALL SELECT 'schemes' UNION ALL SELECT 'programs' UNION ALL
  SELECT 'ref_user_status' UNION ALL SELECT 'roles' UNION ALL SELECT 'permissions' UNION ALL
  SELECT 'users' UNION ALL SELECT 'user_roles' UNION ALL SELECT 'role_permissions' UNION ALL
  SELECT 'ref_gender' UNION ALL SELECT 'ref_education_level' UNION ALL
  SELECT 'ref_employment_status' UNION ALL SELECT 'ref_profile_status' UNION ALL
  SELECT 'ref_identity_document_type' UNION ALL SELECT 'ref_consent_type' UNION ALL
  SELECT 'ref_consent_status' UNION ALL SELECT 'trainees' UNION ALL
  SELECT 'trainee_identity_hashes' UNION ALL SELECT 'trainee_consents' UNION ALL
  SELECT 'ref_accreditation_status' UNION ALL SELECT 'training_providers' UNION ALL
  SELECT 'program_training_providers' UNION ALL SELECT 'sectors' UNION ALL SELECT 'industries' UNION ALL
  SELECT 'ref_qualification_level' UNION ALL SELECT 'ref_delivery_mode' UNION ALL
  SELECT 'ref_batch_status' UNION ALL SELECT 'training_centers' UNION ALL SELECT 'courses' UNION ALL
  SELECT 'course_programs' UNION ALL SELECT 'training_batches' UNION ALL
  SELECT 'skill_categories' UNION ALL SELECT 'skill_levels' UNION ALL
  SELECT 'ref_skill_importance' UNION ALL SELECT 'skills' UNION ALL SELECT 'course_skills' UNION ALL
  SELECT 'job_roles' UNION ALL SELECT 'job_role_skills' UNION ALL
  SELECT 'ref_enrollment_status' UNION ALL SELECT 'ref_training_dropout_reason' UNION ALL
  SELECT 'ref_attendance_status' UNION ALL SELECT 'training_enrollments' UNION ALL
  SELECT 'attendance_records' UNION ALL SELECT 'ref_assessment_type' UNION ALL
  SELECT 'ref_assessment_outcome' UNION ALL SELECT 'ref_certificate_status' UNION ALL
  SELECT 'ref_certificate_verification_status' UNION ALL SELECT 'assessments' UNION ALL
  SELECT 'trainee_assessments' UNION ALL SELECT 'assessment_results' UNION ALL
  SELECT 'certifications' UNION ALL SELECT 'ref_company_size' UNION ALL
  SELECT 'ref_engagement_type' UNION ALL SELECT 'ref_salary_frequency' UNION ALL
  SELECT 'ref_job_posting_status' UNION ALL SELECT 'ref_application_status' UNION ALL
  SELECT 'ref_placement_status' UNION ALL SELECT 'ref_placement_source' UNION ALL
  SELECT 'ref_non_selection_reason' UNION ALL SELECT 'ref_offer_status' UNION ALL
  SELECT 'ref_joining_status' UNION ALL SELECT 'ref_record_verification_status' UNION ALL
  SELECT 'employers' UNION ALL SELECT 'employer_branches' UNION ALL SELECT 'job_postings' UNION ALL
  SELECT 'job_posting_skills' UNION ALL SELECT 'job_applications' UNION ALL
  SELECT 'placement_records' UNION ALL SELECT 'placement_offers' UNION ALL
  SELECT 'ref_employment_spell_status' UNION ALL SELECT 'ref_employment_info_source' UNION ALL
  SELECT 'ref_employment_exit_reason' UNION ALL SELECT 'employment_records' UNION ALL
  SELECT 'salary_history' UNION ALL SELECT 'ref_question_type' UNION ALL
  SELECT 'ref_survey_purpose' UNION ALL SELECT 'ref_survey_instance_status' UNION ALL
  SELECT 'ref_survey_response_status' UNION ALL SELECT 'ref_followup_type' UNION ALL
  SELECT 'ref_followup_status' UNION ALL SELECT 'ref_followup_outcome' UNION ALL
  SELECT 'ref_non_response_reason' UNION ALL SELECT 'ref_communication_channel' UNION ALL
  SELECT 'ref_communication_direction' UNION ALL SELECT 'ref_communication_purpose' UNION ALL
  SELECT 'ref_communication_status' UNION ALL SELECT 'trainee_channel_preferences' UNION ALL
  SELECT 'survey_templates' UNION ALL SELECT 'survey_template_versions' UNION ALL
  SELECT 'survey_questions' UNION ALL SELECT 'survey_question_options' UNION ALL
  SELECT 'surveys' UNION ALL SELECT 'followup_campaigns' UNION ALL SELECT 'followup_tasks' UNION ALL
  SELECT 'survey_responses' UNION ALL SELECT 'survey_response_answers' UNION ALL
  SELECT 'survey_response_answer_options' UNION ALL SELECT 'communication_logs' UNION ALL
  SELECT 'ref_employment_verification_method' UNION ALL
  SELECT 'ref_employment_verification_request_status' UNION ALL
  SELECT 'ref_employment_verification_attempt_status' UNION ALL
  SELECT 'ref_employment_verification_rejection_reason' UNION ALL
  SELECT 'ref_employment_verification_evidence_type' UNION ALL
  SELECT 'employment_verification_requests' UNION ALL
  SELECT 'employment_verification_attempts' UNION ALL
  SELECT 'employment_verifications' UNION ALL
  SELECT 'employment_verification_evidence' UNION ALL
  SELECT 'ref_skill_gap_source' UNION ALL SELECT 'ref_skill_gap_severity' UNION ALL
  SELECT 'ref_skill_gap_status' UNION ALL SELECT 'ref_skill_gap_assessment_status' UNION ALL
  SELECT 'ref_skill_gap_action_type' UNION ALL SELECT 'skill_gap_assessments' UNION ALL
  SELECT 'skill_gaps' UNION ALL SELECT 'skill_gap_observations' UNION ALL
  SELECT 'skill_gap_recommendations' UNION ALL SELECT 'ref_unemployment_reason' UNION ALL
  SELECT 'ref_separation_nature' UNION ALL SELECT 'employment_exit_events' UNION ALL
  SELECT 'trainee_unemployment_events' UNION ALL SELECT 'ref_audit_action' UNION ALL
  SELECT 'ref_data_quality_category' UNION ALL SELECT 'ref_data_quality_severity' UNION ALL
  SELECT 'ref_data_quality_issue_status' UNION ALL SELECT 'ref_data_quality_detection_source' UNION ALL
  SELECT 'ref_system_event_category' UNION ALL SELECT 'ref_system_event_severity' UNION ALL
  SELECT 'ref_import_batch_status' UNION ALL SELECT 'ref_import_record_status' UNION ALL
  SELECT 'audit_logs' UNION ALL SELECT 'data_quality_rules' UNION ALL
  SELECT 'data_quality_issues' UNION ALL SELECT 'data_quality_issue_events' UNION ALL
  SELECT 'system_event_logs' UNION ALL SELECT 'import_batches' UNION ALL
  SELECT 'import_records'
)
SELECT t.TABLE_NAME AS unexpected_base_table
FROM information_schema.TABLES t
LEFT JOIN expected_tables e
  ON e.table_name = t.TABLE_NAME
WHERE t.TABLE_SCHEMA = 'SIH26135'
  AND t.TABLE_TYPE = 'BASE TABLE'
  AND e.table_name IS NULL
ORDER BY t.TABLE_NAME;

-- A08 Phase 16 analytical views — missing expected views
WITH expected_views AS (
  SELECT 'vw_enrollment_outcome_fact' AS view_name UNION ALL
  SELECT 'vw_placement_fact' UNION ALL
  SELECT 'vw_employment_fact' UNION ALL
  SELECT 'vw_salary_progression_fact' UNION ALL
  SELECT 'vw_employment_retention_fact' UNION ALL
  SELECT 'vw_skill_gap_fact' UNION ALL
  SELECT 'vw_unemployment_fact' UNION ALL
  SELECT 'vw_employment_exit_fact' UNION ALL
  SELECT 'vw_followup_fact' UNION ALL
  SELECT 'vw_survey_response_fact' UNION ALL
  SELECT 'vw_employment_verification_fact' UNION ALL
  SELECT 'vw_trainee_outcome_summary' UNION ALL
  SELECT 'vw_program_outcome_summary' UNION ALL
  SELECT 'vw_provider_outcome_summary' UNION ALL
  SELECT 'vw_course_outcome_summary' UNION ALL
  SELECT 'vw_district_outcome_summary' UNION ALL
  SELECT 'vw_employer_hiring_summary' UNION ALL
  SELECT 'vw_unemployment_reason_summary' UNION ALL
  SELECT 'vw_attrition_reason_summary' UNION ALL
  SELECT 'vw_skill_gap_summary' UNION ALL
  SELECT 'vw_followup_outcome_summary'
)
SELECT e.view_name AS missing_phase16_view
FROM expected_views e
LEFT JOIN information_schema.VIEWS v
  ON v.TABLE_SCHEMA = 'SIH26135'
 AND v.TABLE_NAME = e.view_name
WHERE v.TABLE_NAME IS NULL
ORDER BY e.view_name;

-- A09 Extra views beyond the 21 Phase 16 views
WITH expected_views AS (
  SELECT 'vw_enrollment_outcome_fact' AS view_name UNION ALL
  SELECT 'vw_placement_fact' UNION ALL
  SELECT 'vw_employment_fact' UNION ALL
  SELECT 'vw_salary_progression_fact' UNION ALL
  SELECT 'vw_employment_retention_fact' UNION ALL
  SELECT 'vw_skill_gap_fact' UNION ALL
  SELECT 'vw_unemployment_fact' UNION ALL
  SELECT 'vw_employment_exit_fact' UNION ALL
  SELECT 'vw_followup_fact' UNION ALL
  SELECT 'vw_survey_response_fact' UNION ALL
  SELECT 'vw_employment_verification_fact' UNION ALL
  SELECT 'vw_trainee_outcome_summary' UNION ALL
  SELECT 'vw_program_outcome_summary' UNION ALL
  SELECT 'vw_provider_outcome_summary' UNION ALL
  SELECT 'vw_course_outcome_summary' UNION ALL
  SELECT 'vw_district_outcome_summary' UNION ALL
  SELECT 'vw_employer_hiring_summary' UNION ALL
  SELECT 'vw_unemployment_reason_summary' UNION ALL
  SELECT 'vw_attrition_reason_summary' UNION ALL
  SELECT 'vw_skill_gap_summary' UNION ALL
  SELECT 'vw_followup_outcome_summary'
)
SELECT v.TABLE_NAME AS unexpected_view
FROM information_schema.VIEWS v
LEFT JOIN expected_views e
  ON e.view_name = v.TABLE_NAME
WHERE v.TABLE_SCHEMA = 'SIH26135'
  AND e.view_name IS NULL
ORDER BY v.TABLE_NAME;

-- A10 No Phase 17 seed/demo tables (name pattern only)
SELECT TABLE_NAME, TABLE_TYPE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'SIH26135'
  AND TABLE_NAME REGEXP '(^|_)(demo|seed|sample|fake|dummy|mock)(_|$)';

-- A11 Confirm transactional domains contain no business rows (Phase 17 skipped)
SELECT 'trainees' AS table_name, COUNT(*) AS row_count FROM trainees
UNION ALL SELECT 'users', COUNT(*) FROM users
UNION ALL SELECT 'training_enrollments', COUNT(*) FROM training_enrollments
UNION ALL SELECT 'attendance_records', COUNT(*) FROM attendance_records
UNION ALL SELECT 'assessments', COUNT(*) FROM assessments
UNION ALL SELECT 'trainee_assessments', COUNT(*) FROM trainee_assessments
UNION ALL SELECT 'assessment_results', COUNT(*) FROM assessment_results
UNION ALL SELECT 'certifications', COUNT(*) FROM certifications
UNION ALL SELECT 'job_applications', COUNT(*) FROM job_applications
UNION ALL SELECT 'placement_records', COUNT(*) FROM placement_records
UNION ALL SELECT 'placement_offers', COUNT(*) FROM placement_offers
UNION ALL SELECT 'employment_records', COUNT(*) FROM employment_records
UNION ALL SELECT 'salary_history', COUNT(*) FROM salary_history
UNION ALL SELECT 'followup_tasks', COUNT(*) FROM followup_tasks
UNION ALL SELECT 'survey_responses', COUNT(*) FROM survey_responses
UNION ALL SELECT 'employment_verifications', COUNT(*) FROM employment_verifications
UNION ALL SELECT 'skill_gaps', COUNT(*) FROM skill_gaps
UNION ALL SELECT 'trainee_unemployment_events', COUNT(*) FROM trainee_unemployment_events
UNION ALL SELECT 'audit_logs', COUNT(*) FROM audit_logs
UNION ALL SELECT 'import_records', COUNT(*) FROM import_records;

-- ===========================================================================
-- B. TABLE / SCHEMA STRUCTURE
-- ===========================================================================

-- B01 Tables with no PRIMARY KEY
SELECT t.TABLE_NAME
FROM information_schema.TABLES t
LEFT JOIN information_schema.TABLE_CONSTRAINTS c
  ON c.TABLE_SCHEMA = t.TABLE_SCHEMA
 AND c.TABLE_NAME = t.TABLE_NAME
 AND c.CONSTRAINT_TYPE = 'PRIMARY KEY'
WHERE t.TABLE_SCHEMA = 'SIH26135'
  AND t.TABLE_TYPE = 'BASE TABLE'
  AND c.CONSTRAINT_NAME IS NULL
ORDER BY t.TABLE_NAME;

-- B02 Duplicate PRIMARY KEY constraint definitions
SELECT
  TABLE_NAME,
  COUNT(*) AS primary_key_constraint_count
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'SIH26135'
  AND CONSTRAINT_TYPE = 'PRIMARY KEY'
GROUP BY TABLE_NAME
HAVING COUNT(*) > 1;

-- B03 Nullable columns that participate in a PRIMARY KEY
SELECT
  kcu.TABLE_NAME,
  kcu.COLUMN_NAME,
  c.IS_NULLABLE,
  c.COLUMN_TYPE
FROM information_schema.KEY_COLUMN_USAGE kcu
JOIN information_schema.TABLE_CONSTRAINTS tc
  ON tc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
 AND tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
 AND tc.TABLE_NAME = kcu.TABLE_NAME
JOIN information_schema.COLUMNS c
  ON c.TABLE_SCHEMA = kcu.TABLE_SCHEMA
 AND c.TABLE_NAME = kcu.TABLE_NAME
 AND c.COLUMN_NAME = kcu.COLUMN_NAME
WHERE kcu.TABLE_SCHEMA = 'SIH26135'
  AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
  AND c.IS_NULLABLE = 'YES';

-- B04 Duplicate indexes with identical column sequences
SELECT
  a.TABLE_NAME,
  a.INDEX_NAME AS index_name_a,
  b.INDEX_NAME AS index_name_b,
  a.column_list
FROM (
  SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS column_list
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'SIH26135'
  GROUP BY TABLE_NAME, INDEX_NAME
) a
JOIN (
  SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS column_list
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'SIH26135'
  GROUP BY TABLE_NAME, INDEX_NAME
) b
  ON a.TABLE_NAME = b.TABLE_NAME
 AND a.column_list = b.column_list
 AND a.INDEX_NAME < b.INDEX_NAME
ORDER BY a.TABLE_NAME, a.INDEX_NAME;

-- B05 Duplicate column names within a table (should be empty)
SELECT
  TABLE_NAME,
  COLUMN_NAME,
  COUNT(*) AS definition_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'SIH26135'
GROUP BY TABLE_NAME, COLUMN_NAME
HAVING COUNT(*) > 1;

-- B06 Major domain tables present (boolean flags)
SELECT
  SUM(TABLE_NAME = 'trainees') AS has_trainees,
  SUM(TABLE_NAME = 'users') AS has_users,
  SUM(TABLE_NAME = 'states') AS has_states,
  SUM(TABLE_NAME = 'programs') AS has_programs,
  SUM(TABLE_NAME = 'training_providers') AS has_training_providers,
  SUM(TABLE_NAME = 'training_centers') AS has_training_centers,
  SUM(TABLE_NAME = 'courses') AS has_courses,
  SUM(TABLE_NAME = 'training_batches') AS has_training_batches,
  SUM(TABLE_NAME = 'skills') AS has_skills,
  SUM(TABLE_NAME = 'job_roles') AS has_job_roles,
  SUM(TABLE_NAME = 'training_enrollments') AS has_training_enrollments,
  SUM(TABLE_NAME = 'attendance_records') AS has_attendance_records,
  SUM(TABLE_NAME = 'assessments') AS has_assessments,
  SUM(TABLE_NAME = 'certifications') AS has_certifications,
  SUM(TABLE_NAME = 'employers') AS has_employers,
  SUM(TABLE_NAME = 'job_postings') AS has_job_postings,
  SUM(TABLE_NAME = 'job_applications') AS has_job_applications,
  SUM(TABLE_NAME = 'placement_records') AS has_placement_records,
  SUM(TABLE_NAME = 'employment_records') AS has_employment_records,
  SUM(TABLE_NAME = 'salary_history') AS has_salary_history,
  SUM(TABLE_NAME = 'surveys') AS has_surveys,
  SUM(TABLE_NAME = 'followup_tasks') AS has_followup_tasks,
  SUM(TABLE_NAME = 'employment_verifications') AS has_employment_verifications,
  SUM(TABLE_NAME = 'skill_gaps') AS has_skill_gaps,
  SUM(TABLE_NAME = 'trainee_unemployment_events') AS has_trainee_unemployment_events,
  SUM(TABLE_NAME = 'audit_logs') AS has_audit_logs,
  SUM(TABLE_NAME = 'import_batches') AS has_import_batches
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'SIH26135'
  AND TABLE_TYPE = 'BASE TABLE';

-- ===========================================================================
-- C. FOREIGN KEY INTEGRITY
-- ===========================================================================

-- C01 Foreign key count
SELECT COUNT(*) AS foreign_key_count
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'SIH26135';

-- C02 Foreign key catalog
SELECT
  rc.CONSTRAINT_NAME,
  rc.TABLE_NAME,
  rc.REFERENCED_TABLE_NAME,
  rc.UPDATE_RULE,
  rc.DELETE_RULE,
  kcu.COLUMN_NAME,
  kcu.REFERENCED_COLUMN_NAME,
  kcu.ORDINAL_POSITION
FROM information_schema.REFERENTIAL_CONSTRAINTS rc
JOIN information_schema.KEY_COLUMN_USAGE kcu
  ON kcu.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA
 AND kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME
 AND kcu.TABLE_NAME = rc.TABLE_NAME
WHERE rc.CONSTRAINT_SCHEMA = 'SIH26135'
ORDER BY rc.TABLE_NAME, rc.CONSTRAINT_NAME, kcu.ORDINAL_POSITION;

-- C03 FK references to missing tables
SELECT
  kcu.CONSTRAINT_NAME,
  kcu.TABLE_NAME,
  kcu.COLUMN_NAME,
  kcu.REFERENCED_TABLE_NAME,
  kcu.REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE kcu
WHERE kcu.TABLE_SCHEMA = 'SIH26135'
  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM information_schema.TABLES t
    WHERE t.TABLE_SCHEMA = kcu.TABLE_SCHEMA
      AND t.TABLE_NAME = kcu.REFERENCED_TABLE_NAME
  );

-- C04 FK references to missing columns
SELECT
  kcu.CONSTRAINT_NAME,
  kcu.TABLE_NAME,
  kcu.COLUMN_NAME,
  kcu.REFERENCED_TABLE_NAME,
  kcu.REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE kcu
WHERE kcu.TABLE_SCHEMA = 'SIH26135'
  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS c
    WHERE c.TABLE_SCHEMA = kcu.TABLE_SCHEMA
      AND c.TABLE_NAME = kcu.REFERENCED_TABLE_NAME
      AND c.COLUMN_NAME = kcu.REFERENCED_COLUMN_NAME
  );

-- C05 Orphan checks for denormalized / copied keys
-- InnoDB FKs prevent true orphans; these catch copied-column mismatches.

SELECT ar.attendance_id, ar.enrollment_id, ar.trainee_id, ar.batch_id
FROM attendance_records ar
JOIN training_enrollments e ON e.enrollment_id = ar.enrollment_id
WHERE ar.trainee_id <> e.trainee_id
   OR ar.batch_id <> e.batch_id;

SELECT ta.trainee_assessment_id, ta.assessment_id, ta.enrollment_id, ta.trainee_id
FROM trainee_assessments ta
JOIN training_enrollments e ON e.enrollment_id = ta.enrollment_id
WHERE ta.trainee_id <> e.trainee_id;

SELECT ar.assessment_result_id, ar.trainee_assessment_id, ar.trainee_id
FROM assessment_results ar
JOIN trainee_assessments ta ON ta.trainee_assessment_id = ar.trainee_assessment_id
WHERE ar.trainee_id <> ta.trainee_id;

SELECT c.certification_id, c.trainee_id, c.enrollment_id, c.course_id, c.program_id
FROM certifications c
JOIN training_enrollments e ON e.enrollment_id = c.enrollment_id
WHERE c.trainee_id <> e.trainee_id
   OR c.course_id <> e.course_id
   OR c.program_id <> e.program_id;

SELECT pr.placement_id, pr.trainee_id, pr.enrollment_id, pr.course_id, pr.program_id, pr.provider_id
FROM placement_records pr
JOIN training_enrollments e ON e.enrollment_id = pr.enrollment_id
WHERE pr.trainee_id <> e.trainee_id
   OR pr.course_id <> e.course_id
   OR pr.program_id <> e.program_id
   OR pr.provider_id <> e.provider_id;

SELECT pr.placement_id, pr.job_application_id, pr.trainee_id, pr.job_posting_id
FROM placement_records pr
JOIN job_applications ja ON ja.job_application_id = pr.job_application_id
WHERE pr.job_application_id IS NOT NULL
  AND (
    pr.trainee_id <> ja.trainee_id
    OR (pr.job_posting_id IS NOT NULL AND ja.job_posting_id <> pr.job_posting_id)
    OR (pr.enrollment_id IS NOT NULL AND ja.enrollment_id IS NOT NULL AND pr.enrollment_id <> ja.enrollment_id)
  );

SELECT er.employment_id, er.trainee_id, er.placement_id
FROM employment_records er
JOIN placement_records pr ON pr.placement_id = er.placement_id
WHERE er.placement_id IS NOT NULL
  AND er.trainee_id <> pr.trainee_id;

SELECT sh.salary_history_id, sh.employment_id, sh.trainee_id
FROM salary_history sh
JOIN employment_records er ON er.employment_id = sh.employment_id
WHERE sh.trainee_id <> er.trainee_id;

SELECT r.employment_verification_request_id, r.employment_id, r.trainee_id, r.placement_id
FROM employment_verification_requests r
JOIN employment_records er ON er.employment_id = r.employment_id
WHERE r.trainee_id <> er.trainee_id
   OR (r.placement_id IS NOT NULL AND er.placement_id IS NOT NULL AND r.placement_id <> er.placement_id);

SELECT v.employment_verification_id, v.employment_id, v.trainee_id, v.placement_id
FROM employment_verifications v
JOIN employment_records er ON er.employment_id = v.employment_id
WHERE v.trainee_id <> er.trainee_id
   OR (v.placement_id IS NOT NULL AND er.placement_id IS NOT NULL AND v.placement_id <> er.placement_id);

SELECT g.skill_gap_id, g.skill_gap_assessment_id, g.trainee_id
FROM skill_gaps g
JOIN skill_gap_assessments a ON a.skill_gap_assessment_id = g.skill_gap_assessment_id
WHERE g.trainee_id <> a.trainee_id;

SELECT u.unemployment_event_id, u.trainee_id, u.preceding_employment_id, u.succeeding_employment_id
FROM trainee_unemployment_events u
LEFT JOIN employment_records pre ON pre.employment_id = u.preceding_employment_id
LEFT JOIN employment_records suc ON suc.employment_id = u.succeeding_employment_id
WHERE (u.preceding_employment_id IS NOT NULL AND pre.trainee_id <> u.trainee_id)
   OR (u.succeeding_employment_id IS NOT NULL AND suc.trainee_id <> u.trainee_id);

SELECT x.employment_exit_event_id, x.employment_id, x.trainee_id
FROM employment_exit_events x
JOIN employment_records er ON er.employment_id = x.employment_id
WHERE x.trainee_id <> er.trainee_id;

SELECT ft.followup_task_id, ft.trainee_id, ft.enrollment_id, ft.employment_id, ft.placement_id
FROM followup_tasks ft
LEFT JOIN training_enrollments e ON e.enrollment_id = ft.enrollment_id
LEFT JOIN employment_records er ON er.employment_id = ft.employment_id
LEFT JOIN placement_records pr ON pr.placement_id = ft.placement_id
WHERE (ft.enrollment_id IS NOT NULL AND e.trainee_id <> ft.trainee_id)
   OR (ft.employment_id IS NOT NULL AND er.trainee_id <> ft.trainee_id)
   OR (ft.placement_id IS NOT NULL AND pr.trainee_id <> ft.trainee_id);

-- C06 Geography parent consistency for trainees
SELECT t.trainee_id, t.state_id, t.district_id, d.state_id AS district_state_id
FROM trainees t
JOIN districts d ON d.district_id = t.district_id
WHERE t.state_id <> d.state_id;

-- ===========================================================================
-- D. UNIQUE / DUPLICATE DATA INTEGRITY
-- ===========================================================================

-- D01 UNIQUE constraints from metadata
SELECT
  tc.TABLE_NAME,
  tc.CONSTRAINT_NAME,
  GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION) AS column_list
FROM information_schema.TABLE_CONSTRAINTS tc
JOIN information_schema.KEY_COLUMN_USAGE kcu
  ON kcu.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
 AND kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
 AND kcu.TABLE_NAME = tc.TABLE_NAME
WHERE tc.TABLE_SCHEMA = 'SIH26135'
  AND tc.CONSTRAINT_TYPE = 'UNIQUE'
GROUP BY tc.TABLE_NAME, tc.CONSTRAINT_NAME
ORDER BY tc.TABLE_NAME, tc.CONSTRAINT_NAME;

-- D02 Duplicate business codes on populated master/reference tables
SELECT 'states.state_code' AS check_name, state_code AS duplicate_value, COUNT(*) AS row_count
FROM states GROUP BY state_code HAVING COUNT(*) > 1
UNION ALL
SELECT 'districts.lgd_code', lgd_code, COUNT(*) FROM districts GROUP BY lgd_code HAVING COUNT(*) > 1
UNION ALL
SELECT 'programs.program_code', program_code, COUNT(*) FROM programs GROUP BY program_code HAVING COUNT(*) > 1
UNION ALL
SELECT 'users.username', username, COUNT(*) FROM users GROUP BY username HAVING COUNT(*) > 1
UNION ALL
SELECT 'trainees.registration_number', registration_number, COUNT(*) FROM trainees GROUP BY registration_number HAVING COUNT(*) > 1
UNION ALL
SELECT 'training_enrollments.enrollment_number', enrollment_number, COUNT(*) FROM training_enrollments GROUP BY enrollment_number HAVING COUNT(*) > 1
UNION ALL
SELECT 'employment_records.employment_number', employment_number, COUNT(*) FROM employment_records GROUP BY employment_number HAVING COUNT(*) > 1
UNION ALL
SELECT 'placement_records.placement_number', placement_number, COUNT(*) FROM placement_records GROUP BY placement_number HAVING COUNT(*) > 1
UNION ALL
SELECT 'skills.skill_code', skill_code, COUNT(*) FROM skills GROUP BY skill_code HAVING COUNT(*) > 1;

-- D03 Duplicate UNIQUE index names across tables in the same schema (informational)
SELECT
  CONSTRAINT_NAME,
  COUNT(*) AS table_count,
  GROUP_CONCAT(TABLE_NAME ORDER BY TABLE_NAME) AS tables
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'SIH26135'
  AND CONSTRAINT_TYPE = 'UNIQUE'
GROUP BY CONSTRAINT_NAME
HAVING COUNT(*) > 1;

-- ===========================================================================
-- E. CHECK CONSTRAINTS
-- ===========================================================================

-- E01 Live CHECK constraints (MySQL 8 CHECK_CONSTRAINTS)
SELECT
  cc.CONSTRAINT_SCHEMA,
  cc.CONSTRAINT_NAME,
  tc.TABLE_NAME,
  cc.CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS cc
LEFT JOIN information_schema.TABLE_CONSTRAINTS tc
  ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA
 AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
 AND tc.CONSTRAINT_TYPE = 'CHECK'
WHERE cc.CONSTRAINT_SCHEMA = 'SIH26135'
ORDER BY tc.TABLE_NAME, cc.CONSTRAINT_NAME;

-- E02 CHECK constraints with empty CHECK_CLAUSE
SELECT CONSTRAINT_NAME, CHECK_CLAUSE
FROM information_schema.CHECK_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'SIH26135'
  AND (CHECK_CLAUSE IS NULL OR CHAR_LENGTH(TRIM(CHECK_CLAUSE)) = 0);

-- E03 Expected CHECK names from Phases 01–15 that are missing
WITH expected_checks AS (
  SELECT 'ck_locations_pincode' AS constraint_name UNION ALL SELECT 'ck_locations_lat' UNION ALL
  SELECT 'ck_locations_lng' UNION ALL SELECT 'ck_schemes_budget' UNION ALL SELECT 'ck_schemes_dates' UNION ALL
  SELECT 'ck_programs_budget' UNION ALL SELECT 'ck_programs_dates' UNION ALL
  SELECT 'ck_users_failed_login' UNION ALL SELECT 'ck_trainees_pincode' UNION ALL
  SELECT 'ck_trainee_identity_hash_hex' UNION ALL SELECT 'ck_trainee_consents_revoke_after_grant' UNION ALL
  SELECT 'ck_training_providers_rating' UNION ALL SELECT 'ck_training_providers_pincode' UNION ALL
  SELECT 'ck_ptp_dates' UNION ALL SELECT 'ck_ref_qualification_nsqf' UNION ALL
  SELECT 'ck_training_centers_capacity' UNION ALL SELECT 'ck_training_centers_pincode' UNION ALL
  SELECT 'ck_courses_duration_hours' UNION ALL SELECT 'ck_courses_duration_days' UNION ALL
  SELECT 'ck_training_batches_capacity' UNION ALL SELECT 'ck_training_batches_dates' UNION ALL
  SELECT 'ck_skill_levels_rank' UNION ALL SELECT 'ck_ref_skill_importance_weight' UNION ALL
  SELECT 'ck_enrollments_start_after_enroll' UNION ALL SELECT 'ck_enrollments_expected_after_start' UNION ALL
  SELECT 'ck_enrollments_actual_after_start' UNION ALL SELECT 'ck_attendance_session_sequence' UNION ALL
  SELECT 'ck_attendance_session_times' UNION ALL SELECT 'ck_assessments_maximum_score' UNION ALL
  SELECT 'ck_assessments_pass_score' UNION ALL SELECT 'ck_trainee_assessments_attempt' UNION ALL
  SELECT 'ck_assessment_results_maximum_score' UNION ALL SELECT 'ck_assessment_results_obtained_score' UNION ALL
  SELECT 'ck_assessment_results_percentage' UNION ALL SELECT 'ck_certifications_expiry' UNION ALL
  SELECT 'ck_ref_engagement_type_flags' UNION ALL SELECT 'ck_employers_pincode' UNION ALL
  SELECT 'ck_employers_gstin' UNION ALL SELECT 'ck_employer_branches_pincode' UNION ALL
  SELECT 'ck_job_postings_vacancies' UNION ALL SELECT 'ck_job_postings_salary_range' UNION ALL
  SELECT 'ck_job_postings_min_salary' UNION ALL SELECT 'ck_job_postings_max_salary' UNION ALL
  SELECT 'ck_job_postings_dates' UNION ALL SELECT 'ck_placement_records_offered_salary' UNION ALL
  SELECT 'ck_placement_records_joining_salary' UNION ALL SELECT 'ck_placement_records_offer_before_join' UNION ALL
  SELECT 'ck_placement_offers_salary' UNION ALL SELECT 'ck_placement_offers_dates' UNION ALL
  SELECT 'ck_employment_dates' UNION ALL SELECT 'ck_employment_starting_salary' UNION ALL
  SELECT 'ck_employment_current_end' UNION ALL SELECT 'ck_salary_history_amount' UNION ALL
  SELECT 'ck_salary_history_dates' UNION ALL SELECT 'ck_salary_history_milestone' UNION ALL
  SELECT 'ck_ref_survey_purpose_offset' UNION ALL SELECT 'ck_ref_followup_type_offset' UNION ALL
  SELECT 'ck_survey_template_versions_number' UNION ALL SELECT 'ck_survey_template_versions_dates' UNION ALL
  SELECT 'ck_survey_questions_order' UNION ALL SELECT 'ck_survey_question_options_order' UNION ALL
  SELECT 'ck_surveys_offset' UNION ALL SELECT 'ck_surveys_dates' UNION ALL
  SELECT 'ck_followup_campaigns_dates' UNION ALL SELECT 'ck_followup_tasks_next_date' UNION ALL
  SELECT 'ck_survey_responses_attempt' UNION ALL SELECT 'ck_survey_responses_submitted' UNION ALL
  SELECT 'ck_emp_verif_requests_cycle' UNION ALL SELECT 'ck_emp_verif_requests_reverify' UNION ALL
  SELECT 'ck_emp_verif_requests_due' UNION ALL SELECT 'ck_emp_verif_requests_first_attempt' UNION ALL
  SELECT 'ck_emp_verif_requests_completed' UNION ALL SELECT 'ck_emp_verif_attempts_number' UNION ALL
  SELECT 'ck_emp_verif_attempts_completed' UNION ALL SELECT 'ck_emp_verifications_cycle' UNION ALL
  SELECT 'ck_emp_verifications_reverify' UNION ALL SELECT 'ck_emp_verifications_verified_at' UNION ALL
  SELECT 'ck_emp_verifications_outcome_at' UNION ALL SELECT 'ck_emp_verifications_rejection_notes' UNION ALL
  SELECT 'ck_emp_verif_evidence_sha256' UNION ALL SELECT 'ck_ref_skill_gap_severity_rank' UNION ALL
  SELECT 'ck_skill_gaps_delta' UNION ALL SELECT 'ck_skill_gaps_dates' UNION ALL
  SELECT 'ck_sgo_number' UNION ALL SELECT 'ck_sgo_delta' UNION ALL
  SELECT 'ck_sgr_number' UNION ALL SELECT 'ck_sgr_accepted_on' UNION ALL
  SELECT 'ck_ref_separation_nature_flags' UNION ALL SELECT 'ck_unemployment_events_period_number' UNION ALL
  SELECT 'ck_unemployment_events_dates' UNION ALL SELECT 'ck_unemployment_events_current_end' UNION ALL
  SELECT 'ck_unemployment_events_distinct_jobs' UNION ALL SELECT 'ck_dq_issues_resolved_at' UNION ALL
  SELECT 'ck_import_batches_counts' UNION ALL SELECT 'ck_import_batches_success_count' UNION ALL
  SELECT 'ck_import_batches_failure_count' UNION ALL SELECT 'ck_import_batches_dates' UNION ALL
  SELECT 'ck_import_records_row_number'
)
SELECT e.constraint_name AS missing_check_constraint
FROM expected_checks e
LEFT JOIN information_schema.CHECK_CONSTRAINTS cc
  ON cc.CONSTRAINT_SCHEMA = 'SIH26135'
 AND cc.CONSTRAINT_NAME = e.constraint_name
WHERE cc.CONSTRAINT_NAME IS NULL
ORDER BY e.constraint_name;

-- ===========================================================================
-- F. NULL / DOMAIN VALIDATION
-- Re-evaluate documented CHECKs on live rows. Empty sets expected.
-- ===========================================================================

SELECT enrollment_id, enrollment_date, start_date
FROM training_enrollments
WHERE start_date IS NOT NULL AND start_date < enrollment_date;

SELECT enrollment_id, start_date, expected_completion_date, actual_completion_date
FROM training_enrollments
WHERE (expected_completion_date IS NOT NULL AND start_date IS NOT NULL AND expected_completion_date < start_date)
   OR (actual_completion_date IS NOT NULL AND start_date IS NOT NULL AND actual_completion_date < start_date);

SELECT attendance_id, session_sequence, session_start_time, session_end_time
FROM attendance_records
WHERE session_sequence < 1
   OR (session_end_time IS NOT NULL AND session_start_time IS NOT NULL AND session_end_time < session_start_time);

SELECT batch_id, start_date, end_date, capacity
FROM training_batches
WHERE capacity <= 0
   OR (end_date IS NOT NULL AND end_date < start_date);

SELECT employment_id, start_date, end_date, is_current, starting_salary
FROM employment_records
WHERE (end_date IS NOT NULL AND end_date < start_date)
   OR (is_current = 1 AND end_date IS NOT NULL)
   OR (starting_salary IS NOT NULL AND starting_salary < 0);

SELECT salary_history_id, salary_amount, effective_from, effective_to, observation_month_offset
FROM salary_history
WHERE salary_amount < 0
   OR (effective_to IS NOT NULL AND effective_to < effective_from)
   OR (
     observation_month_offset IS NOT NULL
     AND observation_month_offset NOT IN (0, 6, 12, 24, 36)
   );

SELECT placement_id, offered_salary, joining_salary, offer_date, actual_joining_date
FROM placement_records
WHERE (offered_salary IS NOT NULL AND offered_salary < 0)
   OR (joining_salary IS NOT NULL AND joining_salary < 0)
   OR (actual_joining_date IS NOT NULL AND offer_date IS NOT NULL AND actual_joining_date < offer_date);

SELECT job_posting_id, vacancies, min_salary, max_salary, posted_date, closing_date
FROM job_postings
WHERE vacancies < 1
   OR (min_salary IS NOT NULL AND min_salary < 0)
   OR (max_salary IS NOT NULL AND max_salary < 0)
   OR (min_salary IS NOT NULL AND max_salary IS NOT NULL AND max_salary < min_salary)
   OR (closing_date IS NOT NULL AND closing_date < posted_date);

SELECT unemployment_event_id, period_number, start_date, end_date, is_current,
       preceding_employment_id, succeeding_employment_id
FROM trainee_unemployment_events
WHERE period_number < 1
   OR (end_date IS NOT NULL AND end_date < start_date)
   OR (is_current = 1 AND end_date IS NOT NULL)
   OR (
     preceding_employment_id IS NOT NULL
     AND succeeding_employment_id IS NOT NULL
     AND preceding_employment_id = succeeding_employment_id
   );

SELECT skill_gap_id, gap_level_delta, identified_on, resolved_on, is_current
FROM skill_gaps
WHERE (gap_level_delta IS NOT NULL AND gap_level_delta NOT BETWEEN -4 AND 4)
   OR (resolved_on IS NOT NULL AND resolved_on < identified_on);

SELECT trainee_assessment_id, attempt_number
FROM trainee_assessments
WHERE attempt_number < 1;

SELECT assessment_result_id, maximum_score, obtained_score, score_percentage
FROM assessment_results
WHERE maximum_score <= 0
   OR (obtained_score IS NOT NULL AND (obtained_score < 0 OR obtained_score > maximum_score))
   OR (score_percentage IS NOT NULL AND (score_percentage < 0 OR score_percentage > 100));

SELECT certification_id, issue_date, expiry_date
FROM certifications
WHERE expiry_date IS NOT NULL AND expiry_date < issue_date;

SELECT survey_response_id, attempt_number, started_at, submitted_at
FROM survey_responses
WHERE attempt_number < 1
   OR (submitted_at IS NOT NULL AND submitted_at < started_at);

SELECT import_batch_id, row_count, success_count, failure_count, started_at, completed_at
FROM import_batches
WHERE (row_count IS NOT NULL AND row_count < 0)
   OR (success_count IS NOT NULL AND success_count < 0)
   OR (failure_count IS NOT NULL AND failure_count < 0)
   OR (completed_at IS NOT NULL AND started_at IS NOT NULL AND completed_at < started_at);

SELECT trainee_id, pincode
FROM trainees
WHERE pincode IS NOT NULL AND pincode NOT REGEXP '^[1-9][0-9]{5}$';

SELECT trainee_identity_hash_id, identity_hash
FROM trainee_identity_hashes
WHERE identity_hash NOT REGEXP '^[0-9a-f]{64}$';

-- ===========================================================================
-- G. CROSS-DOMAIN CONSISTENCY
-- ===========================================================================

-- G01 Enrollment copied keys must match the referenced batch
SELECT e.enrollment_id, e.batch_id, e.course_id, e.program_id, e.provider_id, e.center_id
FROM training_enrollments e
JOIN training_batches b ON b.batch_id = e.batch_id
WHERE e.course_id <> b.course_id
   OR e.program_id <> b.program_id
   OR e.provider_id <> b.provider_id
   OR e.center_id <> b.center_id;

-- G02 Assessment batch/course/program alignment
SELECT a.assessment_id, a.batch_id, a.course_id, a.program_id
FROM assessments a
JOIN training_batches b ON b.batch_id = a.batch_id
WHERE a.course_id <> b.course_id
   OR a.program_id <> b.program_id;

-- G03 Trainee assessment belongs to the assessment batch enrollment
SELECT ta.trainee_assessment_id, ta.assessment_id, ta.enrollment_id
FROM trainee_assessments ta
JOIN assessments a ON a.assessment_id = ta.assessment_id
JOIN training_enrollments e ON e.enrollment_id = ta.enrollment_id
WHERE e.batch_id <> a.batch_id
   OR e.course_id <> a.course_id
   OR e.program_id <> a.program_id;

-- G04 Placement application trainee/posting/enrollment alignment is in C05.
-- Employment linked to enrollment must keep the same trainee.
SELECT er.employment_id, er.trainee_id, er.enrollment_id
FROM employment_records er
JOIN training_enrollments e ON e.enrollment_id = er.enrollment_id
WHERE er.enrollment_id IS NOT NULL
  AND er.trainee_id <> e.trainee_id;

-- G05 Salary history effective_from should not precede employment.start_date
SELECT sh.salary_history_id, sh.employment_id, sh.effective_from, er.start_date
FROM salary_history sh
JOIN employment_records er ON er.employment_id = sh.employment_id
WHERE sh.effective_from < er.start_date;

-- G06 Verification cycle consistency vs parent request
SELECT v.employment_verification_id, v.employment_verification_request_id, v.cycle_number
FROM employment_verifications v
JOIN employment_verification_requests r
  ON r.employment_verification_request_id = v.employment_verification_request_id
WHERE v.employment_verification_request_id IS NOT NULL
  AND (
    v.employment_id <> r.employment_id
    OR v.trainee_id <> r.trainee_id
    OR v.cycle_number <> r.cycle_number
  );

-- G07 Skill-gap assessment optional enrollment trainee match
SELECT a.skill_gap_assessment_id, a.trainee_id, a.enrollment_id
FROM skill_gap_assessments a
JOIN training_enrollments e ON e.enrollment_id = a.enrollment_id
WHERE a.enrollment_id IS NOT NULL
  AND a.trainee_id <> e.trainee_id;

-- G08 Unemployment preceding employment must end on/before period start
SELECT u.unemployment_event_id, u.start_date, pre.employment_id, pre.end_date
FROM trainee_unemployment_events u
JOIN employment_records pre ON pre.employment_id = u.preceding_employment_id
WHERE u.preceding_employment_id IS NOT NULL
  AND pre.end_date IS NOT NULL
  AND u.start_date < pre.end_date;

-- G09 Survey answers must belong to questions on the response version
SELECT ans.survey_response_answer_id, sr.survey_template_version_id, q.survey_template_version_id AS question_version_id
FROM survey_response_answers ans
JOIN survey_responses sr ON sr.survey_response_id = ans.survey_response_id
JOIN survey_questions q ON q.survey_question_id = ans.survey_question_id
WHERE sr.survey_template_version_id <> q.survey_template_version_id;

-- G10 Survey response version should match the survey instance version
SELECT sr.survey_response_id, sr.survey_id, sr.survey_template_version_id, s.survey_template_version_id AS survey_version_id
FROM survey_responses sr
JOIN surveys s ON s.survey_id = sr.survey_id
WHERE sr.survey_template_version_id <> s.survey_template_version_id;

-- G11 Follow-up task campaign/survey optional alignment
SELECT ft.followup_task_id, ft.survey_id, fc.survey_id AS campaign_survey_id
FROM followup_tasks ft
JOIN followup_campaigns fc ON fc.followup_campaign_id = ft.followup_campaign_id
WHERE ft.survey_id IS NOT NULL
  AND fc.survey_id IS NOT NULL
  AND ft.survey_id <> fc.survey_id;

-- G12 Import records must belong to an existing batch/status (FK plus status master)
SELECT ir.import_record_id, ir.import_batch_id, ir.import_record_status_id
FROM import_records ir
LEFT JOIN import_batches ib ON ib.import_batch_id = ir.import_batch_id
LEFT JOIN ref_import_record_status st ON st.import_record_status_id = ir.import_record_status_id
WHERE ib.import_batch_id IS NULL
   OR st.import_record_status_id IS NULL;

-- G13 Audit logs referencing a missing user when actor is set
SELECT al.audit_log_id, al.actor_user_id, al.audit_action_id
FROM audit_logs al
LEFT JOIN users u ON u.user_id = al.actor_user_id
LEFT JOIN ref_audit_action a ON a.audit_action_id = al.audit_action_id
WHERE (al.actor_user_id IS NOT NULL AND u.user_id IS NULL)
   OR a.audit_action_id IS NULL;

-- ===========================================================================
-- H. ANALYTICAL VIEW CONSISTENCY
-- ===========================================================================

-- H01 View definitions present
SELECT
  TABLE_NAME AS view_name,
  CHAR_LENGTH(VIEW_DEFINITION) AS definition_length,
  IS_UPDATABLE
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA = 'SIH26135'
ORDER BY TABLE_NAME;

-- H02 Lightweight COUNT(*) per Phase 16 view
SELECT 'vw_enrollment_outcome_fact' AS view_name, COUNT(*) AS row_count FROM vw_enrollment_outcome_fact
UNION ALL SELECT 'vw_placement_fact', COUNT(*) FROM vw_placement_fact
UNION ALL SELECT 'vw_employment_fact', COUNT(*) FROM vw_employment_fact
UNION ALL SELECT 'vw_salary_progression_fact', COUNT(*) FROM vw_salary_progression_fact
UNION ALL SELECT 'vw_employment_retention_fact', COUNT(*) FROM vw_employment_retention_fact
UNION ALL SELECT 'vw_skill_gap_fact', COUNT(*) FROM vw_skill_gap_fact
UNION ALL SELECT 'vw_unemployment_fact', COUNT(*) FROM vw_unemployment_fact
UNION ALL SELECT 'vw_employment_exit_fact', COUNT(*) FROM vw_employment_exit_fact
UNION ALL SELECT 'vw_followup_fact', COUNT(*) FROM vw_followup_fact
UNION ALL SELECT 'vw_survey_response_fact', COUNT(*) FROM vw_survey_response_fact
UNION ALL SELECT 'vw_employment_verification_fact', COUNT(*) FROM vw_employment_verification_fact
UNION ALL SELECT 'vw_trainee_outcome_summary', COUNT(*) FROM vw_trainee_outcome_summary
UNION ALL SELECT 'vw_program_outcome_summary', COUNT(*) FROM vw_program_outcome_summary
UNION ALL SELECT 'vw_provider_outcome_summary', COUNT(*) FROM vw_provider_outcome_summary
UNION ALL SELECT 'vw_course_outcome_summary', COUNT(*) FROM vw_course_outcome_summary
UNION ALL SELECT 'vw_district_outcome_summary', COUNT(*) FROM vw_district_outcome_summary
UNION ALL SELECT 'vw_employer_hiring_summary', COUNT(*) FROM vw_employer_hiring_summary
UNION ALL SELECT 'vw_unemployment_reason_summary', COUNT(*) FROM vw_unemployment_reason_summary
UNION ALL SELECT 'vw_attrition_reason_summary', COUNT(*) FROM vw_attrition_reason_summary
UNION ALL SELECT 'vw_skill_gap_summary', COUNT(*) FROM vw_skill_gap_summary
UNION ALL SELECT 'vw_followup_outcome_summary', COUNT(*) FROM vw_followup_outcome_summary;

-- H03 Trainee-linked views must not reference missing trainees
SELECT 'vw_enrollment_outcome_fact' AS view_name, v.trainee_id
FROM vw_enrollment_outcome_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_placement_fact', v.trainee_id
FROM vw_placement_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_employment_fact', v.trainee_id
FROM vw_employment_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_salary_progression_fact', v.trainee_id
FROM vw_salary_progression_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_employment_retention_fact', v.trainee_id
FROM vw_employment_retention_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_skill_gap_fact', v.trainee_id
FROM vw_skill_gap_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_unemployment_fact', v.trainee_id
FROM vw_unemployment_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_employment_exit_fact', v.trainee_id
FROM vw_employment_exit_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_followup_fact', v.trainee_id
FROM vw_followup_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_survey_response_fact', v.trainee_id
FROM vw_survey_response_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_employment_verification_fact', v.trainee_id
FROM vw_employment_verification_fact v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL
UNION ALL
SELECT 'vw_trainee_outcome_summary', v.trainee_id
FROM vw_trainee_outcome_summary v
LEFT JOIN trainees t ON t.trainee_id = v.trainee_id
WHERE t.trainee_id IS NULL;

-- H04 Aggregate views: impossible negative counts/amounts
SELECT 'vw_program_outcome_summary' AS view_name, program_id AS grain_id
FROM vw_program_outcome_summary
WHERE enrollment_count < 0 OR trainee_count < 0 OR completed_count < 0
   OR certified_count < 0 OR joined_placement_count < 0 OR employed_count < 0
UNION ALL
SELECT 'vw_provider_outcome_summary', provider_id
FROM vw_provider_outcome_summary
WHERE enrollment_count < 0 OR trainee_count < 0 OR completed_count < 0
UNION ALL
SELECT 'vw_course_outcome_summary', course_id
FROM vw_course_outcome_summary
WHERE enrollment_count < 0 OR trainee_count < 0 OR completed_count < 0
UNION ALL
SELECT 'vw_district_outcome_summary', district_id
FROM vw_district_outcome_summary
WHERE enrollment_count < 0 OR trainee_count < 0 OR completed_count < 0
UNION ALL
SELECT 'vw_employer_hiring_summary', employer_id
FROM vw_employer_hiring_summary
WHERE placement_count < 0 OR trainee_count < 0 OR joined_count < 0
   OR (avg_joining_salary IS NOT NULL AND avg_joining_salary < 0)
UNION ALL
SELECT 'vw_unemployment_reason_summary', unemployment_reason_id
FROM vw_unemployment_reason_summary
WHERE period_count < 0 OR trainee_count < 0 OR current_period_count < 0
UNION ALL
SELECT 'vw_attrition_reason_summary', employment_exit_reason_id
FROM vw_attrition_reason_summary
WHERE exit_count < 0 OR trainee_count < 0
UNION ALL
SELECT 'vw_skill_gap_summary', skill_id
FROM vw_skill_gap_summary
WHERE gap_count < 0 OR trainee_count < 0 OR current_gap_count < 0
UNION ALL
SELECT 'vw_followup_outcome_summary', NULL
FROM vw_followup_outcome_summary
WHERE task_count < 0 OR trainee_count < 0 OR success_count < 0
   OR no_response_count < 0 OR unreachable_count < 0
UNION ALL
SELECT 'vw_trainee_outcome_summary', trainee_id
FROM vw_trainee_outcome_summary
WHERE enrollment_count < 0 OR employment_spell_count < 0
   OR unemployment_period_count < 0 OR current_skill_gap_count < 0;

-- ===========================================================================
-- I. PHASE 18 DASHBOARD QUERY SAFETY
-- File inspection of database/phase-18-dashboard-analytical-queries.sql:
-- comments + SELECT only; no INSERT/UPDATE/DELETE/DROP/ALTER/CREATE/TRUNCATE.
-- This block records that inspection as a read-only result set.
-- ===========================================================================

SELECT
  'phase-18-dashboard-analytical-queries.sql' AS artifact,
  'INSPECTED_READ_ONLY' AS inspection_result,
  0 AS mutation_statement_count,
  'SELECT_ONLY_AGAINST_PHASE16_VIEWS' AS observed_statement_class;

-- ===========================================================================
-- J. PRIVACY / SENSITIVE DATA STRUCTURE
-- Report names only. Do not SELECT column values from identity/credential fields.
-- ===========================================================================

-- J01 Column names resembling plaintext national IDs
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'SIH26135'
  AND COLUMN_NAME REGEXP 'aadhaar|aadhar|pan_no|pan_number|national_id|passport_no|ssn|social_security';

-- J02 Credential/token columns on business tables (hash/algo flags excluded from this hit list)
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'SIH26135'
  AND COLUMN_NAME REGEXP '(^|_)(password|api_key|apikey|auth_token|access_token|refresh_token|secret_key|secret)($|_)'
  AND COLUMN_NAME NOT REGEXP '(hash|algo|algorithm|must_change)';

-- J03 Expected hashed identity/credential columns (structural confirmation only)
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'SIH26135'
  AND COLUMN_NAME IN ('password_hash', 'password_algo', 'identity_hash', 'hash_algorithm', 'content_sha256');

-- ===========================================================================
-- K. SOFT DELETE / HISTORICAL DATA SAFETY
-- ===========================================================================

-- K01 Tables with deleted_at
SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, DATA_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'SIH26135'
  AND COLUMN_NAME = 'deleted_at'
ORDER BY TABLE_NAME;

-- K02 Current spells that are soft-deleted
SELECT employment_id, is_current, deleted_at
FROM employment_records
WHERE is_current = 1
  AND deleted_at IS NOT NULL;

SELECT skill_gap_id, is_current
FROM skill_gaps
WHERE is_current = 1
  AND EXISTS (
    SELECT 1
    FROM skill_gap_assessments a
    WHERE a.skill_gap_assessment_id = skill_gaps.skill_gap_assessment_id
      AND a.trainee_id <> skill_gaps.trainee_id
  );

-- K03 CASCADE delete rules on historical/outcome tables (expect none)
SELECT
  rc.CONSTRAINT_NAME,
  rc.TABLE_NAME,
  rc.REFERENCED_TABLE_NAME,
  rc.DELETE_RULE
FROM information_schema.REFERENTIAL_CONSTRAINTS rc
WHERE rc.CONSTRAINT_SCHEMA = 'SIH26135'
  AND rc.DELETE_RULE = 'CASCADE'
  AND rc.TABLE_NAME IN (
    'trainees',
    'training_enrollments',
    'attendance_records',
    'assessments',
    'trainee_assessments',
    'assessment_results',
    'certifications',
    'placement_records',
    'placement_offers',
    'employment_records',
    'salary_history',
    'employment_exit_events',
    'trainee_unemployment_events',
    'employment_verification_requests',
    'employment_verification_attempts',
    'employment_verifications',
    'employment_verification_evidence',
    'skill_gap_assessments',
    'skill_gaps',
    'skill_gap_observations',
    'followup_tasks',
    'survey_responses',
    'communication_logs',
    'audit_logs',
    'data_quality_issues',
    'import_batches',
    'import_records'
  );

-- K04 Allowed CASCADE inventory (junction / assignment tables)
SELECT
  rc.CONSTRAINT_NAME,
  rc.TABLE_NAME,
  rc.REFERENCED_TABLE_NAME,
  rc.DELETE_RULE
FROM information_schema.REFERENTIAL_CONSTRAINTS rc
WHERE rc.CONSTRAINT_SCHEMA = 'SIH26135'
  AND rc.DELETE_RULE = 'CASCADE'
ORDER BY rc.TABLE_NAME, rc.CONSTRAINT_NAME;

-- ===========================================================================
-- L. INDEX / PERFORMANCE SANITY
-- ===========================================================================

-- L01 FK columns that are not the leftmost column of any index
SELECT
  kcu.TABLE_NAME,
  kcu.CONSTRAINT_NAME,
  kcu.COLUMN_NAME AS fk_column
FROM information_schema.KEY_COLUMN_USAGE kcu
WHERE kcu.TABLE_SCHEMA = 'SIH26135'
  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
  AND kcu.ORDINAL_POSITION = 1
  AND NOT EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = kcu.TABLE_SCHEMA
      AND s.TABLE_NAME = kcu.TABLE_NAME
      AND s.COLUMN_NAME = kcu.COLUMN_NAME
      AND s.SEQ_IN_INDEX = 1
  )
ORDER BY kcu.TABLE_NAME, kcu.CONSTRAINT_NAME;

-- L02 Major transactional tables: PK / UNIQUE / secondary index coverage
SELECT
  t.TABLE_NAME,
  SUM(CASE WHEN tc.CONSTRAINT_TYPE = 'PRIMARY KEY' THEN 1 ELSE 0 END) AS pk_count,
  SUM(CASE WHEN tc.CONSTRAINT_TYPE = 'UNIQUE' THEN 1 ELSE 0 END) AS unique_count,
  (
    SELECT COUNT(DISTINCT INDEX_NAME)
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = t.TABLE_SCHEMA
      AND s.TABLE_NAME = t.TABLE_NAME
  ) AS index_count
FROM information_schema.TABLES t
LEFT JOIN information_schema.TABLE_CONSTRAINTS tc
  ON tc.TABLE_SCHEMA = t.TABLE_SCHEMA
 AND tc.TABLE_NAME = t.TABLE_NAME
 AND tc.CONSTRAINT_TYPE IN ('PRIMARY KEY', 'UNIQUE')
WHERE t.TABLE_SCHEMA = 'SIH26135'
  AND t.TABLE_TYPE = 'BASE TABLE'
  AND t.TABLE_NAME IN (
    'trainees',
    'training_enrollments',
    'attendance_records',
    'job_applications',
    'placement_records',
    'employment_records',
    'salary_history',
    'followup_tasks',
    'survey_responses',
    'employment_verifications',
    'skill_gaps',
    'trainee_unemployment_events',
    'audit_logs',
    'import_records'
  )
GROUP BY t.TABLE_SCHEMA, t.TABLE_NAME
ORDER BY t.TABLE_NAME;

-- L03 Transactional tables lacking any secondary index besides PRIMARY
SELECT t.TABLE_NAME
FROM information_schema.TABLES t
WHERE t.TABLE_SCHEMA = 'SIH26135'
  AND t.TABLE_TYPE = 'BASE TABLE'
  AND t.TABLE_NAME IN (
    'trainees',
    'training_enrollments',
    'attendance_records',
    'placement_records',
    'employment_records',
    'salary_history',
    'survey_responses',
    'employment_verifications',
    'skill_gaps',
    'trainee_unemployment_events'
  )
  AND (
    SELECT COUNT(DISTINCT INDEX_NAME)
    FROM information_schema.STATISTICS s
    WHERE s.TABLE_SCHEMA = t.TABLE_SCHEMA
      AND s.TABLE_NAME = t.TABLE_NAME
      AND s.INDEX_NAME <> 'PRIMARY'
  ) = 0;

-- ===========================================================================
-- M. FINAL SUMMARY
-- ===========================================================================

SELECT
  (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'SIH26135' AND TABLE_TYPE = 'BASE TABLE') AS base_tables,
  (SELECT COUNT(*) FROM information_schema.VIEWS WHERE TABLE_SCHEMA = 'SIH26135') AS views,
  (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA = 'SIH26135') AS foreign_keys,
  (
    SELECT COUNT(*)
    FROM information_schema.TABLES t
    LEFT JOIN information_schema.TABLE_CONSTRAINTS c
      ON c.TABLE_SCHEMA = t.TABLE_SCHEMA
     AND c.TABLE_NAME = t.TABLE_NAME
     AND c.CONSTRAINT_TYPE = 'PRIMARY KEY'
    WHERE t.TABLE_SCHEMA = 'SIH26135'
      AND t.TABLE_TYPE = 'BASE TABLE'
      AND c.CONSTRAINT_NAME IS NULL
  ) AS primary_key_issues,
  (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = 'SIH26135'
      AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
      AND (
        NOT EXISTS (
          SELECT 1 FROM information_schema.TABLES t
          WHERE t.TABLE_SCHEMA = kcu.TABLE_SCHEMA
            AND t.TABLE_NAME = kcu.REFERENCED_TABLE_NAME
        )
        OR NOT EXISTS (
          SELECT 1 FROM information_schema.COLUMNS c
          WHERE c.TABLE_SCHEMA = kcu.TABLE_SCHEMA
            AND c.TABLE_NAME = kcu.REFERENCED_TABLE_NAME
            AND c.COLUMN_NAME = kcu.REFERENCED_COLUMN_NAME
        )
      )
  ) AS foreign_key_issues,
  (
    (SELECT COUNT(*) FROM attendance_records ar JOIN training_enrollments e ON e.enrollment_id = ar.enrollment_id WHERE ar.trainee_id <> e.trainee_id OR ar.batch_id <> e.batch_id)
    + (SELECT COUNT(*) FROM salary_history sh JOIN employment_records er ON er.employment_id = sh.employment_id WHERE sh.trainee_id <> er.trainee_id)
    + (SELECT COUNT(*) FROM skill_gaps g JOIN skill_gap_assessments a ON a.skill_gap_assessment_id = g.skill_gap_assessment_id WHERE g.trainee_id <> a.trainee_id)
  ) AS orphan_issues,
  (
    SELECT COUNT(*)
    FROM (
      SELECT username FROM users GROUP BY username HAVING COUNT(*) > 1
    ) d
  ) AS unique_constraint_issues,
  (
    SELECT COUNT(*)
    FROM information_schema.CHECK_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = 'SIH26135'
      AND (CHECK_CLAUSE IS NULL OR CHAR_LENGTH(TRIM(CHECK_CLAUSE)) = 0)
  ) AS check_constraint_issues,
  (
    (SELECT COUNT(*) FROM employment_records WHERE (end_date IS NOT NULL AND end_date < start_date) OR (is_current = 1 AND end_date IS NOT NULL) OR (starting_salary IS NOT NULL AND starting_salary < 0))
    + (SELECT COUNT(*) FROM salary_history WHERE salary_amount < 0 OR (effective_to IS NOT NULL AND effective_to < effective_from))
    + (SELECT COUNT(*) FROM placement_records WHERE (offered_salary IS NOT NULL AND offered_salary < 0) OR (joining_salary IS NOT NULL AND joining_salary < 0))
  ) AS date_amount_issues,
  (
    (SELECT COUNT(*) FROM training_enrollments e JOIN training_batches b ON b.batch_id = e.batch_id WHERE e.course_id <> b.course_id OR e.program_id <> b.program_id OR e.provider_id <> b.provider_id OR e.center_id <> b.center_id)
    + (SELECT COUNT(*) FROM survey_responses sr JOIN surveys s ON s.survey_id = sr.survey_id WHERE sr.survey_template_version_id <> s.survey_template_version_id)
  ) AS cross_domain_issues,
  (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'SIH26135'
      AND COLUMN_NAME REGEXP 'aadhaar|aadhar|pan_no|pan_number|national_id|passport_no|ssn|social_security'
  ) AS privacy_structure_issues,
  (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.TABLE_SCHEMA = 'SIH26135'
      AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
      AND kcu.ORDINAL_POSITION = 1
      AND NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS s
        WHERE s.TABLE_SCHEMA = kcu.TABLE_SCHEMA
          AND s.TABLE_NAME = kcu.TABLE_NAME
          AND s.COLUMN_NAME = kcu.COLUMN_NAME
          AND s.SEQ_IN_INDEX = 1
      )
  ) AS index_issues;
