-- Phase 08 — Assessments & certifications
-- Dependencies: Phases 00–07
-- MySQL 8.0+
-- Certification is not employment. Certificate numbers are unique.
-- Scores and percentages use DECIMAL, never FLOAT.

USE SIH26135;

CREATE TABLE IF NOT EXISTS ref_assessment_type (
  assessment_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(32) NOT NULL,
  type_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (assessment_type_id),
  UNIQUE KEY uk_ref_assessment_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_assessment_outcome (
  assessment_outcome_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  outcome_code VARCHAR(32) NOT NULL,
  outcome_name VARCHAR(100) NOT NULL,
  is_pass_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (assessment_outcome_id),
  UNIQUE KEY uk_ref_assessment_outcome_code (outcome_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_certificate_status (
  certificate_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (certificate_status_id),
  UNIQUE KEY uk_ref_certificate_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_certificate_verification_status (
  certificate_verification_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (certificate_verification_status_id),
  UNIQUE KEY uk_ref_cert_verification_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Assessment event for a batch/course. Not an employment record.
CREATE TABLE IF NOT EXISTS assessments (
  assessment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  assessment_code VARCHAR(32) NOT NULL,
  assessment_name VARCHAR(200) NOT NULL,
  assessment_type_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  batch_id BIGINT UNSIGNED NOT NULL,
  program_id BIGINT UNSIGNED NOT NULL,
  assessment_date DATE NOT NULL,
  maximum_score DECIMAL(8,2) NOT NULL,
  pass_score DECIMAL(8,2) NULL,
  evaluator_user_id BIGINT UNSIGNED NULL,
  evaluator_name VARCHAR(150) NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (assessment_id),
  UNIQUE KEY uk_assessments_code (assessment_code),
  KEY idx_assessments_type (assessment_type_id),
  KEY idx_assessments_course (course_id),
  KEY idx_assessments_batch (batch_id),
  KEY idx_assessments_program (program_id),
  KEY idx_assessments_date (assessment_date),
  KEY idx_assessments_evaluator (evaluator_user_id),
  KEY idx_assessments_status (lifecycle_status_id),
  CONSTRAINT fk_assessments_type
    FOREIGN KEY (assessment_type_id) REFERENCES ref_assessment_type (assessment_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_assessments_course
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_assessments_batch
    FOREIGN KEY (batch_id) REFERENCES training_batches (batch_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_assessments_program
    FOREIGN KEY (program_id) REFERENCES programs (program_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_assessments_evaluator
    FOREIGN KEY (evaluator_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_assessments_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_assessments_maximum_score
    CHECK (maximum_score > 0),
  CONSTRAINT ck_assessments_pass_score
    CHECK (pass_score IS NULL OR (pass_score >= 0 AND pass_score <= maximum_score))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Participation / attempt. Multiple attempts per trainee per assessment are allowed.
CREATE TABLE IF NOT EXISTS trainee_assessments (
  trainee_assessment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  assessment_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  attempt_number SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  appeared_flag TINYINT(1) NOT NULL DEFAULT 1,
  assessment_date DATE NOT NULL,
  evaluator_user_id BIGINT UNSIGNED NULL,
  evaluator_name VARCHAR(150) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (trainee_assessment_id),
  UNIQUE KEY uk_trainee_assessments_attempt (assessment_id, trainee_id, attempt_number),
  KEY idx_trainee_assessments_enrollment (enrollment_id),
  KEY idx_trainee_assessments_trainee (trainee_id),
  KEY idx_trainee_assessments_date (assessment_date),
  KEY idx_trainee_assessments_evaluator (evaluator_user_id),
  CONSTRAINT fk_trainee_assessments_assessment
    FOREIGN KEY (assessment_id) REFERENCES assessments (assessment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainee_assessments_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainee_assessments_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainee_assessments_evaluator
    FOREIGN KEY (evaluator_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_trainee_assessments_attempt
    CHECK (attempt_number >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Scoring outcome. One result row per attempt. Pass/fail is not employment.
CREATE TABLE IF NOT EXISTS assessment_results (
  assessment_result_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  trainee_assessment_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  maximum_score DECIMAL(8,2) NOT NULL,
  obtained_score DECIMAL(8,2) NULL,
  score_percentage DECIMAL(5,2) NULL,
  assessment_outcome_id BIGINT UNSIGNED NOT NULL,
  result_declared_at DATETIME NULL,
  remarks VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (assessment_result_id),
  UNIQUE KEY uk_assessment_results_attempt (trainee_assessment_id),
  KEY idx_assessment_results_trainee (trainee_id),
  KEY idx_assessment_results_outcome (assessment_outcome_id),
  KEY idx_assessment_results_pass_pct (assessment_outcome_id, score_percentage),
  CONSTRAINT fk_assessment_results_attempt
    FOREIGN KEY (trainee_assessment_id) REFERENCES trainee_assessments (trainee_assessment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_assessment_results_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_assessment_results_outcome
    FOREIGN KEY (assessment_outcome_id) REFERENCES ref_assessment_outcome (assessment_outcome_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_assessment_results_maximum_score
    CHECK (maximum_score > 0),
  CONSTRAINT ck_assessment_results_obtained_score
    CHECK (obtained_score IS NULL OR (obtained_score >= 0 AND obtained_score <= maximum_score)),
  CONSTRAINT ck_assessment_results_percentage
    CHECK (score_percentage IS NULL OR (score_percentage >= 0 AND score_percentage <= 100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Issued credentials. Do not treat a certificate as placement or employment.
CREATE TABLE IF NOT EXISTS certifications (
  certification_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  certificate_number VARCHAR(64) NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  program_id BIGINT UNSIGNED NOT NULL,
  assessment_result_id BIGINT UNSIGNED NULL,
  issuing_body VARCHAR(200) NULL,
  issue_date DATE NOT NULL,
  expiry_date DATE NULL,
  certificate_status_id BIGINT UNSIGNED NOT NULL,
  certificate_verification_status_id BIGINT UNSIGNED NOT NULL,
  verified_at DATETIME NULL,
  verified_by_user_id BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (certification_id),
  UNIQUE KEY uk_certifications_certificate_number (certificate_number),
  KEY idx_certifications_trainee (trainee_id),
  KEY idx_certifications_enrollment (enrollment_id),
  KEY idx_certifications_course (course_id),
  KEY idx_certifications_program (program_id),
  KEY idx_certifications_result (assessment_result_id),
  KEY idx_certifications_status (certificate_status_id),
  KEY idx_certifications_verification (certificate_verification_status_id),
  KEY idx_certifications_issue_date (issue_date),
  KEY idx_certifications_verified_by (verified_by_user_id),
  CONSTRAINT fk_certifications_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_certifications_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_certifications_course
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_certifications_program
    FOREIGN KEY (program_id) REFERENCES programs (program_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_certifications_result
    FOREIGN KEY (assessment_result_id) REFERENCES assessment_results (assessment_result_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_certifications_status
    FOREIGN KEY (certificate_status_id) REFERENCES ref_certificate_status (certificate_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_certifications_verification
    FOREIGN KEY (certificate_verification_status_id) REFERENCES ref_certificate_verification_status (certificate_verification_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_certifications_verified_by
    FOREIGN KEY (verified_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_certifications_expiry
    CHECK (expiry_date IS NULL OR expiry_date >= issue_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_assessment_type (assessment_type_id, type_code, type_name, sort_order) VALUES
  (1, 'THEORY', 'Theory', 1),
  (2, 'PRACTICAL', 'Practical', 2),
  (3, 'VIVA', 'Viva / oral', 3),
  (4, 'PROJECT', 'Project', 4),
  (5, 'FORMATIVE', 'Formative', 5),
  (6, 'SUMMATIVE', 'Summative', 6),
  (7, 'REASSESSMENT', 'Re-assessment', 7)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_assessment_outcome (
  assessment_outcome_id, outcome_code, outcome_name, is_pass_flag, sort_order
) VALUES
  (1, 'PENDING', 'Pending', 0, 1),
  (2, 'PASS', 'Pass', 1, 2),
  (3, 'FAIL', 'Fail', 0, 3),
  (4, 'ABSENT', 'Absent', 0, 4),
  (5, 'WITHHELD', 'Withheld', 0, 5),
  (6, 'INCOMPLETE', 'Incomplete', 0, 6)
ON DUPLICATE KEY UPDATE
  outcome_name = VALUES(outcome_name),
  is_pass_flag = VALUES(is_pass_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_certificate_status (certificate_status_id, status_code, status_name, sort_order) VALUES
  (1, 'ISSUED', 'Issued', 1),
  (2, 'SUPERSEDED', 'Superseded', 2),
  (3, 'REVOKED', 'Revoked', 3),
  (4, 'EXPIRED', 'Expired', 4)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_certificate_verification_status (
  certificate_verification_status_id, status_code, status_name, sort_order
) VALUES
  (1, 'UNVERIFIED', 'Unverified', 1),
  (2, 'PENDING', 'Pending verification', 2),
  (3, 'VERIFIED', 'Verified', 3),
  (4, 'REJECTED', 'Rejected', 4)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

COMMIT;
