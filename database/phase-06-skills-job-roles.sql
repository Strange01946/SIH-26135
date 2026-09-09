-- Phase 06 — Skills, job roles & skill mapping
-- Dependencies: Phases 00–05
-- MySQL 8.0+
-- Courses N—N skills; job roles N—N skills. Track taught/required level and importance.

USE SIH26135;

-- ---------------------------------------------------------------------------
-- Skill catalog lookups
-- skill_levels.level_rank supports later gap scoring (required - current)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS skill_categories (
  skill_category_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_code VARCHAR(32) NOT NULL,
  category_name VARCHAR(150) NOT NULL,
  description VARCHAR(500) NULL,
  parent_category_id BIGINT UNSIGNED NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (skill_category_id),
  UNIQUE KEY uk_skill_categories_code (category_code),
  UNIQUE KEY uk_skill_categories_name (category_name),
  KEY idx_skill_categories_parent (parent_category_id),
  KEY idx_skill_categories_status (lifecycle_status_id),
  CONSTRAINT fk_skill_categories_parent
    FOREIGN KEY (parent_category_id) REFERENCES skill_categories (skill_category_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_skill_categories_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS skill_levels (
  skill_level_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  level_code VARCHAR(32) NOT NULL,
  level_name VARCHAR(100) NOT NULL,
  level_rank TINYINT UNSIGNED NOT NULL,
  description VARCHAR(500) NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_level_id),
  UNIQUE KEY uk_skill_levels_code (level_code),
  UNIQUE KEY uk_skill_levels_rank (level_rank),
  CONSTRAINT ck_skill_levels_rank
    CHECK (level_rank BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_skill_importance (
  skill_importance_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  importance_code VARCHAR(32) NOT NULL,
  importance_name VARCHAR(100) NOT NULL,
  importance_weight TINYINT UNSIGNED NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (skill_importance_id),
  UNIQUE KEY uk_ref_skill_importance_code (importance_code),
  CONSTRAINT ck_ref_skill_importance_weight
    CHECK (importance_weight BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS skills (
  skill_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  skill_code VARCHAR(32) NOT NULL,
  skill_name VARCHAR(150) NOT NULL,
  description VARCHAR(1000) NULL,
  skill_category_id BIGINT UNSIGNED NOT NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (skill_id),
  UNIQUE KEY uk_skills_code (skill_code),
  UNIQUE KEY uk_skills_name (skill_name),
  KEY idx_skills_category (skill_category_id),
  KEY idx_skills_status (lifecycle_status_id),
  CONSTRAINT fk_skills_category
    FOREIGN KEY (skill_category_id) REFERENCES skill_categories (skill_category_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_skills_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Junction: CASCADE when a course catalog row is removed; RESTRICT skill deletes
-- while the skill remains mapped to any course.
CREATE TABLE IF NOT EXISTS course_skills (
  course_id BIGINT UNSIGNED NOT NULL,
  skill_id BIGINT UNSIGNED NOT NULL,
  taught_skill_level_id BIGINT UNSIGNED NOT NULL,
  skill_importance_id BIGINT UNSIGNED NOT NULL,
  is_core_skill TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (course_id, skill_id),
  KEY idx_course_skills_skill (skill_id),
  KEY idx_course_skills_level (taught_skill_level_id),
  KEY idx_course_skills_importance (skill_importance_id),
  KEY idx_course_skills_core (course_id, is_core_skill),
  CONSTRAINT fk_course_skills_course
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  CONSTRAINT fk_course_skills_skill
    FOREIGN KEY (skill_id) REFERENCES skills (skill_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_course_skills_level
    FOREIGN KEY (taught_skill_level_id) REFERENCES skill_levels (skill_level_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_course_skills_importance
    FOREIGN KEY (skill_importance_id) REFERENCES ref_skill_importance (skill_importance_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS job_roles (
  job_role_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  job_role_code VARCHAR(32) NOT NULL,
  job_role_name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  sector_id BIGINT UNSIGNED NOT NULL,
  industry_id BIGINT UNSIGNED NULL,
  qualification_level_id BIGINT UNSIGNED NULL,
  nco_code VARCHAR(16) NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (job_role_id),
  UNIQUE KEY uk_job_roles_code (job_role_code),
  KEY idx_job_roles_sector (sector_id),
  KEY idx_job_roles_industry (industry_id),
  KEY idx_job_roles_qualification (qualification_level_id),
  KEY idx_job_roles_status (lifecycle_status_id),
  KEY idx_job_roles_nco (nco_code),
  CONSTRAINT fk_job_roles_sector
    FOREIGN KEY (sector_id) REFERENCES sectors (sector_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_roles_industry
    FOREIGN KEY (industry_id) REFERENCES industries (industry_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_job_roles_qualification
    FOREIGN KEY (qualification_level_id) REFERENCES ref_qualification_level (qualification_level_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_job_roles_lifecycle_status
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Junction: CASCADE when a job role is removed from the catalog; RESTRICT skill deletes
-- while the skill remains required by any job role. Later employment history should
-- reference job_roles with RESTRICT so roles in use are not dropped.
CREATE TABLE IF NOT EXISTS job_role_skills (
  job_role_id BIGINT UNSIGNED NOT NULL,
  skill_id BIGINT UNSIGNED NOT NULL,
  required_skill_level_id BIGINT UNSIGNED NOT NULL,
  skill_importance_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (job_role_id, skill_id),
  KEY idx_job_role_skills_skill (skill_id),
  KEY idx_job_role_skills_level (required_skill_level_id),
  KEY idx_job_role_skills_importance (skill_importance_id),
  KEY idx_job_role_skills_skill_level (skill_id, required_skill_level_id),
  CONSTRAINT fk_job_role_skills_job_role
    FOREIGN KEY (job_role_id) REFERENCES job_roles (job_role_id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  CONSTRAINT fk_job_role_skills_skill
    FOREIGN KEY (skill_id) REFERENCES skills (skill_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_role_skills_level
    FOREIGN KEY (required_skill_level_id) REFERENCES skill_levels (skill_level_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_job_role_skills_importance
    FOREIGN KEY (skill_importance_id) REFERENCES ref_skill_importance (skill_importance_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO skill_levels (
  skill_level_id, level_code, level_name, level_rank, description, sort_order
) VALUES
  (1, 'AWARENESS', 'Awareness', 1, 'Can recognize the skill and related terms', 1),
  (2, 'BASIC', 'Basic', 2, 'Can perform simple tasks with supervision', 2),
  (3, 'INTERMEDIATE', 'Intermediate', 3, 'Can perform independently in typical situations', 3),
  (4, 'ADVANCED', 'Advanced', 4, 'Can handle complex situations and mentor others', 4),
  (5, 'EXPERT', 'Expert', 5, 'Can design standards and lead specialist work', 5)
ON DUPLICATE KEY UPDATE
  level_name = VALUES(level_name),
  level_rank = VALUES(level_rank),
  description = VALUES(description),
  sort_order = VALUES(sort_order);

INSERT INTO ref_skill_importance (
  skill_importance_id, importance_code, importance_name, importance_weight, sort_order
) VALUES
  (1, 'OPTIONAL', 'Optional', 1, 1),
  (2, 'DESIRABLE', 'Desirable', 2, 2),
  (3, 'IMPORTANT', 'Important', 3, 3),
  (4, 'CORE', 'Core', 4, 4),
  (5, 'MANDATORY', 'Mandatory', 5, 5)
ON DUPLICATE KEY UPDATE
  importance_name = VALUES(importance_name),
  importance_weight = VALUES(importance_weight),
  sort_order = VALUES(sort_order);

COMMIT;
