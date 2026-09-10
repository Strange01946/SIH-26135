-- Phase 09 — Employers, jobs & placement
-- Dependencies: Phases 00–08
-- MySQL 8.0+
-- Placement is not confirmed employment (Phase 10). Do not treat a certificate as a placement.
-- Wage jobs, apprenticeships, internships, and self-employment are distinct engagement types.
-- Salary/CTC uses DECIMAL. Never use FLOAT. Never store Aadhaar, passwords, or API keys.

USE SIH26135;

-- ---------------------------------------------------------------------------
-- Lookups
-- Reuse existing: sectors, industries, job_roles, skills, skill_levels,
-- ref_skill_importance, states, districts, locations, organizations,
-- ref_organization_type, ref_lifecycle_status, ref_qualification_level,
-- trainees, training_enrollments, courses, programs, training_providers, users.
-- Do not reuse ref_employment_status (trainee snapshot) as a job/engagement type.
-- Do not reuse ref_certificate_verification_status (certificate-specific).
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ref_company_size (
  company_size_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  size_code VARCHAR(32) NOT NULL,
  size_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (company_size_id),
  UNIQUE KEY uk_ref_company_size_code (size_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Engagement type distinguishes wage work from apprenticeship, internship, and self-employment.
-- Later employment_records (Phase 10) should reuse this table rather than a second type catalog.
CREATE TABLE IF NOT EXISTS ref_engagement_type (
  engagement_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(32) NOT NULL,
  type_name VARCHAR(100) NOT NULL,
  is_wage_employment TINYINT(1) NOT NULL DEFAULT 0,
  is_self_employment TINYINT(1) NOT NULL DEFAULT 0,
  is_apprenticeship TINYINT(1) NOT NULL DEFAULT 0,
  is_internship TINYINT(1) NOT NULL DEFAULT 0,
  requires_employer TINYINT(1) NOT NULL DEFAULT 1,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (engagement_type_id),
  UNIQUE KEY uk_ref_engagement_type_code (type_code),
  CONSTRAINT ck_ref_engagement_type_flags
    CHECK (
      is_wage_employment + is_self_employment + is_apprenticeship + is_internship <= 1
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_salary_frequency (
  salary_frequency_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  frequency_code VARCHAR(32) NOT NULL,
  frequency_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (salary_frequency_id),
  UNIQUE KEY uk_ref_salary_frequency_code (frequency_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_job_posting_status (
  job_posting_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_open_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (job_posting_status_id),
  UNIQUE KEY uk_ref_job_posting_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_application_status (
  application_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (application_status_id),
  UNIQUE KEY uk_ref_application_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_placement_status (
  placement_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_offer_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_joined_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_unsuccessful_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (placement_status_id),
  UNIQUE KEY uk_ref_placement_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_placement_source (
  placement_source_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_code VARCHAR(32) NOT NULL,
  source_name VARCHAR(150) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (placement_source_id),
  UNIQUE KEY uk_ref_placement_source_code (source_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_non_selection_reason (
  non_selection_reason_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reason_code VARCHAR(32) NOT NULL,
  reason_name VARCHAR(150) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (non_selection_reason_id),
  UNIQUE KEY uk_ref_non_selection_reason_code (reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_offer_status (
  offer_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (offer_status_id),
  UNIQUE KEY uk_ref_offer_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_joining_status (
  joining_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_joined_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (joining_status_id),
  UNIQUE KEY uk_ref_joining_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Generic record verification. Not certificate verification (Phase 08).
-- Phase 12 employment verification can reuse these codes.
CREATE TABLE IF NOT EXISTS ref_record_verification_status (
  record_verification_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_verified_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (record_verification_status_id),
  UNIQUE KEY uk_ref_record_verification_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Employers and branches
-- ON DELETE RESTRICT: do not erase an employer under historical placements.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employers (
  employer_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  employer_code VARCHAR(32) NOT NULL,
  employer_name VARCHAR(200) NOT NULL,
  registration_number VARCHAR(64) NULL,
  gstin VARCHAR(15) NULL,
  organization_type_id BIGINT UNSIGNED NOT NULL,
  organization_id BIGINT UNSIGNED NULL,
  industry_id BIGINT UNSIGNED NULL,
  sector_id BIGINT UNSIGNED NULL,
  company_size_id BIGINT UNSIGNED NULL,
  contact_person_name VARCHAR(150) NULL,
  contact_email VARCHAR(255) NULL,
  contact_phone VARCHAR(15) NULL,
  address_line1 VARCHAR(200) NULL,
  address_line2 VARCHAR(200) NULL,
  pincode CHAR(6) NULL,
  location_id BIGINT UNSIGNED NULL,
  state_id BIGINT UNSIGNED NOT NULL,
  district_id BIGINT UNSIGNED NOT NULL,
  record_verification_status_id BIGINT UNSIGNED NOT NULL,
  verified_at DATETIME NULL,
  verified_by_user_id BIGINT UNSIGNED NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (employer_id),
  UNIQUE KEY uk_employers_code (employer_code),
  UNIQUE KEY uk_employers_registration (registration_number),
  UNIQUE KEY uk_employers_gstin (gstin),
  KEY idx_employers_industry (industry_id),
  KEY idx_employers_sector (sector_id),
  KEY idx_employers_district (district_id),
  KEY idx_employers_state (state_id),
  KEY idx_employers_location (location_id),
  KEY idx_employers_company_size (company_size_id),
  KEY idx_employers_org_type (organization_type_id),
  KEY idx_employers_organization (organization_id),
  KEY idx_employers_verification (record_verification_status_id),
  KEY idx_employers_status (lifecycle_status_id),
  KEY idx_employers_district_industry (district_id, industry_id),
  KEY idx_employers_verified_by (verified_by_user_id),
  CONSTRAINT fk_employers_org_type
    FOREIGN KEY (organization_type_id) REFERENCES ref_organization_type (organization_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employers_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (organization_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employers_industry
    FOREIGN KEY (industry_id) REFERENCES industries (industry_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employers_sector
    FOREIGN KEY (sector_id) REFERENCES sectors (sector_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employers_company_size
    FOREIGN KEY (company_size_id) REFERENCES ref_company_size (company_size_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employers_location
    FOREIGN KEY (location_id) REFERENCES locations (location_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employers_state
    FOREIGN KEY (state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employers_district
    FOREIGN KEY (district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employers_verification
    FOREIGN KEY (record_verification_status_id) REFERENCES ref_record_verification_status (record_verification_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employers_verified_by
    FOREIGN KEY (verified_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employers_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_employers_pincode
    CHECK (pincode IS NULL OR pincode REGEXP '^[1-9][0-9]{5}$'),
  CONSTRAINT ck_employers_gstin
    CHECK (gstin IS NULL OR CHAR_LENGTH(gstin) BETWEEN 10 AND 15)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS employer_branches (
  employer_branch_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  employer_id BIGINT UNSIGNED NOT NULL,
  branch_code VARCHAR(32) NOT NULL,
  branch_name VARCHAR(200) NOT NULL,
  is_head_office TINYINT(1) NOT NULL DEFAULT 0,
  contact_person_name VARCHAR(150) NULL,
  contact_email VARCHAR(255) NULL,
  contact_phone VARCHAR(15) NULL,
  address_line1 VARCHAR(200) NULL,
  address_line2 VARCHAR(200) NULL,
  pincode CHAR(6) NULL,
  location_id BIGINT UNSIGNED NULL,
  state_id BIGINT UNSIGNED NOT NULL,
  district_id BIGINT UNSIGNED NOT NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (employer_branch_id),
  UNIQUE KEY uk_employer_branches_code (employer_id, branch_code),
  KEY idx_employer_branches_employer (employer_id),
  KEY idx_employer_branches_district (district_id),
  KEY idx_employer_branches_state (state_id),
  KEY idx_employer_branches_location (location_id),
  KEY idx_employer_branches_status (lifecycle_status_id),
  CONSTRAINT fk_employer_branches_employer
    FOREIGN KEY (employer_id) REFERENCES employers (employer_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employer_branches_location
    FOREIGN KEY (location_id) REFERENCES locations (location_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_employer_branches_state
    FOREIGN KEY (state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employer_branches_district
    FOREIGN KEY (district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_employer_branches_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_employer_branches_pincode
    CHECK (pincode IS NULL OR pincode REGEXP '^[1-9][0-9]{5}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Job openings
-- employer_id is nullable only for self-employment / enterprise-support openings.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS job_postings (
  job_posting_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  posting_code VARCHAR(32) NOT NULL,
  posting_title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  employer_id BIGINT UNSIGNED NULL,
  employer_branch_id BIGINT UNSIGNED NULL,
  job_role_id BIGINT UNSIGNED NOT NULL,
  engagement_type_id BIGINT UNSIGNED NOT NULL,
  qualification_level_id BIGINT UNSIGNED NULL,
  vacancies INT UNSIGNED NOT NULL DEFAULT 1,
  min_salary DECIMAL(12,2) NULL,
  max_salary DECIMAL(12,2) NULL,
  salary_frequency_id BIGINT UNSIGNED NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'INR',
  state_id BIGINT UNSIGNED NOT NULL,
  district_id BIGINT UNSIGNED NOT NULL,
  location_id BIGINT UNSIGNED NULL,
  posted_date DATE NOT NULL,
  closing_date DATE NULL,
  job_posting_status_id BIGINT UNSIGNED NOT NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (job_posting_id),
  UNIQUE KEY uk_job_postings_code (posting_code),
  KEY idx_job_postings_employer (employer_id),
  KEY idx_job_postings_branch (employer_branch_id),
  KEY idx_job_postings_job_role (job_role_id),
  KEY idx_job_postings_engagement (engagement_type_id),
  KEY idx_job_postings_district (district_id),
  KEY idx_job_postings_state (state_id),
  KEY idx_job_postings_status (job_posting_status_id),
  KEY idx_job_postings_dates (posted_date, closing_date),
  KEY idx_job_postings_role_status (job_role_id, job_posting_status_id),
  KEY idx_job_postings_employer_status (employer_id, job_posting_status_id),
  KEY idx_job_postings_created_by (created_by_user_id),
  KEY idx_job_postings_qualification (qualification_level_id),
  KEY idx_job_postings_salary_frequency (salary_frequency_id),
  KEY idx_job_postings_location (location_id),
  CONSTRAINT fk_job_postings_employer
    FOREIGN KEY (employer_id) REFERENCES employers (employer_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_postings_branch
    FOREIGN KEY (employer_branch_id) REFERENCES employer_branches (employer_branch_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_job_postings_job_role
    FOREIGN KEY (job_role_id) REFERENCES job_roles (job_role_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_postings_engagement
    FOREIGN KEY (engagement_type_id) REFERENCES ref_engagement_type (engagement_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_postings_qualification
    FOREIGN KEY (qualification_level_id) REFERENCES ref_qualification_level (qualification_level_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_job_postings_salary_frequency
    FOREIGN KEY (salary_frequency_id) REFERENCES ref_salary_frequency (salary_frequency_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_job_postings_state
    FOREIGN KEY (state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_postings_district
    FOREIGN KEY (district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_postings_location
    FOREIGN KEY (location_id) REFERENCES locations (location_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_job_postings_status
    FOREIGN KEY (job_posting_status_id) REFERENCES ref_job_posting_status (job_posting_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_postings_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_job_postings_vacancies
    CHECK (vacancies >= 1),
  CONSTRAINT ck_job_postings_salary_range
    CHECK (
      min_salary IS NULL
      OR max_salary IS NULL
      OR max_salary >= min_salary
    ),
  CONSTRAINT ck_job_postings_min_salary
    CHECK (min_salary IS NULL OR min_salary >= 0),
  CONSTRAINT ck_job_postings_max_salary
    CHECK (max_salary IS NULL OR max_salary >= 0),
  CONSTRAINT ck_job_postings_dates
    CHECK (closing_date IS NULL OR closing_date >= posted_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Posting-specific skill demand. CASCADE only the junction if a posting is removed.
CREATE TABLE IF NOT EXISTS job_posting_skills (
  job_posting_id BIGINT UNSIGNED NOT NULL,
  skill_id BIGINT UNSIGNED NOT NULL,
  required_skill_level_id BIGINT UNSIGNED NOT NULL,
  skill_importance_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (job_posting_id, skill_id),
  KEY idx_job_posting_skills_skill (skill_id),
  KEY idx_job_posting_skills_level (required_skill_level_id),
  KEY idx_job_posting_skills_importance (skill_importance_id),
  CONSTRAINT fk_job_posting_skills_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings (job_posting_id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  CONSTRAINT fk_job_posting_skills_skill
    FOREIGN KEY (skill_id) REFERENCES skills (skill_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_posting_skills_level
    FOREIGN KEY (required_skill_level_id) REFERENCES skill_levels (skill_level_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_posting_skills_importance
    FOREIGN KEY (skill_importance_id) REFERENCES ref_skill_importance (skill_importance_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- One application per trainee per posting. Status changes update the row; do not duplicate.
CREATE TABLE IF NOT EXISTS job_applications (
  job_application_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  trainee_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NULL,
  job_posting_id BIGINT UNSIGNED NOT NULL,
  application_status_id BIGINT UNSIGNED NOT NULL,
  applied_date DATE NOT NULL,
  referred_by_user_id BIGINT UNSIGNED NULL,
  non_selection_reason_id BIGINT UNSIGNED NULL,
  remarks VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (job_application_id),
  UNIQUE KEY uk_job_applications_trainee_posting (trainee_id, job_posting_id),
  KEY idx_job_applications_enrollment (enrollment_id),
  KEY idx_job_applications_posting (job_posting_id),
  KEY idx_job_applications_status (application_status_id),
  KEY idx_job_applications_applied_date (applied_date),
  KEY idx_job_applications_referred_by (referred_by_user_id),
  KEY idx_job_applications_non_selection (non_selection_reason_id),
  CONSTRAINT fk_job_applications_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_applications_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_job_applications_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings (job_posting_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_applications_status
    FOREIGN KEY (application_status_id) REFERENCES ref_application_status (application_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_applications_referred_by
    FOREIGN KEY (referred_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_job_applications_non_selection
    FOREIGN KEY (non_selection_reason_id) REFERENCES ref_non_selection_reason (non_selection_reason_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Placement records
-- Historical: insert a new row for a later placement; do not overwrite a joined outcome
-- with a different employer. JOINED is facilitated placement, not a Phase 10 employment spell.
-- course_id / program_id / provider_id are copied from enrollment for analytics.
-- employer_id may be NULL for self-employment.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS placement_records (
  placement_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  placement_number VARCHAR(32) NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  program_id BIGINT UNSIGNED NOT NULL,
  provider_id BIGINT UNSIGNED NOT NULL,
  employer_id BIGINT UNSIGNED NULL,
  employer_branch_id BIGINT UNSIGNED NULL,
  job_posting_id BIGINT UNSIGNED NULL,
  job_application_id BIGINT UNSIGNED NULL,
  job_role_id BIGINT UNSIGNED NULL,
  engagement_type_id BIGINT UNSIGNED NOT NULL,
  placement_source_id BIGINT UNSIGNED NOT NULL,
  placement_status_id BIGINT UNSIGNED NOT NULL,
  joining_status_id BIGINT UNSIGNED NOT NULL,
  offered_salary DECIMAL(12,2) NULL,
  joining_salary DECIMAL(12,2) NULL,
  salary_frequency_id BIGINT UNSIGNED NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'INR',
  offer_date DATE NULL,
  expected_joining_date DATE NULL,
  actual_joining_date DATE NULL,
  non_selection_reason_id BIGINT UNSIGNED NULL,
  outcome_remarks VARCHAR(500) NULL,
  work_state_id BIGINT UNSIGNED NULL,
  work_district_id BIGINT UNSIGNED NULL,
  record_verification_status_id BIGINT UNSIGNED NOT NULL,
  verified_at DATETIME NULL,
  verified_by_user_id BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (placement_id),
  UNIQUE KEY uk_placement_records_number (placement_number),
  UNIQUE KEY uk_placement_records_application (job_application_id),
  UNIQUE KEY uk_placement_records_enrollment_posting (enrollment_id, job_posting_id),
  KEY idx_placement_records_trainee (trainee_id),
  KEY idx_placement_records_enrollment (enrollment_id),
  KEY idx_placement_records_course (course_id),
  KEY idx_placement_records_program (program_id),
  KEY idx_placement_records_provider (provider_id),
  KEY idx_placement_records_employer (employer_id),
  KEY idx_placement_records_posting (job_posting_id),
  KEY idx_placement_records_job_role (job_role_id),
  KEY idx_placement_records_engagement (engagement_type_id),
  KEY idx_placement_records_source (placement_source_id),
  KEY idx_placement_records_status (placement_status_id),
  KEY idx_placement_records_joining (joining_status_id),
  KEY idx_placement_records_district (work_district_id),
  KEY idx_placement_records_verification (record_verification_status_id),
  KEY idx_placement_records_employer_status (employer_id, placement_status_id),
  KEY idx_placement_records_course_status (course_id, placement_status_id),
  KEY idx_placement_records_verified_by (verified_by_user_id),
  KEY idx_placement_records_branch (employer_branch_id),
  KEY idx_placement_records_salary_frequency (salary_frequency_id),
  KEY idx_placement_records_non_selection (non_selection_reason_id),
  KEY idx_placement_records_work_state (work_state_id),
  CONSTRAINT fk_placement_records_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_course
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_program
    FOREIGN KEY (program_id) REFERENCES programs (program_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_provider
    FOREIGN KEY (provider_id) REFERENCES training_providers (provider_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_employer
    FOREIGN KEY (employer_id) REFERENCES employers (employer_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_branch
    FOREIGN KEY (employer_branch_id) REFERENCES employer_branches (employer_branch_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_placement_records_posting
    FOREIGN KEY (job_posting_id) REFERENCES job_postings (job_posting_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_application
    FOREIGN KEY (job_application_id) REFERENCES job_applications (job_application_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_placement_records_job_role
    FOREIGN KEY (job_role_id) REFERENCES job_roles (job_role_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_placement_records_engagement
    FOREIGN KEY (engagement_type_id) REFERENCES ref_engagement_type (engagement_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_source
    FOREIGN KEY (placement_source_id) REFERENCES ref_placement_source (placement_source_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_status
    FOREIGN KEY (placement_status_id) REFERENCES ref_placement_status (placement_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_joining
    FOREIGN KEY (joining_status_id) REFERENCES ref_joining_status (joining_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_salary_frequency
    FOREIGN KEY (salary_frequency_id) REFERENCES ref_salary_frequency (salary_frequency_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_placement_records_non_selection
    FOREIGN KEY (non_selection_reason_id) REFERENCES ref_non_selection_reason (non_selection_reason_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_placement_records_work_state
    FOREIGN KEY (work_state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_placement_records_work_district
    FOREIGN KEY (work_district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_placement_records_verification
    FOREIGN KEY (record_verification_status_id) REFERENCES ref_record_verification_status (record_verification_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_records_verified_by
    FOREIGN KEY (verified_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_placement_records_offered_salary
    CHECK (offered_salary IS NULL OR offered_salary >= 0),
  CONSTRAINT ck_placement_records_joining_salary
    CHECK (joining_salary IS NULL OR joining_salary >= 0),
  CONSTRAINT ck_placement_records_offer_before_join
    CHECK (actual_joining_date IS NULL OR offer_date IS NULL OR actual_joining_date >= offer_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Offer history. Do not overwrite a previous offer; insert a new row and mark the old one SUPERSEDED.
CREATE TABLE IF NOT EXISTS placement_offers (
  placement_offer_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  placement_id BIGINT UNSIGNED NOT NULL,
  offer_number VARCHAR(32) NOT NULL,
  employer_id BIGINT UNSIGNED NULL,
  job_role_id BIGINT UNSIGNED NULL,
  engagement_type_id BIGINT UNSIGNED NOT NULL,
  offered_salary DECIMAL(12,2) NULL,
  salary_frequency_id BIGINT UNSIGNED NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'INR',
  offer_date DATE NOT NULL,
  offer_valid_until DATE NULL,
  proposed_joining_date DATE NULL,
  offer_status_id BIGINT UNSIGNED NOT NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (placement_offer_id),
  UNIQUE KEY uk_placement_offers_number (offer_number),
  KEY idx_placement_offers_placement (placement_id),
  KEY idx_placement_offers_employer (employer_id),
  KEY idx_placement_offers_status (offer_status_id),
  KEY idx_placement_offers_engagement (engagement_type_id),
  KEY idx_placement_offers_created_by (created_by_user_id),
  KEY idx_placement_offers_job_role (job_role_id),
  KEY idx_placement_offers_salary_frequency (salary_frequency_id),
  CONSTRAINT fk_placement_offers_placement
    FOREIGN KEY (placement_id) REFERENCES placement_records (placement_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_offers_employer
    FOREIGN KEY (employer_id) REFERENCES employers (employer_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_offers_job_role
    FOREIGN KEY (job_role_id) REFERENCES job_roles (job_role_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_placement_offers_engagement
    FOREIGN KEY (engagement_type_id) REFERENCES ref_engagement_type (engagement_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_offers_salary_frequency
    FOREIGN KEY (salary_frequency_id) REFERENCES ref_salary_frequency (salary_frequency_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_placement_offers_status
    FOREIGN KEY (offer_status_id) REFERENCES ref_offer_status (offer_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_placement_offers_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_placement_offers_salary
    CHECK (offered_salary IS NULL OR offered_salary >= 0),
  CONSTRAINT ck_placement_offers_dates
    CHECK (offer_valid_until IS NULL OR offer_valid_until >= offer_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Optional user affiliation. Idempotent if this phase is re-run.
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'SIH26135'
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'employer_id'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE users
     ADD COLUMN employer_id BIGINT UNSIGNED NULL AFTER training_provider_id,
     ADD KEY idx_users_employer (employer_id),
     ADD CONSTRAINT fk_users_employer
       FOREIGN KEY (employer_id) REFERENCES employers (employer_id)
       ON UPDATE RESTRICT
       ON DELETE SET NULL',
  'SELECT ''users.employer_id already exists'' AS alter_skip_notice'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

START TRANSACTION;

INSERT INTO ref_company_size (company_size_id, size_code, size_name, sort_order) VALUES
  (1, 'MICRO', 'Micro', 1),
  (2, 'SMALL', 'Small', 2),
  (3, 'MEDIUM', 'Medium', 3),
  (4, 'LARGE', 'Large', 4),
  (5, 'ENTERPRISE', 'Enterprise / large corporate', 5),
  (6, 'GOVERNMENT', 'Government / public sector', 6),
  (7, 'UNCLASSIFIED', 'Unclassified', 7)
ON DUPLICATE KEY UPDATE
  size_name = VALUES(size_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_engagement_type (
  engagement_type_id, type_code, type_name,
  is_wage_employment, is_self_employment, is_apprenticeship, is_internship,
  requires_employer, sort_order
) VALUES
  (1, 'WAGE_EMPLOYMENT', 'Wage employment', 1, 0, 0, 0, 1, 1),
  (2, 'CONTRACT_EMPLOYMENT', 'Fixed-term / contract wage employment', 1, 0, 0, 0, 1, 2),
  (3, 'APPRENTICESHIP', 'Apprenticeship', 0, 0, 1, 0, 1, 3),
  (4, 'INTERNSHIP', 'Internship', 0, 0, 0, 1, 1, 4),
  (5, 'SELF_EMPLOYMENT', 'Self-employment / enterprise', 0, 1, 0, 0, 0, 5),
  (6, 'GIG_CASUAL', 'Gig / casual work', 0, 0, 0, 0, 1, 6)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  is_wage_employment = VALUES(is_wage_employment),
  is_self_employment = VALUES(is_self_employment),
  is_apprenticeship = VALUES(is_apprenticeship),
  is_internship = VALUES(is_internship),
  requires_employer = VALUES(requires_employer),
  sort_order = VALUES(sort_order);

INSERT INTO ref_salary_frequency (salary_frequency_id, frequency_code, frequency_name, sort_order) VALUES
  (1, 'MONTHLY', 'Monthly', 1),
  (2, 'ANNUAL', 'Annual CTC', 2),
  (3, 'WEEKLY', 'Weekly', 3),
  (4, 'DAILY', 'Daily', 4)
ON DUPLICATE KEY UPDATE
  frequency_name = VALUES(frequency_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_job_posting_status (
  job_posting_status_id, status_code, status_name, is_open_flag, sort_order
) VALUES
  (1, 'DRAFT', 'Draft', 0, 1),
  (2, 'OPEN', 'Open', 1, 2),
  (3, 'ON_HOLD', 'On hold', 0, 3),
  (4, 'FILLED', 'Filled', 0, 4),
  (5, 'CLOSED', 'Closed', 0, 5),
  (6, 'CANCELLED', 'Cancelled', 0, 6)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_open_flag = VALUES(is_open_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_application_status (application_status_id, status_code, status_name, sort_order) VALUES
  (1, 'APPLIED', 'Applied', 1),
  (2, 'REFERRED', 'Referred', 2),
  (3, 'SHORTLISTED', 'Shortlisted', 3),
  (4, 'INTERVIEWED', 'Interviewed', 4),
  (5, 'SELECTED', 'Selected', 5),
  (6, 'REJECTED', 'Rejected / not selected', 6),
  (7, 'WITHDRAWN', 'Withdrawn', 7),
  (8, 'OFFERED', 'Offered', 8)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_placement_status (
  placement_status_id, status_code, status_name,
  is_offer_flag, is_joined_flag, is_unsuccessful_flag, sort_order
) VALUES
  (1, 'INITIATED', 'Initiated', 0, 0, 0, 1),
  (2, 'OFFERED', 'Offered', 1, 0, 0, 2),
  (3, 'ACCEPTED', 'Offer accepted', 1, 0, 0, 3),
  (4, 'JOINED', 'Joined', 1, 1, 0, 4),
  (5, 'NOT_JOINED', 'Did not join', 1, 0, 1, 5),
  (6, 'REJECTED', 'Not selected', 0, 0, 1, 6),
  (7, 'DECLINED', 'Offer declined', 1, 0, 1, 7),
  (8, 'WITHDRAWN', 'Withdrawn', 0, 0, 1, 8),
  (9, 'CANCELLED', 'Cancelled', 0, 0, 1, 9)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_offer_flag = VALUES(is_offer_flag),
  is_joined_flag = VALUES(is_joined_flag),
  is_unsuccessful_flag = VALUES(is_unsuccessful_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_placement_source (placement_source_id, source_code, source_name, sort_order) VALUES
  (1, 'CAMPUS', 'Campus / batch placement drive', 1),
  (2, 'PROVIDER_CELL', 'Training provider placement cell', 2),
  (3, 'JOB_MELA', 'Government job mela / Rozgar mela', 3),
  (4, 'JOB_PORTAL', 'Job portal', 4),
  (5, 'EMPLOYER_DIRECT', 'Employer direct / walk-in', 5),
  (6, 'REFERRAL', 'Referral', 6),
  (7, 'SELF_SOURCED', 'Trainee self-sourced', 7),
  (8, 'APPRENTICESHIP_PORTAL', 'Apprenticeship portal', 8),
  (9, 'SELF_ENTERPRISE', 'Self-enterprise / start-up support', 9)
ON DUPLICATE KEY UPDATE
  source_name = VALUES(source_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_non_selection_reason (non_selection_reason_id, reason_code, reason_name, sort_order) VALUES
  (1, 'SKILL_GAP', 'Skill gap / did not meet skill requirement', 1),
  (2, 'EXPERIENCE', 'Insufficient experience', 2),
  (3, 'QUALIFICATION', 'Qualification mismatch', 3),
  (4, 'INTERVIEW_PERFORMANCE', 'Interview performance', 4),
  (5, 'SALARY_EXPECTATION', 'Salary expectation mismatch', 5),
  (6, 'LOCATION', 'Location / relocation', 6),
  (7, 'VACANCY_FILLED', 'Vacancy already filled', 7),
  (8, 'DOCUMENTATION', 'Documentation incomplete', 8),
  (9, 'MEDICAL', 'Medical / fitness', 9),
  (10, 'OTHER', 'Other', 10)
ON DUPLICATE KEY UPDATE
  reason_name = VALUES(reason_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_offer_status (offer_status_id, status_code, status_name, sort_order) VALUES
  (1, 'EXTENDED', 'Extended', 1),
  (2, 'ACCEPTED', 'Accepted', 2),
  (3, 'DECLINED', 'Declined', 3),
  (4, 'EXPIRED', 'Expired', 4),
  (5, 'WITHDRAWN', 'Withdrawn by employer', 5),
  (6, 'SUPERSEDED', 'Superseded by a later offer', 6)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_joining_status (
  joining_status_id, status_code, status_name, is_joined_flag, sort_order
) VALUES
  (1, 'NOT_APPLICABLE', 'Not applicable', 0, 1),
  (2, 'PENDING', 'Pending joining', 0, 2),
  (3, 'JOINED', 'Joined', 1, 3),
  (4, 'DELAYED', 'Delayed', 0, 4),
  (5, 'NO_SHOW', 'No show', 0, 5),
  (6, 'DEFERRED', 'Deferred', 0, 6)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_joined_flag = VALUES(is_joined_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_record_verification_status (
  record_verification_status_id, status_code, status_name, is_verified_flag, sort_order
) VALUES
  (1, 'UNVERIFIED', 'Unverified', 0, 1),
  (2, 'SELF_REPORTED', 'Self-reported', 0, 2),
  (3, 'PENDING', 'Pending verification', 0, 3),
  (4, 'VERIFIED', 'Verified', 1, 4),
  (5, 'REJECTED', 'Rejected', 0, 5)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_verified_flag = VALUES(is_verified_flag),
  sort_order = VALUES(sort_order);

COMMIT;
