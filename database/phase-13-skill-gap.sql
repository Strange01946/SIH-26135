-- ============================================================
-- PHASE 13: SKILL GAP
-- ============================================================
-- Dependencies: Phases 00–12
-- MySQL 8.0+
-- Do not recreate skills, skill_levels, job_roles, course_skills,
-- job_role_skills, or job_posting_skills. Reuse skill_levels.level_rank
-- for required/observed/target proficiency and gap_level_delta scoring
-- (required rank minus observed rank).
-- Do not store comma-separated skill/course/job-role IDs.
-- Do not store Aadhaar, passwords, API keys, tokens, or document bytes.
-- MySQL DDL may implicitly commit; only the DML seed uses a transaction.
-- CHECK constraints must not reference columns that participate in FOREIGN
-- KEY referential actions (MySQL Error 3823). Application/service-layer
-- must enforce: RESOLVED/CLOSED rows have resolved_on; gap_level_delta
-- equals skill_levels.level_rank(required) - level_rank(observed) when
-- both levels are set; REJECTED-style pairing does not apply here.

USE SIH26135;

-- ---------------------------------------------------------------------------
-- Lookups
-- Reuse: skill_levels, ref_skill_importance, skills, job_roles,
-- job_role_skills, course_skills, job_posting_skills, courses,
-- training_batches, training_enrollments, trainees, users,
-- trainee_assessments, assessment_results, certifications,
-- job_postings, employment_records, placement_records,
-- survey_responses, followup_tasks, employment_verifications.
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ref_skill_gap_source (
  skill_gap_source_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_code VARCHAR(32) NOT NULL,
  source_name VARCHAR(150) NOT NULL,
  is_self_reported_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_employer_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_official_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_gap_source_id),
  UNIQUE KEY uk_ref_skill_gap_source_code (source_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_skill_gap_severity (
  skill_gap_severity_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  severity_code VARCHAR(32) NOT NULL,
  severity_name VARCHAR(100) NOT NULL,
  severity_rank TINYINT UNSIGNED NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_gap_severity_id),
  UNIQUE KEY uk_ref_skill_gap_severity_code (severity_code),
  UNIQUE KEY uk_ref_skill_gap_severity_rank (severity_rank),
  CONSTRAINT ck_ref_skill_gap_severity_rank
    CHECK (severity_rank BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_skill_gap_status (
  skill_gap_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_open_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_resolved_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_gap_status_id),
  UNIQUE KEY uk_ref_skill_gap_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_skill_gap_assessment_status (
  skill_gap_assessment_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_completed_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_gap_assessment_status_id),
  UNIQUE KEY uk_ref_skill_gap_assessment_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_skill_gap_action_type (
  skill_gap_action_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  action_code VARCHAR(32) NOT NULL,
  action_name VARCHAR(150) NOT NULL,
  requires_course_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_gap_action_type_id),
  UNIQUE KEY uk_ref_skill_gap_action_type_code (action_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Skill-gap assessment event
-- One comparison context per event (optional job role / posting / course /
-- employment). Do not overwrite a completed assessment; insert a new event
-- for a later cycle. District/provider analytics join trainees and enrollments.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS skill_gap_assessments (
  skill_gap_assessment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  assessment_number VARCHAR(32) NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NULL,
  course_id BIGINT UNSIGNED NULL,
  batch_id BIGINT UNSIGNED NULL,
  job_role_id BIGINT UNSIGNED NULL,
  job_posting_id BIGINT UNSIGNED NULL,
  employment_id BIGINT UNSIGNED NULL,
  placement_id BIGINT UNSIGNED NULL,
  trainee_assessment_id BIGINT UNSIGNED NULL,
  assessment_result_id BIGINT UNSIGNED NULL,
  certification_id BIGINT UNSIGNED NULL,
  survey_response_id BIGINT UNSIGNED NULL,
  followup_task_id BIGINT UNSIGNED NULL,
  employment_verification_id BIGINT UNSIGNED NULL,
  skill_gap_source_id BIGINT UNSIGNED NOT NULL,
  skill_gap_assessment_status_id BIGINT UNSIGNED NOT NULL,
  assessed_on DATE NOT NULL,
  assessed_by_user_id BIGINT UNSIGNED NULL,
  notes VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_gap_assessment_id),
  UNIQUE KEY uk_skill_gap_assessments_number (assessment_number),
  KEY idx_sga_trainee (trainee_id),
  KEY idx_sga_enrollment (enrollment_id),
  KEY idx_sga_course (course_id),
  KEY idx_sga_batch (batch_id),
  KEY idx_sga_job_role (job_role_id),
  KEY idx_sga_job_posting (job_posting_id),
  KEY idx_sga_employment (employment_id),
  KEY idx_sga_placement (placement_id),
  KEY idx_sga_trainee_assessment (trainee_assessment_id),
  KEY idx_sga_result (assessment_result_id),
  KEY idx_sga_certification (certification_id),
  KEY idx_sga_survey (survey_response_id),
  KEY idx_sga_followup (followup_task_id),
  KEY idx_sga_verification (employment_verification_id),
  KEY idx_sga_source (skill_gap_source_id),
  KEY idx_sga_status (skill_gap_assessment_status_id),
  KEY idx_sga_assessed_on (assessed_on),
  KEY idx_sga_assessed_by (assessed_by_user_id),
  KEY idx_sga_trainee_assessed (trainee_id, assessed_on),
  CONSTRAINT fk_sga_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sga_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_course
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_batch
    FOREIGN KEY (batch_id) REFERENCES training_batches (batch_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_job_role
    FOREIGN KEY (job_role_id) REFERENCES job_roles (job_role_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_job_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings (job_posting_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_employment
    FOREIGN KEY (employment_id) REFERENCES employment_records (employment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_placement
    FOREIGN KEY (placement_id) REFERENCES placement_records (placement_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_trainee_assessment
    FOREIGN KEY (trainee_assessment_id) REFERENCES trainee_assessments (trainee_assessment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_result
    FOREIGN KEY (assessment_result_id) REFERENCES assessment_results (assessment_result_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_certification
    FOREIGN KEY (certification_id) REFERENCES certifications (certification_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_survey
    FOREIGN KEY (survey_response_id) REFERENCES survey_responses (survey_response_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_followup
    FOREIGN KEY (followup_task_id) REFERENCES followup_tasks (followup_task_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_verification
    FOREIGN KEY (employment_verification_id) REFERENCES employment_verifications (employment_verification_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sga_source
    FOREIGN KEY (skill_gap_source_id) REFERENCES ref_skill_gap_source (skill_gap_source_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sga_status
    FOREIGN KEY (skill_gap_assessment_status_id) REFERENCES ref_skill_gap_assessment_status (skill_gap_assessment_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sga_assessed_by
    FOREIGN KEY (assessed_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Individual skill-gap findings
-- One skill per assessment (uk_skill_gaps_assessment_skill).
-- At most one current finding per trainee per skill (current_gap_key).
-- Re-assessment inserts a new row and sets the previous is_current = 0.
-- gap_level_delta is required.level_rank - observed.level_rank (positive = shortage).
-- Do not duplicate job_role_skills / job_posting_skills / course_skills rows;
-- required_skill_level_id should match those catalogs when the comparison
-- target is a role, posting, or course.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS skill_gaps (
  skill_gap_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  skill_gap_number VARCHAR(32) NOT NULL,
  skill_gap_assessment_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  skill_id BIGINT UNSIGNED NOT NULL,
  observed_skill_level_id BIGINT UNSIGNED NULL,
  required_skill_level_id BIGINT UNSIGNED NOT NULL,
  target_skill_level_id BIGINT UNSIGNED NULL,
  gap_level_delta TINYINT NULL,
  skill_importance_id BIGINT UNSIGNED NULL,
  skill_gap_severity_id BIGINT UNSIGNED NOT NULL,
  skill_gap_status_id BIGINT UNSIGNED NOT NULL,
  skill_gap_source_id BIGINT UNSIGNED NOT NULL,
  is_current TINYINT(1) NOT NULL DEFAULT 1,
  current_gap_key TINYINT UNSIGNED GENERATED ALWAYS AS (IF(is_current = 1, 1, NULL)) STORED,
  identified_on DATE NOT NULL,
  resolved_on DATE NULL,
  notes VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_gap_id),
  UNIQUE KEY uk_skill_gaps_number (skill_gap_number),
  UNIQUE KEY uk_skill_gaps_assessment_skill (skill_gap_assessment_id, skill_id),
  UNIQUE KEY uk_skill_gaps_current (trainee_id, skill_id, current_gap_key),
  KEY idx_skill_gaps_trainee (trainee_id),
  KEY idx_skill_gaps_skill (skill_id),
  KEY idx_skill_gaps_status (skill_gap_status_id),
  KEY idx_skill_gaps_severity (skill_gap_severity_id),
  KEY idx_skill_gaps_source (skill_gap_source_id),
  KEY idx_skill_gaps_observed_level (observed_skill_level_id),
  KEY idx_skill_gaps_required_level (required_skill_level_id),
  KEY idx_skill_gaps_target_level (target_skill_level_id),
  KEY idx_skill_gaps_importance (skill_importance_id),
  KEY idx_skill_gaps_identified (identified_on),
  KEY idx_skill_gaps_resolved (resolved_on),
  KEY idx_skill_gaps_delta (gap_level_delta),
  KEY idx_skill_gaps_current_status (is_current, skill_gap_status_id),
  KEY idx_skill_gaps_skill_current (skill_id, is_current, skill_gap_severity_id),
  CONSTRAINT fk_skill_gaps_assessment
    FOREIGN KEY (skill_gap_assessment_id) REFERENCES skill_gap_assessments (skill_gap_assessment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_skill_gaps_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_skill_gaps_skill
    FOREIGN KEY (skill_id) REFERENCES skills (skill_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_skill_gaps_observed_level
    FOREIGN KEY (observed_skill_level_id) REFERENCES skill_levels (skill_level_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_skill_gaps_required_level
    FOREIGN KEY (required_skill_level_id) REFERENCES skill_levels (skill_level_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_skill_gaps_target_level
    FOREIGN KEY (target_skill_level_id) REFERENCES skill_levels (skill_level_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_skill_gaps_importance
    FOREIGN KEY (skill_importance_id) REFERENCES ref_skill_importance (skill_importance_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_skill_gaps_severity
    FOREIGN KEY (skill_gap_severity_id) REFERENCES ref_skill_gap_severity (skill_gap_severity_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_skill_gaps_status
    FOREIGN KEY (skill_gap_status_id) REFERENCES ref_skill_gap_status (skill_gap_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_skill_gaps_source
    FOREIGN KEY (skill_gap_source_id) REFERENCES ref_skill_gap_source (skill_gap_source_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_skill_gaps_delta
    CHECK (gap_level_delta IS NULL OR gap_level_delta BETWEEN -4 AND 4),
  CONSTRAINT ck_skill_gaps_dates
    CHECK (resolved_on IS NULL OR resolved_on >= identified_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Append-only proficiency / status observations
-- Insert a new observation_number when levels or status change.
-- Do not overwrite a previous observation row.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS skill_gap_observations (
  skill_gap_observation_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  skill_gap_id BIGINT UNSIGNED NOT NULL,
  observation_number SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  observed_skill_level_id BIGINT UNSIGNED NULL,
  required_skill_level_id BIGINT UNSIGNED NOT NULL,
  target_skill_level_id BIGINT UNSIGNED NULL,
  gap_level_delta TINYINT NULL,
  skill_gap_severity_id BIGINT UNSIGNED NOT NULL,
  skill_gap_status_id BIGINT UNSIGNED NOT NULL,
  skill_gap_source_id BIGINT UNSIGNED NOT NULL,
  observed_on DATE NOT NULL,
  observed_by_user_id BIGINT UNSIGNED NULL,
  notes VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_gap_observation_id),
  UNIQUE KEY uk_skill_gap_observations_number (skill_gap_id, observation_number),
  KEY idx_sgo_observed_level (observed_skill_level_id),
  KEY idx_sgo_required_level (required_skill_level_id),
  KEY idx_sgo_target_level (target_skill_level_id),
  KEY idx_sgo_severity (skill_gap_severity_id),
  KEY idx_sgo_status (skill_gap_status_id),
  KEY idx_sgo_source (skill_gap_source_id),
  KEY idx_sgo_observed_on (observed_on),
  KEY idx_sgo_observed_by (observed_by_user_id),
  CONSTRAINT fk_sgo_skill_gap
    FOREIGN KEY (skill_gap_id) REFERENCES skill_gaps (skill_gap_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sgo_observed_level
    FOREIGN KEY (observed_skill_level_id) REFERENCES skill_levels (skill_level_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sgo_required_level
    FOREIGN KEY (required_skill_level_id) REFERENCES skill_levels (skill_level_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sgo_target_level
    FOREIGN KEY (target_skill_level_id) REFERENCES skill_levels (skill_level_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sgo_severity
    FOREIGN KEY (skill_gap_severity_id) REFERENCES ref_skill_gap_severity (skill_gap_severity_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sgo_status
    FOREIGN KEY (skill_gap_status_id) REFERENCES ref_skill_gap_status (skill_gap_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sgo_source
    FOREIGN KEY (skill_gap_source_id) REFERENCES ref_skill_gap_source (skill_gap_source_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sgo_observed_by
    FOREIGN KEY (observed_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_sgo_number
    CHECK (observation_number >= 1),
  CONSTRAINT ck_sgo_delta
    CHECK (gap_level_delta IS NULL OR gap_level_delta BETWEEN -4 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Remediation recommendations
-- Reference existing courses. Do not duplicate the course/program catalog.
-- Multiple recommendations per gap are allowed; one row per gap+course+action.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS skill_gap_recommendations (
  skill_gap_recommendation_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  skill_gap_id BIGINT UNSIGNED NOT NULL,
  recommendation_number SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  skill_gap_action_type_id BIGINT UNSIGNED NOT NULL,
  recommended_course_id BIGINT UNSIGNED NULL,
  recommended_skill_id BIGINT UNSIGNED NULL,
  is_accepted_flag TINYINT(1) NOT NULL DEFAULT 0,
  accepted_on DATE NULL,
  remarks VARCHAR(500) NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_gap_recommendation_id),
  UNIQUE KEY uk_sgr_number (skill_gap_id, recommendation_number),
  UNIQUE KEY uk_sgr_gap_action_course (skill_gap_id, skill_gap_action_type_id, recommended_course_id),
  KEY idx_sgr_action (skill_gap_action_type_id),
  KEY idx_sgr_course (recommended_course_id),
  KEY idx_sgr_skill (recommended_skill_id),
  KEY idx_sgr_created_by (created_by_user_id),
  CONSTRAINT fk_sgr_skill_gap
    FOREIGN KEY (skill_gap_id) REFERENCES skill_gaps (skill_gap_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sgr_action
    FOREIGN KEY (skill_gap_action_type_id) REFERENCES ref_skill_gap_action_type (skill_gap_action_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sgr_course
    FOREIGN KEY (recommended_course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_sgr_skill
    FOREIGN KEY (recommended_skill_id) REFERENCES skills (skill_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_sgr_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_sgr_number
    CHECK (recommendation_number >= 1),
  CONSTRAINT ck_sgr_accepted_on
    CHECK (accepted_on IS NULL OR is_accepted_flag = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_skill_gap_source (
  skill_gap_source_id, source_code, source_name,
  is_self_reported_flag, is_employer_flag, is_official_flag, sort_order
) VALUES
  (1, 'ASSESSMENT', 'Training assessment / result', 0, 0, 1, 1),
  (2, 'CERTIFICATION', 'Certification outcome', 0, 0, 1, 2),
  (3, 'TRAINEE_SELF_REPORT', 'Trainee self-report', 1, 0, 0, 3),
  (4, 'EMPLOYER_FEEDBACK', 'Employer feedback', 0, 1, 0, 4),
  (5, 'JOB_ROLE_COMPARISON', 'Comparison to job-role skill requirements', 0, 0, 1, 5),
  (6, 'JOB_POSTING_COMPARISON', 'Comparison to job-posting skill requirements', 0, 0, 1, 6),
  (7, 'SURVEY_FOLLOWUP', 'Follow-up survey', 1, 0, 0, 7),
  (8, 'EMPLOYMENT_VERIFICATION', 'Employment verification outcome', 0, 0, 1, 8),
  (9, 'COURSE_COMPARISON', 'Comparison to course taught-skill levels', 0, 0, 1, 9),
  (10, 'MANUAL_REVIEW', 'Administrator / officer manual review', 0, 0, 1, 10)
ON DUPLICATE KEY UPDATE
  source_name = VALUES(source_name),
  is_self_reported_flag = VALUES(is_self_reported_flag),
  is_employer_flag = VALUES(is_employer_flag),
  is_official_flag = VALUES(is_official_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_skill_gap_severity (
  skill_gap_severity_id, severity_code, severity_name, severity_rank, sort_order
) VALUES
  (1, 'NONE', 'No gap / meets requirement', 1, 1),
  (2, 'LOW', 'Low', 2, 2),
  (3, 'MODERATE', 'Moderate', 3, 3),
  (4, 'HIGH', 'High', 4, 4),
  (5, 'CRITICAL', 'Critical', 5, 5)
ON DUPLICATE KEY UPDATE
  severity_name = VALUES(severity_name),
  severity_rank = VALUES(severity_rank),
  sort_order = VALUES(sort_order);

INSERT INTO ref_skill_gap_status (
  skill_gap_status_id, status_code, status_name, is_open_flag, is_resolved_flag, sort_order
) VALUES
  (1, 'OPEN', 'Open', 1, 0, 1),
  (2, 'IN_REMEDIATION', 'In remediation', 1, 0, 2),
  (3, 'RESOLVED', 'Resolved', 0, 1, 3),
  (4, 'CLOSED_UNRESOLVED', 'Closed unresolved', 0, 0, 4),
  (5, 'SUPERSEDED', 'Superseded by a later assessment', 0, 0, 5)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_open_flag = VALUES(is_open_flag),
  is_resolved_flag = VALUES(is_resolved_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_skill_gap_assessment_status (
  skill_gap_assessment_status_id, status_code, status_name, is_completed_flag, sort_order
) VALUES
  (1, 'DRAFT', 'Draft', 0, 1),
  (2, 'COMPLETED', 'Completed', 1, 2),
  (3, 'CANCELLED', 'Cancelled', 0, 3)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_completed_flag = VALUES(is_completed_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_skill_gap_action_type (
  skill_gap_action_type_id, action_code, action_name, requires_course_flag, sort_order
) VALUES
  (1, 'RECOMMEND_COURSE', 'Recommend an existing course', 1, 1),
  (2, 'REASSESSMENT', 'Re-assessment of the skill', 0, 2),
  (3, 'ON_THE_JOB', 'On-the-job practice / mentoring', 0, 3),
  (4, 'COUNSELLING', 'Career / training counselling', 0, 4),
  (5, 'SELF_LEARNING', 'Self-learning plan', 0, 5),
  (6, 'OTHER', 'Other action', 0, 6)
ON DUPLICATE KEY UPDATE
  action_name = VALUES(action_name),
  requires_course_flag = VALUES(requires_course_flag),
  sort_order = VALUES(sort_order);

COMMIT;
