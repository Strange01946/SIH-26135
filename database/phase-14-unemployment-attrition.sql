-- Phase 14 — Unemployment & attrition
-- Dependencies: Phases 00–13
-- MySQL 8.0+
-- Longitudinal unemployment periods and employment separations.
-- Do not treat trainees.current_employment_status_id as history.
-- Do not duplicate employment_records. Reuse ref_employment_exit_reason (Phase 10 attrition catalog).
-- Reuse ref_employment_info_source and ref_record_verification_status (SELF_REPORTED vs VERIFIED).
-- Reuse ref_non_selection_reason only via placement_records; do not copy placement failure here.
-- ON DELETE RESTRICT on historical trainee/employment links.

USE SIH26135;

-- ---------------------------------------------------------------------------
-- Lookups
-- Attrition / leaving-a-job reasons already exist as ref_employment_exit_reason.
-- Unemployment reasons are a different catalog (why not in work, including never placed).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ref_unemployment_reason (
  unemployment_reason_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reason_code VARCHAR(32) NOT NULL,
  reason_name VARCHAR(150) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (unemployment_reason_id),
  UNIQUE KEY uk_ref_unemployment_reason_code (reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_separation_nature (
  separation_nature_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  nature_code VARCHAR(32) NOT NULL,
  nature_name VARCHAR(100) NOT NULL,
  is_voluntary_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_involuntary_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (separation_nature_id),
  UNIQUE KEY uk_ref_separation_nature_code (nature_code),
  CONSTRAINT ck_ref_separation_nature_flags
    CHECK (is_voluntary_flag + is_involuntary_flag <= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Employment separation / attrition
-- One exit event per employment spell. Do not overwrite; one row ends that spell.
-- employment_records remains the spell; this table records how/why it ended.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employment_exit_events (
  employment_exit_event_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  employment_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NULL,
  separation_date DATE NOT NULL,
  employment_exit_reason_id BIGINT UNSIGNED NOT NULL,
  separation_nature_id BIGINT UNSIGNED NOT NULL,
  employment_info_source_id BIGINT UNSIGNED NOT NULL,
  record_verification_status_id BIGINT UNSIGNED NOT NULL,
  verified_at DATETIME NULL,
  verified_by_user_id BIGINT UNSIGNED NULL,
  remarks VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_exit_event_id),
  UNIQUE KEY uk_employment_exit_events_employment (employment_id),
  KEY idx_employment_exit_events_trainee (trainee_id),
  KEY idx_employment_exit_events_enrollment (enrollment_id),
  KEY idx_employment_exit_events_date (separation_date),
  KEY idx_employment_exit_events_reason (employment_exit_reason_id),
  KEY idx_employment_exit_events_nature (separation_nature_id),
  KEY idx_employment_exit_events_source (employment_info_source_id),
  KEY idx_employment_exit_events_verification (record_verification_status_id),
  KEY idx_employment_exit_events_verified_by (verified_by_user_id),
  KEY idx_employment_exit_events_trainee_date (trainee_id, separation_date),
  CONSTRAINT fk_employment_exit_events_employment
    FOREIGN KEY (employment_id) REFERENCES employment_records (employment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_exit_events_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_exit_events_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employment_exit_events_reason
    FOREIGN KEY (employment_exit_reason_id) REFERENCES ref_employment_exit_reason (employment_exit_reason_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_exit_events_nature
    FOREIGN KEY (separation_nature_id) REFERENCES ref_separation_nature (separation_nature_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_exit_events_source
    FOREIGN KEY (employment_info_source_id) REFERENCES ref_employment_info_source (employment_info_source_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_exit_events_verification
    FOREIGN KEY (record_verification_status_id) REFERENCES ref_record_verification_status (record_verification_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employment_exit_events_verified_by
    FOREIGN KEY (verified_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Unemployment periods
-- Repeated periods are allowed (new row, new period_number).
-- current_period_key: at most one open period per trainee (NULLs do not collide).
-- preceding_employment_id: job left before this spell (NULL = never employed / after training).
-- succeeding_employment_id: re-employment that closed the spell (NULL while open).
-- placement_id: optional unsuccessful or unused placement context; not a second placement system.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS trainee_unemployment_events (
  unemployment_event_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  trainee_id BIGINT UNSIGNED NOT NULL,
  period_number SMALLINT UNSIGNED NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  is_current TINYINT(1) NOT NULL DEFAULT 1,
  current_period_key TINYINT UNSIGNED GENERATED ALWAYS AS (IF(is_current = 1, 1, NULL)) STORED,
  labour_status_id BIGINT UNSIGNED NOT NULL,
  unemployment_reason_id BIGINT UNSIGNED NOT NULL,
  preceding_employment_id BIGINT UNSIGNED NULL,
  employment_exit_event_id BIGINT UNSIGNED NULL,
  succeeding_employment_id BIGINT UNSIGNED NULL,
  enrollment_id BIGINT UNSIGNED NULL,
  placement_id BIGINT UNSIGNED NULL,
  followup_task_id BIGINT UNSIGNED NULL,
  survey_response_id BIGINT UNSIGNED NULL,
  employment_info_source_id BIGINT UNSIGNED NOT NULL,
  record_verification_status_id BIGINT UNSIGNED NOT NULL,
  verified_at DATETIME NULL,
  verified_by_user_id BIGINT UNSIGNED NULL,
  remarks VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (unemployment_event_id),
  UNIQUE KEY uk_unemployment_events_trainee_period (trainee_id, period_number),
  UNIQUE KEY uk_unemployment_events_trainee_start (trainee_id, start_date),
  UNIQUE KEY uk_unemployment_events_current (trainee_id, current_period_key),
  UNIQUE KEY uk_unemployment_events_exit (employment_exit_event_id),
  KEY idx_unemployment_events_trainee (trainee_id),
  KEY idx_unemployment_events_dates (start_date, end_date),
  KEY idx_unemployment_events_reason (unemployment_reason_id),
  KEY idx_unemployment_events_labour (labour_status_id),
  KEY idx_unemployment_events_preceding (preceding_employment_id),
  KEY idx_unemployment_events_succeeding (succeeding_employment_id),
  KEY idx_unemployment_events_enrollment (enrollment_id),
  KEY idx_unemployment_events_placement (placement_id),
  KEY idx_unemployment_events_followup (followup_task_id),
  KEY idx_unemployment_events_survey (survey_response_id),
  KEY idx_unemployment_events_source (employment_info_source_id),
  KEY idx_unemployment_events_verification (record_verification_status_id),
  KEY idx_unemployment_events_verified_by (verified_by_user_id),
  KEY idx_unemployment_events_trainee_current (trainee_id, is_current),
  CONSTRAINT fk_unemployment_events_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_unemployment_events_labour
    FOREIGN KEY (labour_status_id) REFERENCES ref_employment_status (employment_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_unemployment_events_reason
    FOREIGN KEY (unemployment_reason_id) REFERENCES ref_unemployment_reason (unemployment_reason_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_unemployment_events_preceding
    FOREIGN KEY (preceding_employment_id) REFERENCES employment_records (employment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_unemployment_events_exit
    FOREIGN KEY (employment_exit_event_id) REFERENCES employment_exit_events (employment_exit_event_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_unemployment_events_succeeding
    FOREIGN KEY (succeeding_employment_id) REFERENCES employment_records (employment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_unemployment_events_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_unemployment_events_placement
    FOREIGN KEY (placement_id) REFERENCES placement_records (placement_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_unemployment_events_followup
    FOREIGN KEY (followup_task_id) REFERENCES followup_tasks (followup_task_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_unemployment_events_survey
    FOREIGN KEY (survey_response_id) REFERENCES survey_responses (survey_response_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_unemployment_events_source
    FOREIGN KEY (employment_info_source_id) REFERENCES ref_employment_info_source (employment_info_source_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_unemployment_events_verification
    FOREIGN KEY (record_verification_status_id) REFERENCES ref_record_verification_status (record_verification_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_unemployment_events_verified_by
    FOREIGN KEY (verified_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_unemployment_events_period_number
    CHECK (period_number >= 1),
  CONSTRAINT ck_unemployment_events_dates
    CHECK (end_date IS NULL OR end_date >= start_date),
  CONSTRAINT ck_unemployment_events_current_end
    CHECK (is_current = 0 OR end_date IS NULL),
  CONSTRAINT ck_unemployment_events_distinct_jobs
    CHECK (
      preceding_employment_id IS NULL
      OR succeeding_employment_id IS NULL
      OR preceding_employment_id <> succeeding_employment_id
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Reference codes only. No trainee/demo rows.
START TRANSACTION;

INSERT INTO ref_unemployment_reason (unemployment_reason_id, reason_code, reason_name, sort_order) VALUES
  (1, 'INSUFFICIENT_SKILLS', 'Insufficient skills', 1),
  (2, 'SKILL_MISMATCH', 'Skill mismatch', 2),
  (3, 'LACK_OF_JOBS', 'Lack of jobs', 3),
  (4, 'LACK_OF_EXPERIENCE', 'Lack of experience', 4),
  (5, 'SALARY_EXPECTATIONS', 'Salary expectations', 5),
  (6, 'RELOCATION', 'Relocation', 6),
  (7, 'TRANSPORTATION', 'Transportation', 7),
  (8, 'PERSONAL', 'Personal reasons', 8),
  (9, 'FURTHER_EDUCATION', 'Further education', 9),
  (10, 'SEASONAL', 'Seasonal employment ended', 10),
  (11, 'HEALTH', 'Health reasons', 11),
  (12, 'FAMILY', 'Family responsibility', 12),
  (13, 'NEVER_PLACED', 'Did not obtain employment after training', 13),
  (14, 'OTHER', 'Other', 14)
ON DUPLICATE KEY UPDATE
  reason_name = VALUES(reason_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_separation_nature (
  separation_nature_id, nature_code, nature_name, is_voluntary_flag, is_involuntary_flag, sort_order
) VALUES
  (1, 'VOLUNTARY', 'Voluntary resignation / quit', 1, 0, 1),
  (2, 'INVOLUNTARY', 'Involuntary / employer initiated', 0, 1, 2),
  (3, 'CONTRACT_END', 'Contract, internship, or apprenticeship ended', 0, 0, 3),
  (4, 'MUTUAL', 'Mutual agreement', 0, 0, 4),
  (5, 'OTHER', 'Other / unspecified', 0, 0, 5)
ON DUPLICATE KEY UPDATE
  nature_name = VALUES(nature_name),
  is_voluntary_flag = VALUES(is_voluntary_flag),
  is_involuntary_flag = VALUES(is_involuntary_flag),
  sort_order = VALUES(sort_order);

COMMIT;
