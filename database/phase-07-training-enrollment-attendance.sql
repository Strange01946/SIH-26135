-- Phase 07 — Training enrollment & attendance
-- Dependencies: Phases 00–06
-- MySQL 8.0+
-- Multiple enrollments per trainee are allowed. Attendance is session/day level, not a single percentage.

USE SIH26135;

CREATE TABLE IF NOT EXISTS ref_enrollment_status (
  enrollment_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_terminal TINYINT(1) NOT NULL DEFAULT 0,
  is_completed_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (enrollment_status_id),
  UNIQUE KEY uk_ref_enrollment_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_training_dropout_reason (
  dropout_reason_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reason_code VARCHAR(32) NOT NULL,
  reason_name VARCHAR(150) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (dropout_reason_id),
  UNIQUE KEY uk_ref_training_dropout_reason_code (reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_attendance_status (
  attendance_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  counts_as_present TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (attendance_status_id),
  UNIQUE KEY uk_ref_attendance_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- One trainee may have many enrollments over time. Do not overwrite completed rows.
-- ON DELETE RESTRICT on trainee/batch/program: government training history must remain.
CREATE TABLE IF NOT EXISTS training_enrollments (
  enrollment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  enrollment_number VARCHAR(32) NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  program_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  provider_id BIGINT UNSIGNED NOT NULL,
  center_id BIGINT UNSIGNED NOT NULL,
  batch_id BIGINT UNSIGNED NOT NULL,
  enrollment_date DATE NOT NULL,
  start_date DATE NULL,
  expected_completion_date DATE NULL,
  actual_completion_date DATE NULL,
  enrollment_status_id BIGINT UNSIGNED NOT NULL,
  dropout_reason_id BIGINT UNSIGNED NULL,
  dropout_remarks VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (enrollment_id),
  UNIQUE KEY uk_training_enrollments_number (enrollment_number),
  UNIQUE KEY uk_training_enrollments_trainee_batch (trainee_id, batch_id),
  KEY idx_enrollments_trainee (trainee_id),
  KEY idx_enrollments_course (course_id),
  KEY idx_enrollments_provider (provider_id),
  KEY idx_enrollments_program (program_id),
  KEY idx_enrollments_center (center_id),
  KEY idx_enrollments_batch (batch_id),
  KEY idx_enrollments_status (enrollment_status_id),
  KEY idx_enrollments_dropout_reason (dropout_reason_id),
  KEY idx_enrollments_trainee_status (trainee_id, enrollment_status_id),
  KEY idx_enrollments_dates (enrollment_date, actual_completion_date),
  CONSTRAINT fk_enrollments_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_enrollments_program
    FOREIGN KEY (program_id) REFERENCES programs (program_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_enrollments_course
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_enrollments_provider
    FOREIGN KEY (provider_id) REFERENCES training_providers (provider_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_enrollments_center
    FOREIGN KEY (center_id) REFERENCES training_centers (center_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_enrollments_batch
    FOREIGN KEY (batch_id) REFERENCES training_batches (batch_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_enrollments_status
    FOREIGN KEY (enrollment_status_id) REFERENCES ref_enrollment_status (enrollment_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_enrollments_dropout_reason
    FOREIGN KEY (dropout_reason_id) REFERENCES ref_training_dropout_reason (dropout_reason_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_enrollments_start_after_enroll
    CHECK (start_date IS NULL OR start_date >= enrollment_date),
  CONSTRAINT ck_enrollments_expected_after_start
    CHECK (
      expected_completion_date IS NULL
      OR start_date IS NULL
      OR expected_completion_date >= start_date
    ),
  CONSTRAINT ck_enrollments_actual_after_start
    CHECK (
      actual_completion_date IS NULL
      OR start_date IS NULL
      OR actual_completion_date >= start_date
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Session/day-level attendance. Do not replace this with a single stored percentage.
-- ON DELETE RESTRICT: attendance history is not removed when an enrollment is retired.
CREATE TABLE IF NOT EXISTS attendance_records (
  attendance_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  enrollment_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  batch_id BIGINT UNSIGNED NOT NULL,
  session_date DATE NOT NULL,
  session_sequence SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  session_start_time TIME NULL,
  session_end_time TIME NULL,
  attendance_status_id BIGINT UNSIGNED NOT NULL,
  marked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  marked_by_user_id BIGINT UNSIGNED NULL,
  remarks VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (attendance_id),
  UNIQUE KEY uk_attendance_enrollment_session (enrollment_id, session_date, session_sequence),
  KEY idx_attendance_trainee (trainee_id),
  KEY idx_attendance_batch (batch_id),
  KEY idx_attendance_session_date (session_date),
  KEY idx_attendance_status (attendance_status_id),
  KEY idx_attendance_marked_by (marked_by_user_id),
  KEY idx_attendance_batch_date (batch_id, session_date),
  CONSTRAINT fk_attendance_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_attendance_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_attendance_batch
    FOREIGN KEY (batch_id) REFERENCES training_batches (batch_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_attendance_status
    FOREIGN KEY (attendance_status_id) REFERENCES ref_attendance_status (attendance_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_attendance_marked_by
    FOREIGN KEY (marked_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_attendance_session_sequence
    CHECK (session_sequence >= 1),
  CONSTRAINT ck_attendance_session_times
    CHECK (
      session_end_time IS NULL
      OR session_start_time IS NULL
      OR session_end_time >= session_start_time
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_enrollment_status (
  enrollment_status_id, status_code, status_name, is_terminal, is_completed_flag, sort_order
) VALUES
  (1, 'ENROLLED', 'Enrolled', 0, 0, 1),
  (2, 'IN_PROGRESS', 'In progress', 0, 0, 2),
  (3, 'COMPLETED', 'Completed', 1, 1, 3),
  (4, 'DROPPED', 'Dropped out', 1, 0, 4),
  (5, 'DEFERRED', 'Deferred', 0, 0, 5),
  (6, 'TRANSFERRED', 'Transferred', 1, 0, 6),
  (7, 'CANCELLED', 'Cancelled', 1, 0, 7)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_terminal = VALUES(is_terminal),
  is_completed_flag = VALUES(is_completed_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_training_dropout_reason (
  dropout_reason_id, reason_code, reason_name, sort_order
) VALUES
  (1, 'PERSONAL', 'Personal reasons', 1),
  (2, 'HEALTH', 'Health reasons', 2),
  (3, 'EMPLOYMENT', 'Took up employment', 3),
  (4, 'FURTHER_EDUCATION', 'Further education', 4),
  (5, 'RELOCATION', 'Relocation', 5),
  (6, 'FINANCIAL', 'Financial constraints', 6),
  (7, 'COURSE_MISMATCH', 'Course not relevant / skill mismatch', 7),
  (8, 'QUALITY_CONCERN', 'Training quality concern', 8),
  (9, 'TRANSPORTATION', 'Transportation difficulty', 9),
  (10, 'FAMILY', 'Family responsibility', 10),
  (11, 'DISCIPLINARY', 'Disciplinary / terminated', 11),
  (12, 'OTHER', 'Other', 12)
ON DUPLICATE KEY UPDATE
  reason_name = VALUES(reason_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_attendance_status (
  attendance_status_id, status_code, status_name, counts_as_present, sort_order
) VALUES
  (1, 'PRESENT', 'Present', 1, 1),
  (2, 'ABSENT', 'Absent', 0, 2),
  (3, 'LATE', 'Late', 1, 3),
  (4, 'EXCUSED', 'Excused absence', 0, 4),
  (5, 'ON_LEAVE', 'On leave', 0, 5),
  (6, 'HALF_DAY', 'Half day', 1, 6)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  counts_as_present = VALUES(counts_as_present),
  sort_order = VALUES(sort_order);

COMMIT;
