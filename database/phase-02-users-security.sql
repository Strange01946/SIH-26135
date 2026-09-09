-- Phase 02 — Users, roles, permissions & security
-- Dependencies: Phases 00–01
-- MySQL 8.0+
-- Passwords are stored as hashes only. Never store plaintext.

USE SIH26135;

CREATE TABLE IF NOT EXISTS ref_user_status (
  user_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_status_id),
  UNIQUE KEY uk_ref_user_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS roles (
  role_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  is_system_role TINYINT(1) NOT NULL DEFAULT 1,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (role_id),
  UNIQUE KEY uk_roles_code (role_code),
  KEY idx_roles_status (lifecycle_status_id),
  CONSTRAINT fk_roles_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS permissions (
  permission_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  permission_code VARCHAR(100) NOT NULL,
  permission_name VARCHAR(150) NOT NULL,
  module_code VARCHAR(64) NOT NULL,
  description VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (permission_id),
  UNIQUE KEY uk_permissions_code (permission_code),
  KEY idx_permissions_module (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- password_hash stores bcrypt/argon2/scrypt output only. Never store plaintext.
CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(80) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  password_algo VARCHAR(32) NOT NULL DEFAULT 'argon2id',
  user_status_id BIGINT UNSIGNED NOT NULL,
  last_login_at DATETIME NULL,
  failed_login_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  locked_until DATETIME NULL,
  must_change_password TINYINT(1) NOT NULL DEFAULT 0,
  organization_id BIGINT UNSIGNED NULL,
  department_id BIGINT UNSIGNED NULL,
  state_id BIGINT UNSIGNED NULL,
  district_id BIGINT UNSIGNED NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_status (user_status_id),
  KEY idx_users_organization (organization_id),
  KEY idx_users_department (department_id),
  KEY idx_users_state (state_id),
  KEY idx_users_district (district_id),
  KEY idx_users_created_by (created_by_user_id),
  KEY idx_users_last_login (last_login_at),
  CONSTRAINT fk_users_status
    FOREIGN KEY (user_status_id) REFERENCES ref_user_status (user_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_users_organization
    FOREIGN KEY (organization_id) REFERENCES organizations (organization_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_users_department
    FOREIGN KEY (department_id) REFERENCES departments (department_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_users_state
    FOREIGN KEY (state_id) REFERENCES states (state_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_users_district
    FOREIGN KEY (district_id) REFERENCES districts (district_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_users_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_users_failed_login
    CHECK (failed_login_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Junction: CASCADE on user delete removes assignments; RESTRICT on role protects RBAC catalog
CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  assigned_by_user_id BIGINT UNSIGNED NULL,
  PRIMARY KEY (user_id, role_id),
  KEY idx_user_roles_role (role_id),
  KEY idx_user_roles_assigned_by (assigned_by_user_id),
  CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id) REFERENCES roles (role_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_user_roles_assigned_by
    FOREIGN KEY (assigned_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
  role_id BIGINT UNSIGNED NOT NULL,
  permission_id BIGINT UNSIGNED NOT NULL,
  granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (role_id, permission_id),
  KEY idx_role_permissions_permission (permission_id),
  CONSTRAINT fk_role_permissions_role
    FOREIGN KEY (role_id) REFERENCES roles (role_id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  CONSTRAINT fk_role_permissions_permission
    FOREIGN KEY (permission_id) REFERENCES permissions (permission_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_user_status (user_status_id, status_code, status_name, sort_order) VALUES
  (1, 'PENDING', 'Pending activation', 1),
  (2, 'ACTIVE', 'Active', 2),
  (3, 'LOCKED', 'Locked', 3),
  (4, 'DISABLED', 'Disabled', 4),
  (5, 'ARCHIVED', 'Archived', 5)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

INSERT INTO roles (role_id, role_code, role_name, description, is_system_role, lifecycle_status_id) VALUES
  (1, 'super_admin', 'Super Admin', 'Full platform administration', 1, 1),
  (2, 'government_admin', 'Government Admin', 'National/state scheme administration', 1, 1),
  (3, 'government_officer', 'Government Officer', 'Operational government user', 1, 1),
  (4, 'district_officer', 'District Officer', 'District-scoped operations and review', 1, 1),
  (5, 'training_provider', 'Training Provider', 'Provider and center operations', 1, 1),
  (6, 'employer', 'Employer', 'Employer and placement operations', 1, 1),
  (7, 'verifier', 'Verifier', 'Employment and document verification', 1, 1),
  (8, 'analyst', 'Analyst', 'Read-only analytics and reporting', 1, 1),
  (9, 'trainee', 'Trainee', 'Self-service trainee portal', 1, 1)
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  description = VALUES(description),
  is_system_role = VALUES(is_system_role),
  lifecycle_status_id = VALUES(lifecycle_status_id);

INSERT INTO permissions (permission_id, permission_code, permission_name, module_code, description) VALUES
  (1, 'system.manage', 'Manage system', 'system', 'System configuration'),
  (2, 'user.manage', 'Manage users', 'security', 'Create and update users and roles'),
  (3, 'trainee.read', 'Read trainees', 'trainee', 'View trainee records'),
  (4, 'trainee.write', 'Write trainees', 'trainee', 'Create and update trainee records'),
  (5, 'consent.manage', 'Manage consent', 'trainee', 'Record and revoke consent'),
  (6, 'program.manage', 'Manage programs', 'program', 'Maintain schemes and programs'),
  (7, 'provider.manage', 'Manage providers', 'provider', 'Maintain training providers'),
  (8, 'center.manage', 'Manage centers', 'training', 'Maintain training centers'),
  (9, 'course.manage', 'Manage courses', 'training', 'Maintain courses and batches'),
  (10, 'enrollment.manage', 'Manage enrollment', 'training', 'Enroll and complete training'),
  (11, 'assessment.manage', 'Manage assessments', 'training', 'Assessments and certifications'),
  (12, 'placement.manage', 'Manage placements', 'employment', 'Job postings and placements'),
  (13, 'employment.manage', 'Manage employment', 'employment', 'Employment and salary history'),
  (14, 'verification.manage', 'Manage verification', 'employment', 'Verify employment claims'),
  (15, 'survey.manage', 'Manage surveys', 'followup', 'Follow-up surveys and communication'),
  (16, 'analytics.read', 'Read analytics', 'analytics', 'Dashboards and outcome reports'),
  (17, 'audit.read', 'Read audit logs', 'audit', 'View audit and data-quality issues')
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  module_code = VALUES(module_code),
  description = VALUES(description);

INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, permission_id FROM permissions
ON DUPLICATE KEY UPDATE granted_at = granted_at;

INSERT INTO role_permissions (role_id, permission_id) VALUES
  (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9),
  (2, 10), (2, 11), (2, 12), (2, 13), (2, 14), (2, 15), (2, 16), (2, 17),
  (3, 3), (3, 4), (3, 5), (3, 10), (3, 11), (3, 12), (3, 13), (3, 15), (3, 16),
  (4, 3), (4, 4), (4, 10), (4, 12), (4, 13), (4, 15), (4, 16),
  (5, 3), (5, 8), (5, 9), (5, 10), (5, 11), (5, 12),
  (6, 12), (6, 13), (6, 14),
  (7, 3), (7, 13), (7, 14), (7, 17),
  (8, 3), (8, 16), (8, 17),
  (9, 3), (9, 5)
ON DUPLICATE KEY UPDATE granted_at = granted_at;

COMMIT;
