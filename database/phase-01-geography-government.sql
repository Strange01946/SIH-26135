-- Phase 01 — Geography & government master data
-- Dependencies: Phase 00
-- MySQL 8.0+

USE SIH26135;

-- ---------------------------------------------------------------------------
-- Lookup: shared lifecycle status for master and transactional records
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ref_lifecycle_status (
  lifecycle_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (lifecycle_status_id),
  UNIQUE KEY uk_ref_lifecycle_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_location_type (
  location_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(32) NOT NULL,
  type_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (location_type_id),
  UNIQUE KEY uk_ref_location_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_organization_type (
  organization_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(32) NOT NULL,
  type_name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (organization_type_id),
  UNIQUE KEY uk_ref_organization_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Geography
-- ON DELETE RESTRICT: geography masters must not disappear under trainees
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS states (
  state_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  state_code CHAR(2) NOT NULL,
  state_name VARCHAR(100) NOT NULL,
  lgd_code VARCHAR(16) NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (state_id),
  UNIQUE KEY uk_states_code (state_code),
  UNIQUE KEY uk_states_name (state_name),
  UNIQUE KEY uk_states_lgd_code (lgd_code),
  KEY idx_states_status (lifecycle_status_id),
  CONSTRAINT fk_states_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS districts (
  district_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  state_id BIGINT UNSIGNED NOT NULL,
  district_code VARCHAR(16) NOT NULL,
  district_name VARCHAR(100) NOT NULL,
  lgd_code VARCHAR(16) NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (district_id),
  UNIQUE KEY uk_districts_state_code (state_id, district_code),
  UNIQUE KEY uk_districts_state_name (state_id, district_name),
  UNIQUE KEY uk_districts_lgd_code (lgd_code),
  KEY idx_districts_state_id (state_id),
  KEY idx_districts_status (lifecycle_status_id),
  CONSTRAINT fk_districts_state
    FOREIGN KEY (state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_districts_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS blocks (
  block_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  district_id BIGINT UNSIGNED NOT NULL,
  block_code VARCHAR(16) NOT NULL,
  block_name VARCHAR(120) NOT NULL,
  lgd_code VARCHAR(16) NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (block_id),
  UNIQUE KEY uk_blocks_district_code (district_id, block_code),
  UNIQUE KEY uk_blocks_district_name (district_id, block_name),
  UNIQUE KEY uk_blocks_lgd_code (lgd_code),
  KEY idx_blocks_district_id (district_id),
  KEY idx_blocks_status (lifecycle_status_id),
  CONSTRAINT fk_blocks_district
    FOREIGN KEY (district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_blocks_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS locations (
  location_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  district_id BIGINT UNSIGNED NOT NULL,
  block_id BIGINT UNSIGNED NULL,
  location_type_id BIGINT UNSIGNED NOT NULL,
  location_name VARCHAR(150) NOT NULL,
  pincode CHAR(6) NULL,
  latitude DECIMAL(9,6) NULL,
  longitude DECIMAL(9,6) NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (location_id),
  UNIQUE KEY uk_locations_district_name_type (district_id, location_name, location_type_id),
  KEY idx_locations_district_id (district_id),
  KEY idx_locations_block_id (block_id),
  KEY idx_locations_type (location_type_id),
  KEY idx_locations_pincode (pincode),
  KEY idx_locations_status (lifecycle_status_id),
  CONSTRAINT fk_locations_district
    FOREIGN KEY (district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_locations_block
    FOREIGN KEY (block_id) REFERENCES blocks (block_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_locations_type
    FOREIGN KEY (location_type_id) REFERENCES ref_location_type (location_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_locations_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_locations_pincode
    CHECK (pincode IS NULL OR pincode REGEXP '^[1-9][0-9]{5}$'),
  CONSTRAINT ck_locations_lat
    CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
  CONSTRAINT ck_locations_lng
    CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Organizations and departments
-- parent_organization_id ON DELETE SET NULL: unlinks hierarchy, keeps the child
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS organizations (
  organization_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  organization_code VARCHAR(32) NOT NULL,
  organization_name VARCHAR(200) NOT NULL,
  organization_type_id BIGINT UNSIGNED NOT NULL,
  parent_organization_id BIGINT UNSIGNED NULL,
  state_id BIGINT UNSIGNED NULL,
  district_id BIGINT UNSIGNED NULL,
  location_id BIGINT UNSIGNED NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (organization_id),
  UNIQUE KEY uk_organizations_code (organization_code),
  KEY idx_organizations_type (organization_type_id),
  KEY idx_organizations_parent (parent_organization_id),
  KEY idx_organizations_state (state_id),
  KEY idx_organizations_district (district_id),
  KEY idx_organizations_location (location_id),
  KEY idx_organizations_status (lifecycle_status_id),
  CONSTRAINT fk_organizations_type
    FOREIGN KEY (organization_type_id) REFERENCES ref_organization_type (organization_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_organizations_parent
    FOREIGN KEY (parent_organization_id) REFERENCES organizations (organization_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_organizations_state
    FOREIGN KEY (state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_organizations_district
    FOREIGN KEY (district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_organizations_location
    FOREIGN KEY (location_id) REFERENCES locations (location_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_organizations_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS departments (
  department_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  organization_id BIGINT UNSIGNED NOT NULL,
  department_code VARCHAR(32) NOT NULL,
  department_name VARCHAR(200) NOT NULL,
  parent_department_id BIGINT UNSIGNED NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (department_id),
  UNIQUE KEY uk_departments_org_code (organization_id, department_code),
  KEY idx_departments_organization (organization_id),
  KEY idx_departments_parent (parent_department_id),
  KEY idx_departments_status (lifecycle_status_id),
  CONSTRAINT fk_departments_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (organization_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_departments_parent
    FOREIGN KEY (parent_department_id) REFERENCES departments (department_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_departments_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Schemes and programs
-- Budget is DECIMAL. Never use FLOAT for money.
-- ON DELETE RESTRICT on department/scheme: historical program rows must remain
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schemes (
  scheme_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  scheme_code VARCHAR(32) NOT NULL,
  scheme_name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  department_id BIGINT UNSIGNED NOT NULL,
  start_date DATE NULL,
  end_date DATE NULL,
  budget DECIMAL(18,2) NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'INR',
  target_beneficiaries INT UNSIGNED NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (scheme_id),
  UNIQUE KEY uk_schemes_code (scheme_code),
  KEY idx_schemes_department (department_id),
  KEY idx_schemes_status (lifecycle_status_id),
  KEY idx_schemes_dates (start_date, end_date),
  CONSTRAINT fk_schemes_department
    FOREIGN KEY (department_id) REFERENCES departments (department_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_schemes_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_schemes_budget
    CHECK (budget IS NULL OR budget >= 0),
  CONSTRAINT ck_schemes_dates
    CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS programs (
  program_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  program_code VARCHAR(32) NOT NULL,
  program_name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  department_id BIGINT UNSIGNED NOT NULL,
  scheme_id BIGINT UNSIGNED NOT NULL,
  start_date DATE NULL,
  end_date DATE NULL,
  budget DECIMAL(18,2) NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'INR',
  target_beneficiaries INT UNSIGNED NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (program_id),
  UNIQUE KEY uk_programs_code (program_code),
  KEY idx_programs_department (department_id),
  KEY idx_programs_scheme (scheme_id),
  KEY idx_programs_status (lifecycle_status_id),
  KEY idx_programs_dates (start_date, end_date),
  CONSTRAINT fk_programs_department
    FOREIGN KEY (department_id) REFERENCES departments (department_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_programs_scheme
    FOREIGN KEY (scheme_id) REFERENCES schemes (scheme_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_programs_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_programs_budget
    CHECK (budget IS NULL OR budget >= 0),
  CONSTRAINT ck_programs_dates
    CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_lifecycle_status (
  lifecycle_status_id, status_code, status_name, description, sort_order, is_active
) VALUES
  (1, 'ACTIVE', 'Active', 'Record is in operational use', 1, 1),
  (2, 'INACTIVE', 'Inactive', 'Record is not currently operational', 2, 1),
  (3, 'DRAFT', 'Draft', 'Record is not yet published', 3, 1),
  (4, 'SUSPENDED', 'Suspended', 'Temporarily stopped', 4, 1),
  (5, 'ARCHIVED', 'Archived', 'Retained for history; not operational', 5, 1),
  (6, 'CLOSED', 'Closed', 'Completed or closed period', 6, 1)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  description = VALUES(description),
  sort_order = VALUES(sort_order),
  is_active = VALUES(is_active);

INSERT INTO ref_location_type (location_type_id, type_code, type_name, sort_order) VALUES
  (1, 'CITY', 'City', 1),
  (2, 'TOWN', 'Town', 2),
  (3, 'VILLAGE', 'Village', 3),
  (4, 'URBAN_LOCAL_BODY', 'Urban local body', 4),
  (5, 'OTHER', 'Other', 5)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_organization_type (organization_type_id, type_code, type_name, description, sort_order) VALUES
  (1, 'CENTRAL_MINISTRY', 'Central ministry', 'Union government ministry', 1),
  (2, 'STATE_DEPARTMENT', 'State department', 'State government department', 2),
  (3, 'AUTONOMOUS_BODY', 'Autonomous body', 'Statutory or autonomous organization', 3),
  (4, 'SECTOR_SKILL_COUNCIL', 'Sector skill council', 'Industry skill body', 4),
  (5, 'TRAINING_PROVIDER', 'Training provider', 'Empanelled training partner', 5),
  (6, 'EMPLOYER', 'Employer', 'Hiring organization', 6),
  (7, 'NGO', 'NGO / civil society', 'Non-government implementing partner', 7),
  (8, 'OTHER', 'Other', 'Unclassified organization type', 8)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  description = VALUES(description),
  sort_order = VALUES(sort_order);

COMMIT;
