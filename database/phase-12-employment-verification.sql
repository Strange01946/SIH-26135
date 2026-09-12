-- Phase 12 — Employment verification
-- Dependencies: Phases 00–11
-- MySQL 8.0+
-- Employment spells remain in employment_records (Phase 10). Placement remains in
-- placement_records (Phase 09). This phase stores verification process, attempts,
-- outcomes, and evidence metadata. Do not treat a placement JOINED row as verified
-- employment. Reuse ref_record_verification_status and ref_employment_info_source.
-- Never store Aadhaar, passwords, API keys, auth tokens, or document file bytes.
-- MySQL DDL may implicitly commit; the DML seed block is the only explicit transaction.

USE SIH26135;

-- ---------------------------------------------------------------------------
-- Lookups
-- Reuse existing: ref_record_verification_status (UNVERIFIED / SELF_REPORTED /
-- PENDING / VERIFIED / REJECTED), ref_employment_info_source, employment_records,
-- placement_records, employers, trainees, users, followup_tasks, survey_responses,
-- communication_logs.
-- Do not reuse ref_certificate_verification_status (certificate-specific).
-- Do not reuse ref_employment_info_source as the verification method catalog
-- (source of the employment fact vs how it was later verified).
-- ---------------------------------------------------------------------------

-- How a verification was performed (channel / method). Not employment info source.
CREATE TABLE IF NOT EXISTS ref_employment_verification_method (
  employment_verification_method_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  method_code VARCHAR(32) NOT NULL,
  method_name VARCHAR(150) NOT NULL,
  is_employer_side TINYINT(1) NOT NULL DEFAULT 0,
  is_trainee_self_reported TINYINT(1) NOT NULL DEFAULT 0,
  is_official_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_verification_method_id),
  UNIQUE KEY uk_ref_emp_verif_method_code (method_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Request workflow. Outcome codes stay on ref_record_verification_status.
CREATE TABLE IF NOT EXISTS ref_employment_verification_request_status (
  employment_verification_request_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_open_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_completed_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_verification_request_status_id),
  UNIQUE KEY uk_ref_emp_verif_request_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_employment_verification_attempt_status (
  employment_verification_attempt_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_terminal_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_success_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_verification_attempt_status_id),
  UNIQUE KEY uk_ref_emp_verif_attempt_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_employment_verification_rejection_reason (
  employment_verification_rejection_reason_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reason_code VARCHAR(32) NOT NULL,
  reason_name VARCHAR(150) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_verification_rejection_reason_id),
  UNIQUE KEY uk_ref_emp_verif_rejection_reason_code (reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_employment_verification_evidence_type (
  employment_verification_evidence_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(32) NOT NULL,
  type_name VARCHAR(150) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_verification_evidence_type_id),
  UNIQUE KEY uk_ref_emp_verif_evidence_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Verification requests
-- One open request per employment spell (open_request_key). Re-verification is a
-- new request with a higher cycle_number. Do not overwrite a closed request.
-- Status ids 1 and 2 are REQUESTED / IN_PROGRESS in the seed below.
-- ON DELETE RESTRICT: historical requests must not cascade-delete.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employment_verification_requests (
  employment_verification_request_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  request_number VARCHAR(32) NOT NULL,
  employment_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  placement_id BIGINT UNSIGNED NULL,
  employer_id BIGINT UNSIGNED NULL,
  cycle_number SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  is_reverification TINYINT(1) NOT NULL DEFAULT 0,
  employment_verification_request_status_id BIGINT UNSIGNED NOT NULL,
  preferred_verification_method_id BIGINT UNSIGNED NULL,
  employment_info_source_id BIGINT UNSIGNED NULL,
  requested_by_user_id BIGINT UNSIGNED NULL,
  assigned_verifier_user_id BIGINT UNSIGNED NULL,
  followup_task_id BIGINT UNSIGNED NULL,
  survey_response_id BIGINT UNSIGNED NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  due_at DATETIME NULL,
  first_attempt_at DATETIME NULL,
  completed_at DATETIME NULL,
  open_request_key TINYINT UNSIGNED GENERATED ALWAYS AS (
    CASE
      WHEN employment_verification_request_status_id IN (1, 2) THEN 1
      ELSE NULL
    END
  ) STORED,
  remarks VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_verification_request_id),
  UNIQUE KEY uk_emp_verif_requests_number (request_number),
  UNIQUE KEY uk_emp_verif_requests_cycle (employment_id, cycle_number),
  UNIQUE KEY uk_emp_verif_requests_open (employment_id, open_request_key),
  KEY idx_emp_verif_requests_trainee (trainee_id),
  KEY idx_emp_verif_requests_placement (placement_id),
  KEY idx_emp_verif_requests_employer (employer_id),
  KEY idx_emp_verif_requests_status (employment_verification_request_status_id),
  KEY idx_emp_verif_requests_method (preferred_verification_method_id),
  KEY idx_emp_verif_requests_source (employment_info_source_id),
  KEY idx_emp_verif_requests_requested_by (requested_by_user_id),
  KEY idx_emp_verif_requests_assigned (assigned_verifier_user_id),
  KEY idx_emp_verif_requests_followup (followup_task_id),
  KEY idx_emp_verif_requests_survey (survey_response_id),
  KEY idx_emp_verif_requests_requested_at (requested_at),
  KEY idx_emp_verif_requests_due (due_at),
  KEY idx_emp_verif_requests_completed (completed_at),
  KEY idx_emp_verif_requests_trainee_status (trainee_id, employment_verification_request_status_id),
  CONSTRAINT fk_emp_verif_requests_employment
    FOREIGN KEY (employment_id) REFERENCES employment_records (employment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verif_requests_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verif_requests_placement
    FOREIGN KEY (placement_id) REFERENCES placement_records (placement_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verif_requests_employer
    FOREIGN KEY (employer_id) REFERENCES employers (employer_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verif_requests_status
    FOREIGN KEY (employment_verification_request_status_id)
      REFERENCES ref_employment_verification_request_status (employment_verification_request_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verif_requests_method
    FOREIGN KEY (preferred_verification_method_id)
      REFERENCES ref_employment_verification_method (employment_verification_method_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verif_requests_source
    FOREIGN KEY (employment_info_source_id) REFERENCES ref_employment_info_source (employment_info_source_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verif_requests_requested_by
    FOREIGN KEY (requested_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verif_requests_assigned
    FOREIGN KEY (assigned_verifier_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verif_requests_followup
    FOREIGN KEY (followup_task_id) REFERENCES followup_tasks (followup_task_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verif_requests_survey
    FOREIGN KEY (survey_response_id) REFERENCES survey_responses (survey_response_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_emp_verif_requests_cycle
    CHECK (cycle_number >= 1),
  CONSTRAINT ck_emp_verif_requests_reverify
    CHECK (
      (is_reverification = 0 AND cycle_number = 1)
      OR (is_reverification = 1 AND cycle_number >= 2)
    ),
  CONSTRAINT ck_emp_verif_requests_due
    CHECK (due_at IS NULL OR due_at >= requested_at),
  CONSTRAINT ck_emp_verif_requests_first_attempt
    CHECK (first_attempt_at IS NULL OR first_attempt_at >= requested_at),
  CONSTRAINT ck_emp_verif_requests_completed
    CHECK (completed_at IS NULL OR completed_at >= requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Attempt history (append-only)
-- Insert a new row per contact/document attempt. Do not update a completed attempt
-- into a later outcome; close it and insert the next attempt_number.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employment_verification_attempts (
  employment_verification_attempt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  employment_verification_request_id BIGINT UNSIGNED NOT NULL,
  attempt_number SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  employment_verification_method_id BIGINT UNSIGNED NOT NULL,
  employment_verification_attempt_status_id BIGINT UNSIGNED NOT NULL,
  attempted_by_user_id BIGINT UNSIGNED NULL,
  employer_respondent_user_id BIGINT UNSIGNED NULL,
  communication_log_id BIGINT UNSIGNED NULL,
  attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME NULL,
  notes VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_verification_attempt_id),
  UNIQUE KEY uk_emp_verif_attempts_number (employment_verification_request_id, attempt_number),
  KEY idx_emp_verif_attempts_method (employment_verification_method_id),
  KEY idx_emp_verif_attempts_status (employment_verification_attempt_status_id),
  KEY idx_emp_verif_attempts_attempted_by (attempted_by_user_id),
  KEY idx_emp_verif_attempts_employer_user (employer_respondent_user_id),
  KEY idx_emp_verif_attempts_comm (communication_log_id),
  KEY idx_emp_verif_attempts_attempted_at (attempted_at),
  CONSTRAINT fk_emp_verif_attempts_request
    FOREIGN KEY (employment_verification_request_id)
      REFERENCES employment_verification_requests (employment_verification_request_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verif_attempts_method
    FOREIGN KEY (employment_verification_method_id)
      REFERENCES ref_employment_verification_method (employment_verification_method_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verif_attempts_status
    FOREIGN KEY (employment_verification_attempt_status_id)
      REFERENCES ref_employment_verification_attempt_status (employment_verification_attempt_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verif_attempts_attempted_by
    FOREIGN KEY (attempted_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verif_attempts_employer_user
    FOREIGN KEY (employer_respondent_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verif_attempts_comm
    FOREIGN KEY (communication_log_id) REFERENCES communication_logs (communication_log_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_emp_verif_attempts_number
    CHECK (attempt_number >= 1),
  CONSTRAINT ck_emp_verif_attempts_completed
    CHECK (completed_at IS NULL OR completed_at >= attempted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Verification outcomes
-- One current outcome per employment spell (current_verification_key).
-- Re-verification inserts a new row; do not overwrite a previous outcome.
-- employment_records.record_verification_status_id remains the live snapshot
-- and should be updated by the application to match the current outcome row.
-- Application/service-layer must also enforce (not CHECK; MySQL Error 3823):
--   rejection_notes only when employment_verification_rejection_reason_id IS NOT NULL;
--   REJECTED status requires employment_verification_rejection_reason_id.
-- Those columns participate in FOREIGN KEY referential actions, so a CHECK
-- may not reference them.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employment_verifications (
  employment_verification_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  verification_number VARCHAR(32) NOT NULL,
  employment_verification_request_id BIGINT UNSIGNED NULL,
  employment_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  placement_id BIGINT UNSIGNED NULL,
  employer_id BIGINT UNSIGNED NULL,
  cycle_number SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  is_reverification TINYINT(1) NOT NULL DEFAULT 0,
  is_current TINYINT(1) NOT NULL DEFAULT 1,
  current_verification_key TINYINT UNSIGNED GENERATED ALWAYS AS (IF(is_current = 1, 1, NULL)) STORED,
  record_verification_status_id BIGINT UNSIGNED NOT NULL,
  employment_verification_method_id BIGINT UNSIGNED NOT NULL,
  employment_info_source_id BIGINT UNSIGNED NULL,
  employment_verification_rejection_reason_id BIGINT UNSIGNED NULL,
  verified_by_user_id BIGINT UNSIGNED NULL,
  employer_respondent_user_id BIGINT UNSIGNED NULL,
  trainee_attested_flag TINYINT(1) NOT NULL DEFAULT 0,
  employer_confirmed_flag TINYINT(1) NOT NULL DEFAULT 0,
  requested_at DATETIME NOT NULL,
  verified_at DATETIME NULL,
  outcome_recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  notes VARCHAR(500) NULL,
  rejection_notes VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_verification_id),
  UNIQUE KEY uk_emp_verifications_number (verification_number),
  UNIQUE KEY uk_emp_verifications_request (employment_verification_request_id),
  UNIQUE KEY uk_emp_verifications_cycle (employment_id, cycle_number),
  UNIQUE KEY uk_emp_verifications_current (employment_id, current_verification_key),
  KEY idx_emp_verifications_trainee (trainee_id),
  KEY idx_emp_verifications_placement (placement_id),
  KEY idx_emp_verifications_employer (employer_id),
  KEY idx_emp_verifications_status (record_verification_status_id),
  KEY idx_emp_verifications_method (employment_verification_method_id),
  KEY idx_emp_verifications_source (employment_info_source_id),
  KEY idx_emp_verifications_rejection (employment_verification_rejection_reason_id),
  KEY idx_emp_verifications_verified_by (verified_by_user_id),
  KEY idx_emp_verifications_employer_user (employer_respondent_user_id),
  KEY idx_emp_verifications_verified_at (verified_at),
  KEY idx_emp_verifications_requested_at (requested_at),
  KEY idx_emp_verifications_status_method (record_verification_status_id, employment_verification_method_id),
  KEY idx_emp_verifications_current_status (is_current, record_verification_status_id),
  CONSTRAINT fk_emp_verifications_request
    FOREIGN KEY (employment_verification_request_id)
      REFERENCES employment_verification_requests (employment_verification_request_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verifications_employment
    FOREIGN KEY (employment_id) REFERENCES employment_records (employment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verifications_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verifications_placement
    FOREIGN KEY (placement_id) REFERENCES placement_records (placement_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verifications_employer
    FOREIGN KEY (employer_id) REFERENCES employers (employer_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verifications_status
    FOREIGN KEY (record_verification_status_id)
      REFERENCES ref_record_verification_status (record_verification_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verifications_method
    FOREIGN KEY (employment_verification_method_id)
      REFERENCES ref_employment_verification_method (employment_verification_method_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verifications_source
    FOREIGN KEY (employment_info_source_id) REFERENCES ref_employment_info_source (employment_info_source_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verifications_rejection
    FOREIGN KEY (employment_verification_rejection_reason_id)
      REFERENCES ref_employment_verification_rejection_reason (employment_verification_rejection_reason_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verifications_verified_by
    FOREIGN KEY (verified_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verifications_employer_user
    FOREIGN KEY (employer_respondent_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_emp_verifications_cycle
    CHECK (cycle_number >= 1),
  CONSTRAINT ck_emp_verifications_reverify
    CHECK (
      (is_reverification = 0 AND cycle_number = 1)
      OR (is_reverification = 1 AND cycle_number >= 2)
    ),
  CONSTRAINT ck_emp_verifications_verified_at
    CHECK (verified_at IS NULL OR verified_at >= requested_at),
  CONSTRAINT ck_emp_verifications_outcome_at
    CHECK (outcome_recorded_at >= requested_at),
  CONSTRAINT ck_emp_verifications_rejection_notes
    CHECK (rejection_notes IS NULL OR CHAR_LENGTH(TRIM(rejection_notes)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Evidence metadata only
-- Store a reference code and optional SHA-256 of an external object. Never store
-- file bytes, signed URLs with tokens, Aadhaar, or other identity numbers.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employment_verification_evidence (
  employment_verification_evidence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  employment_verification_id BIGINT UNSIGNED NOT NULL,
  employment_verification_attempt_id BIGINT UNSIGNED NULL,
  employment_verification_evidence_type_id BIGINT UNSIGNED NOT NULL,
  document_reference_code VARCHAR(64) NOT NULL,
  content_sha256 CHAR(64) NULL,
  original_filename VARCHAR(200) NULL,
  captured_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  uploaded_by_user_id BIGINT UNSIGNED NULL,
  notes VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_verification_evidence_id),
  UNIQUE KEY uk_emp_verif_evidence_reference (employment_verification_id, document_reference_code),
  KEY idx_emp_verif_evidence_attempt (employment_verification_attempt_id),
  KEY idx_emp_verif_evidence_type (employment_verification_evidence_type_id),
  KEY idx_emp_verif_evidence_uploaded_by (uploaded_by_user_id),
  KEY idx_emp_verif_evidence_captured (captured_at),
  CONSTRAINT fk_emp_verif_evidence_verification
    FOREIGN KEY (employment_verification_id)
      REFERENCES employment_verifications (employment_verification_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verif_evidence_attempt
    FOREIGN KEY (employment_verification_attempt_id)
      REFERENCES employment_verification_attempts (employment_verification_attempt_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_emp_verif_evidence_type
    FOREIGN KEY (employment_verification_evidence_type_id)
      REFERENCES ref_employment_verification_evidence_type (employment_verification_evidence_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_emp_verif_evidence_uploaded_by
    FOREIGN KEY (uploaded_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_emp_verif_evidence_sha256
    CHECK (content_sha256 IS NULL OR content_sha256 REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_employment_verification_method (
  employment_verification_method_id, method_code, method_name,
  is_employer_side, is_trainee_self_reported, is_official_flag, sort_order
) VALUES
  (1, 'EMPLOYER_PORTAL', 'Employer portal confirmation', 1, 0, 0, 1),
  (2, 'EMPLOYER_LETTER', 'Employer letter / HR confirmation', 1, 0, 0, 2),
  (3, 'TRAINEE_SELF_ATTESTATION', 'Trainee self-attestation', 0, 1, 0, 3),
  (4, 'DOCUMENT_REVIEW', 'Supporting document review', 0, 0, 1, 4),
  (5, 'PHONE_CONFIRMATION', 'Phone confirmation', 0, 0, 1, 5),
  (6, 'EMAIL_CONFIRMATION', 'Email confirmation', 0, 0, 1, 6),
  (7, 'FIELD_VISIT', 'Field / in-person verification', 0, 0, 1, 7),
  (8, 'GOVERNMENT_DATABASE', 'Government / departmental database match', 0, 0, 1, 8),
  (9, 'SURVEY_FOLLOWUP', 'Follow-up survey declaration', 0, 1, 0, 9),
  (10, 'THIRD_PARTY', 'Authorized third-party verification', 0, 0, 0, 10)
ON DUPLICATE KEY UPDATE
  method_name = VALUES(method_name),
  is_employer_side = VALUES(is_employer_side),
  is_trainee_self_reported = VALUES(is_trainee_self_reported),
  is_official_flag = VALUES(is_official_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_employment_verification_request_status (
  employment_verification_request_status_id, status_code, status_name,
  is_open_flag, is_completed_flag, sort_order
) VALUES
  (1, 'REQUESTED', 'Requested', 1, 0, 1),
  (2, 'IN_PROGRESS', 'In progress', 1, 0, 2),
  (3, 'COMPLETED', 'Completed', 0, 1, 3),
  (4, 'CANCELLED', 'Cancelled', 0, 0, 4),
  (5, 'EXPIRED', 'Expired', 0, 0, 5),
  (6, 'NO_RESPONSE', 'No response', 0, 0, 6)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_open_flag = VALUES(is_open_flag),
  is_completed_flag = VALUES(is_completed_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_employment_verification_attempt_status (
  employment_verification_attempt_status_id, status_code, status_name,
  is_terminal_flag, is_success_flag, sort_order
) VALUES
  (1, 'INITIATED', 'Initiated', 0, 0, 1),
  (2, 'CONTACTED', 'Contacted', 0, 0, 2),
  (3, 'AWAITING_RESPONSE', 'Awaiting response', 0, 0, 3),
  (4, 'EVIDENCE_RECEIVED', 'Evidence received', 0, 0, 4),
  (5, 'COMPLETED', 'Completed', 1, 1, 5),
  (6, 'NO_RESPONSE', 'No response', 1, 0, 6),
  (7, 'FAILED', 'Failed', 1, 0, 7),
  (8, 'CANCELLED', 'Cancelled', 1, 0, 8)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_terminal_flag = VALUES(is_terminal_flag),
  is_success_flag = VALUES(is_success_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_employment_verification_rejection_reason (
  employment_verification_rejection_reason_id, reason_code, reason_name, sort_order
) VALUES
  (1, 'EMPLOYMENT_NOT_FOUND', 'Employment not found', 1),
  (2, 'EMPLOYER_DENIED', 'Employer denied the claim', 2),
  (3, 'TRAINEE_DENIED', 'Trainee denied or withdrew the claim', 3),
  (4, 'DOCUMENT_INVALID', 'Document invalid or unreadable', 4),
  (5, 'DOCUMENT_MISMATCH', 'Document does not match the employment record', 5),
  (6, 'DATES_INCONSISTENT', 'Dates or employer details inconsistent', 6),
  (7, 'INSUFFICIENT_EVIDENCE', 'Insufficient evidence', 7),
  (8, 'DUPLICATE_CLAIM', 'Duplicate or overlapping claim', 8),
  (9, 'FRAUD_SUSPECTED', 'Suspected fraudulent claim', 9),
  (10, 'OTHER', 'Other', 10)
ON DUPLICATE KEY UPDATE
  reason_name = VALUES(reason_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_employment_verification_evidence_type (
  employment_verification_evidence_type_id, type_code, type_name, sort_order
) VALUES
  (1, 'OFFER_LETTER', 'Offer letter', 1),
  (2, 'JOINING_LETTER', 'Joining / appointment letter', 2),
  (3, 'PAYSLIP', 'Payslip', 3),
  (4, 'BANK_CREDIT', 'Salary credit proof (masked reference)', 4),
  (5, 'EMPLOYER_CONFIRMATION', 'Employer confirmation letter', 5),
  (6, 'PF_OR_ESI', 'PF / ESI contribution proof', 6),
  (7, 'SELF_DECLARATION', 'Trainee self-declaration', 7),
  (8, 'THIRD_PARTY_REPORT', 'Third-party verification report', 8),
  (9, 'OTHER', 'Other supporting document', 9)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  sort_order = VALUES(sort_order);

COMMIT;
