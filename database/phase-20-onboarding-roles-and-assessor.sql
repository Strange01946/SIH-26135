-- Phase 20 — Account Onboarding, Registration Applications & Assessor Role
-- Dependencies: Phases 00–19
-- MySQL 8.0+

USE SIH26135;

START TRANSACTION;

-- 1. Ensure Assessor role exists in roles table
INSERT INTO roles (role_id, role_code, role_name, description, is_system_role, lifecycle_status_id) VALUES
  (10, 'assessor', 'Assessor', 'Assessment, evaluation and credential verification operations', 1, 1)
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  description = VALUES(description),
  is_system_role = VALUES(is_system_role),
  lifecycle_status_id = VALUES(lifecycle_status_id);

-- 2. Map Assessor role permissions (trainee.read = 3, assessment.manage = 11)
INSERT INTO role_permissions (role_id, permission_id) VALUES
  (10, 3),   -- trainee.read
  (10, 11)   -- assessment.manage
ON DUPLICATE KEY UPDATE granted_at = granted_at;

-- 3. Onboarding applications table for tracking provider and employer registration requests
CREATE TABLE IF NOT EXISTS onboarding_applications (
  application_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  application_number VARCHAR(64) NOT NULL,
  applicant_type VARCHAR(32) NOT NULL,           -- 'TRAINING_PROVIDER', 'EMPLOYER'
  user_id BIGINT UNSIGNED NOT NULL,
  entity_type VARCHAR(32) NOT NULL,              -- 'TRAINING_PROVIDER', 'EMPLOYER'
  entity_id BIGINT UNSIGNED NOT NULL,
  organization_name VARCHAR(200) NOT NULL,
  contact_person VARCHAR(150) NOT NULL,
  contact_email VARCHAR(255) NOT NULL,
  contact_phone VARCHAR(15) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
  rejection_reason VARCHAR(500) NULL,
  reviewed_by_user_id BIGINT UNSIGNED NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (application_id),
  UNIQUE KEY uk_onboarding_app_number (application_number),
  KEY idx_onboarding_user (user_id),
  KEY idx_onboarding_status (status),
  KEY idx_onboarding_entity (entity_type, entity_id),
  KEY idx_onboarding_email (contact_email),
  CONSTRAINT fk_onboarding_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  CONSTRAINT fk_onboarding_reviewer
    FOREIGN KEY (reviewed_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

COMMIT;
