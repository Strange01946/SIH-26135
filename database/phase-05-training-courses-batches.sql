-- Phase 05 — Training centers, courses & batches
-- Dependencies: Phases 00–04
-- MySQL 8.0+
-- Certification and employment are not implied by a course or batch row.

USE SIH26135;

CREATE TABLE IF NOT EXISTS sectors (
  sector_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sector_code VARCHAR(32) NOT NULL,
  sector_name VARCHAR(150) NOT NULL,
  description VARCHAR(500) NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (sector_id),
  UNIQUE KEY uk_sectors_code (sector_code),
  UNIQUE KEY uk_sectors_name (sector_name),
  KEY idx_sectors_status (lifecycle_status_id),
  CONSTRAINT fk_sectors_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS industries (
  industry_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  industry_code VARCHAR(32) NOT NULL,
  industry_name VARCHAR(150) NOT NULL,
  sector_id BIGINT UNSIGNED NULL,
  description VARCHAR(500) NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (industry_id),
  UNIQUE KEY uk_industries_code (industry_code),
  UNIQUE KEY uk_industries_name (industry_name),
  KEY idx_industries_sector (sector_id),
  KEY idx_industries_status (lifecycle_status_id),
  CONSTRAINT fk_industries_sector
    FOREIGN KEY (sector_id) REFERENCES sectors (sector_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_industries_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_qualification_level (
  qualification_level_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  level_code VARCHAR(32) NOT NULL,
  level_name VARCHAR(100) NOT NULL,
  nsqf_level TINYINT UNSIGNED NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (qualification_level_id),
  UNIQUE KEY uk_ref_qualification_level_code (level_code),
  CONSTRAINT ck_ref_qualification_nsqf
    CHECK (nsqf_level IS NULL OR (nsqf_level >= 1 AND nsqf_level <= 8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_delivery_mode (
  delivery_mode_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  mode_code VARCHAR(32) NOT NULL,
  mode_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (delivery_mode_id),
  UNIQUE KEY uk_ref_delivery_mode_code (mode_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_batch_status (
  batch_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (batch_status_id),
  UNIQUE KEY uk_ref_batch_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS training_centers (
  center_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  provider_id BIGINT UNSIGNED NOT NULL,
  center_code VARCHAR(32) NOT NULL,
  center_name VARCHAR(200) NOT NULL,
  address_line1 VARCHAR(200) NULL,
  address_line2 VARCHAR(200) NULL,
  pincode CHAR(6) NULL,
  location_id BIGINT UNSIGNED NULL,
  state_id BIGINT UNSIGNED NOT NULL,
  district_id BIGINT UNSIGNED NOT NULL,
  operational_capacity INT UNSIGNED NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (center_id),
  UNIQUE KEY uk_training_centers_code (center_code),
  UNIQUE KEY uk_training_centers_provider_code (provider_id, center_code),
  KEY idx_training_centers_provider (provider_id),
  KEY idx_training_centers_district (district_id),
  KEY idx_training_centers_state (state_id),
  KEY idx_training_centers_location (location_id),
  KEY idx_training_centers_status (lifecycle_status_id),
  CONSTRAINT fk_training_centers_provider
    FOREIGN KEY (provider_id) REFERENCES training_providers (provider_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_centers_location
    FOREIGN KEY (location_id) REFERENCES locations (location_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_training_centers_state
    FOREIGN KEY (state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_centers_district
    FOREIGN KEY (district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_centers_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_training_centers_capacity
    CHECK (operational_capacity IS NULL OR operational_capacity > 0),
  CONSTRAINT ck_training_centers_pincode
    CHECK (pincode IS NULL OR pincode REGEXP '^[1-9][0-9]{5}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS courses (
  course_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  course_code VARCHAR(32) NOT NULL,
  course_name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  sector_id BIGINT UNSIGNED NOT NULL,
  industry_id BIGINT UNSIGNED NULL,
  qualification_level_id BIGINT UNSIGNED NULL,
  duration_hours INT UNSIGNED NULL,
  duration_days INT UNSIGNED NULL,
  delivery_mode_id BIGINT UNSIGNED NOT NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (course_id),
  UNIQUE KEY uk_courses_code (course_code),
  KEY idx_courses_sector (sector_id),
  KEY idx_courses_industry (industry_id),
  KEY idx_courses_qualification (qualification_level_id),
  KEY idx_courses_delivery_mode (delivery_mode_id),
  KEY idx_courses_status (lifecycle_status_id),
  CONSTRAINT fk_courses_sector
    FOREIGN KEY (sector_id) REFERENCES sectors (sector_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_courses_industry
    FOREIGN KEY (industry_id) REFERENCES industries (industry_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_courses_qualification
    FOREIGN KEY (qualification_level_id) REFERENCES ref_qualification_level (qualification_level_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_courses_delivery_mode
    FOREIGN KEY (delivery_mode_id) REFERENCES ref_delivery_mode (delivery_mode_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_courses_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_courses_duration_hours
    CHECK (duration_hours IS NULL OR duration_hours > 0),
  CONSTRAINT ck_courses_duration_days
    CHECK (duration_days IS NULL OR duration_days > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A course may be offered under multiple programs without duplicating the catalog row
CREATE TABLE IF NOT EXISTS course_programs (
  course_id BIGINT UNSIGNED NOT NULL,
  program_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (course_id, program_id),
  KEY idx_course_programs_program (program_id),
  CONSTRAINT fk_course_programs_course
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  CONSTRAINT fk_course_programs_program
    FOREIGN KEY (program_id) REFERENCES programs (program_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS training_batches (
  batch_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_code VARCHAR(32) NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  provider_id BIGINT UNSIGNED NOT NULL,
  center_id BIGINT UNSIGNED NOT NULL,
  program_id BIGINT UNSIGNED NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  capacity INT UNSIGNED NOT NULL,
  batch_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (batch_id),
  UNIQUE KEY uk_training_batches_code (batch_code),
  KEY idx_training_batches_course (course_id),
  KEY idx_training_batches_provider (provider_id),
  KEY idx_training_batches_center (center_id),
  KEY idx_training_batches_program (program_id),
  KEY idx_training_batches_course_start (course_id, start_date),
  KEY idx_training_batches_status (batch_status_id),
  CONSTRAINT fk_training_batches_course
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_batches_provider
    FOREIGN KEY (provider_id) REFERENCES training_providers (provider_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_batches_center
    FOREIGN KEY (center_id) REFERENCES training_centers (center_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_batches_program
    FOREIGN KEY (program_id) REFERENCES programs (program_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_batches_status
    FOREIGN KEY (batch_status_id) REFERENCES ref_batch_status (batch_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_training_batches_capacity
    CHECK (capacity > 0),
  CONSTRAINT ck_training_batches_dates
    CHECK (end_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_qualification_level (
  qualification_level_id, level_code, level_name, nsqf_level, sort_order
) VALUES
  (1, 'NSQF_1', 'NSQF Level 1', 1, 1),
  (2, 'NSQF_2', 'NSQF Level 2', 2, 2),
  (3, 'NSQF_3', 'NSQF Level 3', 3, 3),
  (4, 'NSQF_4', 'NSQF Level 4', 4, 4),
  (5, 'NSQF_5', 'NSQF Level 5', 5, 5),
  (6, 'NSQF_6', 'NSQF Level 6', 6, 6),
  (7, 'NSQF_7', 'NSQF Level 7', 7, 7),
  (8, 'NSQF_8', 'NSQF Level 8', 8, 8),
  (9, 'NON_NSQF', 'Non-NSQF / other', NULL, 9)
ON DUPLICATE KEY UPDATE
  level_name = VALUES(level_name),
  nsqf_level = VALUES(nsqf_level),
  sort_order = VALUES(sort_order);

INSERT INTO ref_delivery_mode (delivery_mode_id, mode_code, mode_name, sort_order) VALUES
  (1, 'OFFLINE', 'Offline / classroom', 1),
  (2, 'ONLINE', 'Online', 2),
  (3, 'BLENDED', 'Blended', 3),
  (4, 'OJT', 'On-the-job training', 4)
ON DUPLICATE KEY UPDATE
  mode_name = VALUES(mode_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_batch_status (batch_status_id, status_code, status_name, sort_order) VALUES
  (1, 'PLANNED', 'Planned', 1),
  (2, 'OPEN', 'Open for enrollment', 2),
  (3, 'ONGOING', 'Ongoing', 3),
  (4, 'COMPLETED', 'Completed', 4),
  (5, 'CANCELLED', 'Cancelled', 5),
  (6, 'SUSPENDED', 'Suspended', 6)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

COMMIT;
