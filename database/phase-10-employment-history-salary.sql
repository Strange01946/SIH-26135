-- Phase 10 — Employment history & salary progression
-- Dependencies: Phases 00–09
-- MySQL 8.0+
-- Placement (Phase 09) is not employment. This phase stores actual employment spells.
-- Never overwrite a closed spell or a past salary row; insert a new spell or salary_history row.
-- Reuse ref_engagement_type, ref_salary_frequency, ref_record_verification_status,
-- employers, employer_branches, job_roles, placement_records, trainees, enrollments, geography, users.
-- DECIMAL for salary. Never FLOAT. Never store Aadhaar, passwords, or API keys.

USE SIH26135;

CREATE TABLE IF NOT EXISTS ref_employment_spell_status (
  employment_spell_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_active_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_spell_status_id),
  UNIQUE KEY uk_ref_employment_spell_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- How the employment fact entered the system (not the Phase 12 verification channel catalog).
CREATE TABLE IF NOT EXISTS ref_employment_info_source (
  employment_info_source_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_code VARCHAR(32) NOT NULL,
  source_name VARCHAR(150) NOT NULL,
  is_self_reported_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_info_source_id),
  UNIQUE KEY uk_ref_employment_info_source_code (source_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Exit / attrition lookup. Phase 14 should reuse this table rather than a second reason catalog.
CREATE TABLE IF NOT EXISTS ref_employment_exit_reason (
  employment_exit_reason_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reason_code VARCHAR(32) NOT NULL,
  reason_name VARCHAR(150) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_exit_reason_id),
  UNIQUE KEY uk_ref_employment_exit_reason_code (reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Employment spells
-- Multiple rows per trainee. employer_id is NULL for self-employment.
-- placement_id is NULL when employment did not come from a recorded placement.
-- current_spell_key: at most one current spell per trainee per engagement type
-- (MySQL allows multiple NULLs, so ended spells do not collide).
-- Date-range overlap is enforced in the application; this table prevents
-- duplicate (trainee, employer, start_date) wage spells.
-- ON DELETE RESTRICT: government outcome history must not cascade-delete.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employment_records (
  employment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  employment_number VARCHAR(32) NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NULL,
  placement_id BIGINT UNSIGNED NULL,
  employer_id BIGINT UNSIGNED NULL,
  employer_branch_id BIGINT UNSIGNED NULL,
  job_role_id BIGINT UNSIGNED NULL,
  engagement_type_id BIGINT UNSIGNED NOT NULL,
  employment_spell_status_id BIGINT UNSIGNED NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  is_current TINYINT(1) NOT NULL DEFAULT 1,
  current_spell_key TINYINT UNSIGNED GENERATED ALWAYS AS (IF(is_current = 1, 1, NULL)) STORED,
  starting_salary DECIMAL(12,2) NULL,
  salary_frequency_id BIGINT UNSIGNED NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'INR',
  work_location_id BIGINT UNSIGNED NULL,
  work_state_id BIGINT UNSIGNED NULL,
  work_district_id BIGINT UNSIGNED NULL,
  employment_info_source_id BIGINT UNSIGNED NOT NULL,
  record_verification_status_id BIGINT UNSIGNED NOT NULL,
  verified_at DATETIME NULL,
  verified_by_user_id BIGINT UNSIGNED NULL,
  employment_exit_reason_id BIGINT UNSIGNED NULL,
  exit_remarks VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (employment_id),
  UNIQUE KEY uk_employment_records_number (employment_number),
  UNIQUE KEY uk_employment_records_placement (placement_id),
  UNIQUE KEY uk_employment_records_trainee_employer_start (trainee_id, employer_id, start_date),
  UNIQUE KEY uk_employment_records_current_per_type (trainee_id, engagement_type_id, current_spell_key),
  KEY idx_employment_trainee (trainee_id),
  KEY idx_employment_employer (employer_id),
  KEY idx_employment_start_date (start_date),
  KEY idx_employment_status (employment_spell_status_id),
  KEY idx_employment_trainee_status (trainee_id, employment_spell_status_id),
  KEY idx_employment_enrollment (enrollment_id),
  KEY idx_employment_job_role (job_role_id),
  KEY idx_employment_engagement (engagement_type_id),
  KEY idx_employment_district (work_district_id),
  KEY idx_employment_state (work_state_id),
  KEY idx_employment_verification (record_verification_status_id),
  KEY idx_employment_source (employment_info_source_id),
  KEY idx_employment_branch (employer_branch_id),
  KEY idx_employment_dates (trainee_id, start_date, end_date),
  KEY idx_employment_verified_by (verified_by_user_id),
  KEY idx_employment_exit_reason (employment_exit_reason_id),
  KEY idx_employment_salary_frequency (salary_frequency_id),
  KEY idx_employment_location (work_location_id),
  CONSTRAINT fk_employment_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employment_placement
    FOREIGN KEY (placement_id) REFERENCES placement_records (placement_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employment_employer
    FOREIGN KEY (employer_id) REFERENCES employers (employer_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_branch
    FOREIGN KEY (employer_branch_id) REFERENCES employer_branches (employer_branch_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employment_job_role
    FOREIGN KEY (job_role_id) REFERENCES job_roles (job_role_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employment_engagement
    FOREIGN KEY (engagement_type_id) REFERENCES ref_engagement_type (engagement_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_spell_status
    FOREIGN KEY (employment_spell_status_id) REFERENCES ref_employment_spell_status (employment_spell_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_salary_frequency
    FOREIGN KEY (salary_frequency_id) REFERENCES ref_salary_frequency (salary_frequency_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employment_location
    FOREIGN KEY (work_location_id) REFERENCES locations (location_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employment_state
    FOREIGN KEY (work_state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employment_district
    FOREIGN KEY (work_district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employment_info_source
    FOREIGN KEY (employment_info_source_id) REFERENCES ref_employment_info_source (employment_info_source_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_verification
    FOREIGN KEY (record_verification_status_id) REFERENCES ref_record_verification_status (record_verification_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_verified_by
    FOREIGN KEY (verified_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employment_exit_reason
    FOREIGN KEY (employment_exit_reason_id) REFERENCES ref_employment_exit_reason (employment_exit_reason_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_employment_dates
    CHECK (end_date IS NULL OR end_date >= start_date),
  CONSTRAINT ck_employment_starting_salary
    CHECK (starting_salary IS NULL OR starting_salary >= 0),
  CONSTRAINT ck_employment_current_end
    CHECK (is_current = 0 OR end_date IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Salary progression
-- Insert a new row for each revision. Do not UPDATE salary_amount on an old row.
-- effective_to NULL means the amount is still in force.
-- observation_month_offset 0/6/12/24/36 supports retention wage snapshots;
-- NULL means an ad-hoc revision between those milestones.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS salary_history (
  salary_history_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  employment_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  salary_amount DECIMAL(12,2) NOT NULL,
  salary_frequency_id BIGINT UNSIGNED NOT NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'INR',
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  observation_month_offset SMALLINT UNSIGNED NULL,
  employment_info_source_id BIGINT UNSIGNED NOT NULL,
  record_verification_status_id BIGINT UNSIGNED NOT NULL,
  verified_at DATETIME NULL,
  verified_by_user_id BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (salary_history_id),
  UNIQUE KEY uk_salary_history_employment_from (employment_id, effective_from),
  UNIQUE KEY uk_salary_history_employment_milestone (employment_id, observation_month_offset),
  KEY idx_salary_history_trainee (trainee_id),
  KEY idx_salary_history_effective (effective_from, effective_to),
  KEY idx_salary_history_trainee_effective (trainee_id, effective_from),
  KEY idx_salary_history_source (employment_info_source_id),
  KEY idx_salary_history_verification (record_verification_status_id),
  KEY idx_salary_history_frequency (salary_frequency_id),
  KEY idx_salary_history_verified_by (verified_by_user_id),
  CONSTRAINT fk_salary_history_employment
    FOREIGN KEY (employment_id) REFERENCES employment_records (employment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_salary_history_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_salary_history_frequency
    FOREIGN KEY (salary_frequency_id) REFERENCES ref_salary_frequency (salary_frequency_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_salary_history_source
    FOREIGN KEY (employment_info_source_id) REFERENCES ref_employment_info_source (employment_info_source_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_salary_history_verification
    FOREIGN KEY (record_verification_status_id) REFERENCES ref_record_verification_status (record_verification_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_salary_history_verified_by
    FOREIGN KEY (verified_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_salary_history_amount
    CHECK (salary_amount >= 0),
  CONSTRAINT ck_salary_history_dates
    CHECK (effective_to IS NULL OR effective_to >= effective_from),
  CONSTRAINT ck_salary_history_milestone
    CHECK (
      observation_month_offset IS NULL
      OR observation_month_offset IN (0, 6, 12, 24, 36)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_employment_spell_status (
  employment_spell_status_id, status_code, status_name, is_active_flag, sort_order
) VALUES
  (1, 'ACTIVE', 'Active', 1, 1),
  (2, 'ENDED', 'Ended / resigned', 0, 2),
  (3, 'TERMINATED', 'Employer terminated', 0, 3),
  (4, 'COMPLETED', 'Contract / apprenticeship completed', 0, 4),
  (5, 'ON_HOLD', 'Temporarily on hold', 1, 5)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_active_flag = VALUES(is_active_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_employment_info_source (
  employment_info_source_id, source_code, source_name, is_self_reported_flag, sort_order
) VALUES
  (1, 'PLACEMENT_CONVERSION', 'Converted from a recorded placement', 0, 1),
  (2, 'TRAINEE_DECLARATION', 'Trainee self-declaration', 1, 2),
  (3, 'EMPLOYER_CONFIRMATION', 'Employer confirmation', 0, 3),
  (4, 'SURVEY_FOLLOWUP', 'Follow-up survey', 1, 4),
  (5, 'GOVERNMENT_IMPORT', 'Government / departmental import', 0, 5),
  (6, 'DOCUMENT_VERIFICATION', 'Supporting document review', 0, 6),
  (7, 'THIRD_PARTY', 'Authorized third party', 0, 7)
ON DUPLICATE KEY UPDATE
  source_name = VALUES(source_name),
  is_self_reported_flag = VALUES(is_self_reported_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_employment_exit_reason (employment_exit_reason_id, reason_code, reason_name, sort_order) VALUES
  (1, 'LOW_SALARY', 'Low salary', 1),
  (2, 'BETTER_OPPORTUNITY', 'Better opportunity', 2),
  (3, 'RELOCATION', 'Relocation', 3),
  (4, 'WORKING_CONDITIONS', 'Poor working conditions', 4),
  (5, 'CAREER_GROWTH', 'Lack of career growth', 5),
  (6, 'EMPLOYER_TERMINATION', 'Employer termination', 6),
  (7, 'PERSONAL', 'Personal reasons', 7),
  (8, 'EDUCATION', 'Education', 8),
  (9, 'MIGRATION', 'Migration', 9),
  (10, 'CONTRACT_END', 'End of contract / apprenticeship', 10),
  (11, 'OTHER', 'Other', 11)
ON DUPLICATE KEY UPDATE
  reason_name = VALUES(reason_name),
  sort_order = VALUES(sort_order);

COMMIT;
