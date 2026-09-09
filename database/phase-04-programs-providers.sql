-- Phase 04 — Programs, schemes & training providers
-- Dependencies: Phases 00–03
-- MySQL 8.0+
-- schemes and programs were created in Phase 01.
-- Training centers are created in Phase 05.

USE SIH26135;

CREATE TABLE IF NOT EXISTS ref_accreditation_status (
  accreditation_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (accreditation_status_id),
  UNIQUE KEY uk_ref_accreditation_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS training_providers (
  provider_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  provider_code VARCHAR(32) NOT NULL,
  provider_name VARCHAR(200) NOT NULL,
  registration_number VARCHAR(64) NOT NULL,
  organization_type_id BIGINT UNSIGNED NOT NULL,
  organization_id BIGINT UNSIGNED NULL,
  contact_person_name VARCHAR(150) NULL,
  contact_email VARCHAR(255) NULL,
  contact_phone VARCHAR(15) NULL,
  address_line1 VARCHAR(200) NULL,
  address_line2 VARCHAR(200) NULL,
  pincode CHAR(6) NULL,
  location_id BIGINT UNSIGNED NULL,
  state_id BIGINT UNSIGNED NOT NULL,
  district_id BIGINT UNSIGNED NOT NULL,
  accreditation_status_id BIGINT UNSIGNED NOT NULL,
  rating DECIMAL(3,2) NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (provider_id),
  UNIQUE KEY uk_training_providers_code (provider_code),
  UNIQUE KEY uk_training_providers_registration (registration_number),
  KEY idx_training_providers_district (district_id),
  KEY idx_training_providers_state (state_id),
  KEY idx_training_providers_location (location_id),
  KEY idx_training_providers_type (organization_type_id),
  KEY idx_training_providers_organization (organization_id),
  KEY idx_training_providers_accreditation (accreditation_status_id),
  KEY idx_training_providers_status (lifecycle_status_id),
  KEY idx_training_providers_district_status (district_id, lifecycle_status_id),
  CONSTRAINT fk_training_providers_org_type
    FOREIGN KEY (organization_type_id) REFERENCES ref_organization_type (organization_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_providers_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (organization_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_training_providers_location
    FOREIGN KEY (location_id) REFERENCES locations (location_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_training_providers_state
    FOREIGN KEY (state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_providers_district
    FOREIGN KEY (district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_providers_accreditation
    FOREIGN KEY (accreditation_status_id) REFERENCES ref_accreditation_status (accreditation_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_training_providers_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_training_providers_rating
    CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5)),
  CONSTRAINT ck_training_providers_pincode
    CHECK (pincode IS NULL OR pincode REGEXP '^[1-9][0-9]{5}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Empanelment history: do not overwrite; add a new row or change status on the mapping
CREATE TABLE IF NOT EXISTS program_training_providers (
  program_training_provider_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  program_id BIGINT UNSIGNED NOT NULL,
  provider_id BIGINT UNSIGNED NOT NULL,
  empanelled_from DATE NOT NULL,
  empanelled_to DATE NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (program_training_provider_id),
  UNIQUE KEY uk_program_provider_from (program_id, provider_id, empanelled_from),
  KEY idx_ptp_provider (provider_id),
  KEY idx_ptp_status (lifecycle_status_id),
  CONSTRAINT fk_ptp_program
    FOREIGN KEY (program_id) REFERENCES programs (program_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_ptp_provider
    FOREIGN KEY (provider_id) REFERENCES training_providers (provider_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_ptp_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_ptp_dates
    CHECK (empanelled_to IS NULL OR empanelled_to >= empanelled_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Idempotent add: skip if column already exists
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'SIH26135'
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'training_provider_id'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE users
     ADD COLUMN training_provider_id BIGINT UNSIGNED NULL AFTER district_id,
     ADD KEY idx_users_training_provider (training_provider_id),
     ADD CONSTRAINT fk_users_training_provider
       FOREIGN KEY (training_provider_id) REFERENCES training_providers (provider_id)
       ON UPDATE RESTRICT
       ON DELETE SET NULL',
  'SELECT ''users.training_provider_id already exists'' AS alter_skip_notice'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

START TRANSACTION;

INSERT INTO ref_accreditation_status (accreditation_status_id, status_code, status_name, sort_order) VALUES
  (1, 'NOT_APPLIED', 'Not applied', 1),
  (2, 'APPLIED', 'Applied', 2),
  (3, 'PROVISIONAL', 'Provisional', 3),
  (4, 'ACCREDITED', 'Accredited', 4),
  (5, 'SUSPENDED', 'Suspended', 5),
  (6, 'EXPIRED', 'Expired', 6),
  (7, 'REVOKED', 'Revoked', 7)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

COMMIT;
