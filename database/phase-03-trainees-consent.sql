-- Phase 03 — Trainees & consent
-- Dependencies: Phases 00–02
-- MySQL 8.0+
-- Do not store Aadhaar or other national IDs in plaintext.

USE SIH26135;

CREATE TABLE IF NOT EXISTS ref_gender (
  gender_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  gender_code VARCHAR(32) NOT NULL,
  gender_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (gender_id),
  UNIQUE KEY uk_ref_gender_code (gender_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_education_level (
  education_level_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  education_code VARCHAR(32) NOT NULL,
  education_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (education_level_id),
  UNIQUE KEY uk_ref_education_level_code (education_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_employment_status (
  employment_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_employed_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employment_status_id),
  UNIQUE KEY uk_ref_employment_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_profile_status (
  profile_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (profile_status_id),
  UNIQUE KEY uk_ref_profile_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_identity_document_type (
  identity_document_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(32) NOT NULL,
  type_name VARCHAR(100) NOT NULL,
  is_highly_sensitive TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (identity_document_type_id),
  UNIQUE KEY uk_ref_identity_document_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_consent_type (
  consent_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  consent_code VARCHAR(64) NOT NULL,
  consent_name VARCHAR(150) NOT NULL,
  purpose VARCHAR(500) NOT NULL,
  allows_employment_followup TINYINT(1) NOT NULL DEFAULT 0,
  is_required TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (consent_type_id),
  UNIQUE KEY uk_ref_consent_type_code (consent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_consent_status (
  consent_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (consent_status_id),
  UNIQUE KEY uk_ref_consent_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Operational identity. Aadhaar/PAN/etc. are NOT stored here.
CREATE TABLE IF NOT EXISTS trainees (
  trainee_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  registration_number VARCHAR(32) NOT NULL,
  first_name VARCHAR(80) NOT NULL,
  middle_name VARCHAR(80) NULL,
  last_name VARCHAR(80) NULL,
  date_of_birth DATE NULL,
  gender_id BIGINT UNSIGNED NOT NULL,
  email VARCHAR(255) NULL,
  phone VARCHAR(15) NULL,
  address_line1 VARCHAR(200) NULL,
  address_line2 VARCHAR(200) NULL,
  pincode CHAR(6) NULL,
  location_id BIGINT UNSIGNED NULL,
  state_id BIGINT UNSIGNED NOT NULL,
  district_id BIGINT UNSIGNED NOT NULL,
  block_id BIGINT UNSIGNED NULL,
  education_level_id BIGINT UNSIGNED NULL,
  current_employment_status_id BIGINT UNSIGNED NOT NULL,
  profile_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (trainee_id),
  UNIQUE KEY uk_trainees_registration_number (registration_number),
  UNIQUE KEY uk_trainees_user_id (user_id),
  KEY idx_trainees_district (district_id),
  KEY idx_trainees_employment_status (current_employment_status_id),
  KEY idx_trainees_district_employment (district_id, current_employment_status_id),
  KEY idx_trainees_state (state_id),
  KEY idx_trainees_block (block_id),
  KEY idx_trainees_location (location_id),
  KEY idx_trainees_gender (gender_id),
  KEY idx_trainees_education (education_level_id),
  KEY idx_trainees_profile_status (profile_status_id),
  KEY idx_trainees_phone (phone),
  KEY idx_trainees_email (email),
  CONSTRAINT fk_trainees_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_trainees_gender
    FOREIGN KEY (gender_id) REFERENCES ref_gender (gender_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainees_location
    FOREIGN KEY (location_id) REFERENCES locations (location_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_trainees_state
    FOREIGN KEY (state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainees_district
    FOREIGN KEY (district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainees_block
    FOREIGN KEY (block_id) REFERENCES blocks (block_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_trainees_education
    FOREIGN KEY (education_level_id) REFERENCES ref_education_level (education_level_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_trainees_employment_status
    FOREIGN KEY (current_employment_status_id) REFERENCES ref_employment_status (employment_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainees_profile_status
    FOREIGN KEY (profile_status_id) REFERENCES ref_profile_status (profile_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_trainees_pincode
    CHECK (pincode IS NULL OR pincode REGEXP '^[1-9][0-9]{5}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Store only a keyed hash (HMAC-SHA-256 hex). Never store Aadhaar or other IDs in plaintext.
CREATE TABLE IF NOT EXISTS trainee_identity_hashes (
  trainee_identity_hash_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  trainee_id BIGINT UNSIGNED NOT NULL,
  identity_document_type_id BIGINT UNSIGNED NOT NULL,
  identity_hash CHAR(64) NOT NULL,
  hash_algorithm VARCHAR(32) NOT NULL DEFAULT 'HMAC-SHA256',
  verified_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (trainee_identity_hash_id),
  UNIQUE KEY uk_trainee_identity_type_hash (identity_document_type_id, identity_hash),
  UNIQUE KEY uk_trainee_identity_type_trainee (trainee_id, identity_document_type_id),
  KEY idx_trainee_identity_trainee (trainee_id),
  CONSTRAINT fk_trainee_identity_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainee_identity_type
    FOREIGN KEY (identity_document_type_id) REFERENCES ref_identity_document_type (identity_document_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_trainee_identity_hash_hex
    CHECK (identity_hash REGEXP '^[0-9a-f]{64}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS trainee_consents (
  trainee_consent_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  trainee_id BIGINT UNSIGNED NOT NULL,
  consent_type_id BIGINT UNSIGNED NOT NULL,
  purpose VARCHAR(500) NOT NULL,
  consent_status_id BIGINT UNSIGNED NOT NULL,
  policy_version VARCHAR(32) NOT NULL,
  granted_at DATETIME NULL,
  revoked_at DATETIME NULL,
  expires_at DATETIME NULL,
  captured_channel VARCHAR(32) NULL,
  captured_by_user_id BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (trainee_consent_id),
  KEY idx_trainee_consents_trainee (trainee_id),
  KEY idx_trainee_consents_type_status (consent_type_id, consent_status_id),
  KEY idx_trainee_consents_granted_at (granted_at),
  KEY idx_trainee_consents_captured_by (captured_by_user_id),
  CONSTRAINT fk_trainee_consents_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainee_consents_type
    FOREIGN KEY (consent_type_id) REFERENCES ref_consent_type (consent_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainee_consents_status
    FOREIGN KEY (consent_status_id) REFERENCES ref_consent_status (consent_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainee_consents_captured_by
    FOREIGN KEY (captured_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_trainee_consents_revoke_after_grant
    CHECK (revoked_at IS NULL OR granted_at IS NULL OR revoked_at >= granted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_gender (gender_id, gender_code, gender_name, sort_order) VALUES
  (1, 'FEMALE', 'Female', 1),
  (2, 'MALE', 'Male', 2),
  (3, 'TRANSGENDER', 'Transgender', 3),
  (4, 'OTHER', 'Other', 4),
  (5, 'PREFER_NOT_TO_SAY', 'Prefer not to say', 5)
ON DUPLICATE KEY UPDATE
  gender_name = VALUES(gender_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_education_level (education_level_id, education_code, education_name, sort_order) VALUES
  (1, 'BELOW_SECONDARY', 'Below secondary', 1),
  (2, 'SECONDARY', 'Secondary (10th)', 2),
  (3, 'HIGHER_SECONDARY', 'Higher secondary (12th)', 3),
  (4, 'ITI', 'ITI', 4),
  (5, 'DIPLOMA', 'Diploma', 5),
  (6, 'GRADUATE', 'Graduate', 6),
  (7, 'POSTGRADUATE', 'Postgraduate', 7),
  (8, 'DOCTORATE', 'Doctorate', 8),
  (9, 'OTHER', 'Other', 9)
ON DUPLICATE KEY UPDATE
  education_name = VALUES(education_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_employment_status (
  employment_status_id, status_code, status_name, is_employed_flag, sort_order
) VALUES
  (1, 'UNEMPLOYED', 'Unemployed', 0, 1),
  (2, 'IN_TRAINING', 'In training', 0, 2),
  (3, 'WAGE_EMPLOYED', 'Wage employed', 1, 3),
  (4, 'SELF_EMPLOYED', 'Self-employed', 1, 4),
  (5, 'APPRENTICE', 'Apprentice', 1, 5),
  (6, 'IN_EDUCATION', 'In further education', 0, 6),
  (7, 'NOT_IN_LABOUR_FORCE', 'Not in labour force', 0, 7),
  (8, 'UNKNOWN', 'Unknown', 0, 8)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_employed_flag = VALUES(is_employed_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_profile_status (profile_status_id, status_code, status_name, sort_order) VALUES
  (1, 'DRAFT', 'Draft', 1),
  (2, 'INCOMPLETE', 'Incomplete', 2),
  (3, 'ACTIVE', 'Active', 3),
  (4, 'SUSPENDED', 'Suspended', 4),
  (5, 'ARCHIVED', 'Archived', 5)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_identity_document_type (
  identity_document_type_id, type_code, type_name, is_highly_sensitive
) VALUES
  (1, 'AADHAAR', 'Aadhaar', 1),
  (2, 'PAN', 'PAN', 1),
  (3, 'VOTER_ID', 'Voter ID', 1),
  (4, 'DRIVING_LICENSE', 'Driving licence', 1),
  (5, 'PASSPORT', 'Passport', 1)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  is_highly_sensitive = VALUES(is_highly_sensitive);

INSERT INTO ref_consent_type (
  consent_type_id, consent_code, consent_name, purpose, allows_employment_followup, is_required
) VALUES
  (1, 'DATA_PROCESSING', 'Data processing', 'Process registration and training records', 0, 1),
  (2, 'EMPLOYMENT_FOLLOW_UP', 'Employment follow-up', 'Contact the trainee about employment outcomes over time', 1, 0),
  (3, 'EMPLOYER_SHARING', 'Share with employers', 'Share relevant profile data with prospective employers', 0, 0),
  (4, 'ANALYTICS', 'Analytics', 'Use de-identified data in outcome analytics', 0, 0),
  (5, 'COMMUNICATION_SMS', 'SMS communication', 'Send operational SMS messages', 0, 0),
  (6, 'COMMUNICATION_WHATSAPP', 'WhatsApp communication', 'Send operational WhatsApp messages', 0, 0),
  (7, 'COMMUNICATION_EMAIL', 'Email communication', 'Send operational email messages', 0, 0),
  (8, 'COMMUNICATION_PHONE', 'Phone communication', 'Contact by phone for follow-up', 1, 0)
ON DUPLICATE KEY UPDATE
  consent_name = VALUES(consent_name),
  purpose = VALUES(purpose),
  allows_employment_followup = VALUES(allows_employment_followup),
  is_required = VALUES(is_required);

INSERT INTO ref_consent_status (consent_status_id, status_code, status_name, sort_order) VALUES
  (1, 'GRANTED', 'Granted', 1),
  (2, 'REVOKED', 'Revoked', 2),
  (3, 'EXPIRED', 'Expired', 3),
  (4, 'DENIED', 'Denied', 4)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

COMMIT;
