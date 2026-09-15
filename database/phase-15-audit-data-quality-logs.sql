-- Phase 15 — Audit, data quality & operational logs
-- Dependencies: Phases 00–14
-- MySQL 8.0+
-- Append-only audit and system events. Do not duplicate communication_logs,
-- employment verification, skill-gap, or unemployment history.
-- Do not store passwords, API keys, tokens, Aadhaar, or full PII snapshots.
-- entity_type is a table/concept name (VARCHAR), not a live catalog FK.
-- MySQL DDL may implicitly commit; lookup DML is the only explicit transaction.

USE SIH26135;

CREATE TABLE IF NOT EXISTS ref_audit_action (
  audit_action_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  action_code VARCHAR(32) NOT NULL,
  action_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (audit_action_id),
  UNIQUE KEY uk_ref_audit_action_code (action_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_data_quality_category (
  data_quality_category_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_code VARCHAR(32) NOT NULL,
  category_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (data_quality_category_id),
  UNIQUE KEY uk_ref_data_quality_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_data_quality_severity (
  data_quality_severity_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  severity_code VARCHAR(32) NOT NULL,
  severity_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (data_quality_severity_id),
  UNIQUE KEY uk_ref_data_quality_severity_code (severity_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_data_quality_issue_status (
  data_quality_issue_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_open_flag TINYINT(1) NOT NULL DEFAULT 1,
  is_terminal_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (data_quality_issue_status_id),
  UNIQUE KEY uk_ref_data_quality_issue_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_data_quality_detection_source (
  data_quality_detection_source_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_code VARCHAR(32) NOT NULL,
  source_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (data_quality_detection_source_id),
  UNIQUE KEY uk_ref_dq_detection_source_code (source_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_system_event_category (
  system_event_category_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_code VARCHAR(32) NOT NULL,
  category_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (system_event_category_id),
  UNIQUE KEY uk_ref_system_event_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_system_event_severity (
  system_event_severity_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  severity_code VARCHAR(32) NOT NULL,
  severity_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (system_event_severity_id),
  UNIQUE KEY uk_ref_system_event_severity_code (severity_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_import_batch_status (
  import_batch_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_terminal_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (import_batch_status_id),
  UNIQUE KEY uk_ref_import_batch_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_import_record_status (
  import_record_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (import_record_status_id),
  UNIQUE KEY uk_ref_import_record_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Append-only. actor_user_id SET NULL if the user account is later retired.
CREATE TABLE IF NOT EXISTS audit_logs (
  audit_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  actor_user_id BIGINT UNSIGNED NULL,
  audit_action_id BIGINT UNSIGNED NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id BIGINT UNSIGNED NULL,
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  correlation_id VARCHAR(64) NULL,
  change_summary VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (audit_log_id),
  KEY idx_audit_logs_actor (actor_user_id),
  KEY idx_audit_logs_action (audit_action_id),
  KEY idx_audit_logs_entity (entity_type, entity_id),
  KEY idx_audit_logs_occurred (occurred_at),
  KEY idx_audit_logs_correlation (correlation_id),
  CONSTRAINT fk_audit_logs_actor
    FOREIGN KEY (actor_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_audit_logs_action
    FOREIGN KEY (audit_action_id) REFERENCES ref_audit_action (audit_action_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS data_quality_rules (
  data_quality_rule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(200) NOT NULL,
  description VARCHAR(1000) NULL,
  target_entity_type VARCHAR(64) NOT NULL,
  data_quality_category_id BIGINT UNSIGNED NOT NULL,
  data_quality_severity_id BIGINT UNSIGNED NOT NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (data_quality_rule_id),
  UNIQUE KEY uk_data_quality_rules_code (rule_code),
  KEY idx_data_quality_rules_category (data_quality_category_id),
  KEY idx_data_quality_rules_severity (data_quality_severity_id),
  KEY idx_data_quality_rules_status (lifecycle_status_id),
  KEY idx_data_quality_rules_target (target_entity_type),
  CONSTRAINT fk_data_quality_rules_category
    FOREIGN KEY (data_quality_category_id) REFERENCES ref_data_quality_category (data_quality_category_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_data_quality_rules_severity
    FOREIGN KEY (data_quality_severity_id) REFERENCES ref_data_quality_severity (data_quality_severity_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_data_quality_rules_lifecycle
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS data_quality_issues (
  data_quality_issue_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  data_quality_rule_id BIGINT UNSIGNED NULL,
  data_quality_category_id BIGINT UNSIGNED NOT NULL,
  data_quality_severity_id BIGINT UNSIGNED NOT NULL,
  data_quality_issue_status_id BIGINT UNSIGNED NOT NULL,
  data_quality_detection_source_id BIGINT UNSIGNED NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id BIGINT UNSIGNED NULL,
  issue_summary VARCHAR(500) NOT NULL,
  detected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  assigned_user_id BIGINT UNSIGNED NULL,
  resolved_at DATETIME NULL,
  resolved_by_user_id BIGINT UNSIGNED NULL,
  resolution_notes VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (data_quality_issue_id),
  KEY idx_dq_issues_rule (data_quality_rule_id),
  KEY idx_dq_issues_category (data_quality_category_id),
  KEY idx_dq_issues_severity (data_quality_severity_id),
  KEY idx_dq_issues_status (data_quality_issue_status_id),
  KEY idx_dq_issues_source (data_quality_detection_source_id),
  KEY idx_dq_issues_entity (entity_type, entity_id),
  KEY idx_dq_issues_detected (detected_at),
  KEY idx_dq_issues_assigned (assigned_user_id),
  KEY idx_dq_issues_resolved_by (resolved_by_user_id),
  KEY idx_dq_issues_status_detected (data_quality_issue_status_id, detected_at),
  CONSTRAINT fk_dq_issues_rule
    FOREIGN KEY (data_quality_rule_id) REFERENCES data_quality_rules (data_quality_rule_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_dq_issues_category
    FOREIGN KEY (data_quality_category_id) REFERENCES ref_data_quality_category (data_quality_category_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_dq_issues_severity
    FOREIGN KEY (data_quality_severity_id) REFERENCES ref_data_quality_severity (data_quality_severity_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_dq_issues_status
    FOREIGN KEY (data_quality_issue_status_id) REFERENCES ref_data_quality_issue_status (data_quality_issue_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_dq_issues_source
    FOREIGN KEY (data_quality_detection_source_id) REFERENCES ref_data_quality_detection_source (data_quality_detection_source_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_dq_issues_assigned
    FOREIGN KEY (assigned_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_dq_issues_resolved_by
    FOREIGN KEY (resolved_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_dq_issues_resolved_at
    CHECK (resolved_at IS NULL OR resolved_at >= detected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Append-only status history. Do not overwrite prior events.
CREATE TABLE IF NOT EXISTS data_quality_issue_events (
  data_quality_issue_event_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  data_quality_issue_id BIGINT UNSIGNED NOT NULL,
  data_quality_issue_status_id BIGINT UNSIGNED NOT NULL,
  changed_by_user_id BIGINT UNSIGNED NULL,
  changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remarks VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (data_quality_issue_event_id),
  KEY idx_dq_issue_events_issue (data_quality_issue_id),
  KEY idx_dq_issue_events_status (data_quality_issue_status_id),
  KEY idx_dq_issue_events_changed_at (changed_at),
  KEY idx_dq_issue_events_changed_by (changed_by_user_id),
  CONSTRAINT fk_dq_issue_events_issue
    FOREIGN KEY (data_quality_issue_id) REFERENCES data_quality_issues (data_quality_issue_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_dq_issue_events_status
    FOREIGN KEY (data_quality_issue_status_id) REFERENCES ref_data_quality_issue_status (data_quality_issue_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_dq_issue_events_changed_by
    FOREIGN KEY (changed_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Important operational events only. Not a high-volume application logger.
CREATE TABLE IF NOT EXISTS system_event_logs (
  system_event_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  system_event_category_id BIGINT UNSIGNED NOT NULL,
  system_event_severity_id BIGINT UNSIGNED NOT NULL,
  event_code VARCHAR(64) NOT NULL,
  actor_user_id BIGINT UNSIGNED NULL,
  entity_type VARCHAR(64) NULL,
  entity_id BIGINT UNSIGNED NULL,
  source_component VARCHAR(64) NULL,
  event_summary VARCHAR(500) NOT NULL,
  occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (system_event_log_id),
  KEY idx_system_event_logs_category (system_event_category_id),
  KEY idx_system_event_logs_severity (system_event_severity_id),
  KEY idx_system_event_logs_code (event_code),
  KEY idx_system_event_logs_actor (actor_user_id),
  KEY idx_system_event_logs_entity (entity_type, entity_id),
  KEY idx_system_event_logs_occurred (occurred_at),
  CONSTRAINT fk_system_event_logs_category
    FOREIGN KEY (system_event_category_id) REFERENCES ref_system_event_category (system_event_category_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_system_event_logs_severity
    FOREIGN KEY (system_event_severity_id) REFERENCES ref_system_event_severity (system_event_severity_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_system_event_logs_actor
    FOREIGN KEY (actor_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS import_batches (
  import_batch_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_code VARCHAR(32) NOT NULL,
  source_system VARCHAR(64) NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  import_batch_status_id BIGINT UNSIGNED NOT NULL,
  initiated_by_user_id BIGINT UNSIGNED NULL,
  row_count INT UNSIGNED NULL,
  success_count INT UNSIGNED NULL,
  failure_count INT UNSIGNED NULL,
  started_at DATETIME NULL,
  completed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (import_batch_id),
  UNIQUE KEY uk_import_batches_code (batch_code),
  KEY idx_import_batches_status (import_batch_status_id),
  KEY idx_import_batches_entity (entity_type),
  KEY idx_import_batches_started (started_at),
  KEY idx_import_batches_initiated_by (initiated_by_user_id),
  CONSTRAINT fk_import_batches_status
    FOREIGN KEY (import_batch_status_id) REFERENCES ref_import_batch_status (import_batch_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_import_batches_initiated_by
    FOREIGN KEY (initiated_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_import_batches_counts
    CHECK (row_count IS NULL OR row_count >= 0),
  CONSTRAINT ck_import_batches_success_count
    CHECK (success_count IS NULL OR success_count >= 0),
  CONSTRAINT ck_import_batches_failure_count
    CHECK (failure_count IS NULL OR failure_count >= 0),
  CONSTRAINT ck_import_batches_dates
    CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS import_records (
  import_record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  import_batch_id BIGINT UNSIGNED NOT NULL,
  source_row_number INT UNSIGNED NOT NULL,
  entity_type VARCHAR(64) NULL,
  entity_id BIGINT UNSIGNED NULL,
  import_record_status_id BIGINT UNSIGNED NOT NULL,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (import_record_id),
  UNIQUE KEY uk_import_records_batch_row (import_batch_id, source_row_number),
  KEY idx_import_records_status (import_record_status_id),
  KEY idx_import_records_entity (entity_type, entity_id),
  CONSTRAINT fk_import_records_batch
    FOREIGN KEY (import_batch_id) REFERENCES import_batches (import_batch_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_import_records_status
    FOREIGN KEY (import_record_status_id) REFERENCES ref_import_record_status (import_record_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_import_records_row_number
    CHECK (source_row_number >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_audit_action (audit_action_id, action_code, action_name, sort_order) VALUES
  (1, 'CREATE', 'Create', 1),
  (2, 'UPDATE', 'Update', 2),
  (3, 'DELETE', 'Hard delete', 3),
  (4, 'SOFT_DELETE', 'Soft delete', 4),
  (5, 'RESTORE', 'Restore', 5),
  (6, 'STATUS_CHANGE', 'Status change', 6),
  (7, 'EXPORT', 'Export', 7)
ON DUPLICATE KEY UPDATE
  action_name = VALUES(action_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_data_quality_category (data_quality_category_id, category_code, category_name, sort_order) VALUES
  (1, 'DUPLICATE', 'Duplicate data', 1),
  (2, 'MISSING', 'Missing data', 2),
  (3, 'INCONSISTENT', 'Inconsistent data', 3),
  (4, 'INVALID', 'Invalid data', 4),
  (5, 'STALE', 'Stale data', 5)
ON DUPLICATE KEY UPDATE
  category_name = VALUES(category_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_data_quality_severity (data_quality_severity_id, severity_code, severity_name, sort_order) VALUES
  (1, 'LOW', 'Low', 1),
  (2, 'MEDIUM', 'Medium', 2),
  (3, 'HIGH', 'High', 3),
  (4, 'CRITICAL', 'Critical', 4)
ON DUPLICATE KEY UPDATE
  severity_name = VALUES(severity_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_data_quality_issue_status (
  data_quality_issue_status_id, status_code, status_name, is_open_flag, is_terminal_flag, sort_order
) VALUES
  (1, 'OPEN', 'Open', 1, 0, 1),
  (2, 'ASSIGNED', 'Assigned', 1, 0, 2),
  (3, 'IN_PROGRESS', 'In progress', 1, 0, 3),
  (4, 'RESOLVED', 'Resolved', 0, 1, 4),
  (5, 'DISMISSED', 'Dismissed', 0, 1, 5),
  (6, 'REOPENED', 'Reopened', 1, 0, 6)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_open_flag = VALUES(is_open_flag),
  is_terminal_flag = VALUES(is_terminal_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_data_quality_detection_source (
  data_quality_detection_source_id, source_code, source_name, sort_order
) VALUES
  (1, 'RULE', 'Data-quality rule', 1),
  (2, 'MANUAL', 'Manual review', 2),
  (3, 'IMPORT', 'Import process', 3),
  (4, 'USER_REPORT', 'User report', 4)
ON DUPLICATE KEY UPDATE
  source_name = VALUES(source_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_system_event_category (system_event_category_id, category_code, category_name, sort_order) VALUES
  (1, 'SECURITY', 'Security', 1),
  (2, 'INTEGRATION', 'Integration', 2),
  (3, 'BATCH_JOB', 'Batch job', 3),
  (4, 'CONFIGURATION', 'Configuration', 4),
  (5, 'ACCESS', 'Access control', 5)
ON DUPLICATE KEY UPDATE
  category_name = VALUES(category_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_system_event_severity (system_event_severity_id, severity_code, severity_name, sort_order) VALUES
  (1, 'INFO', 'Info', 1),
  (2, 'WARNING', 'Warning', 2),
  (3, 'ERROR', 'Error', 3),
  (4, 'CRITICAL', 'Critical', 4)
ON DUPLICATE KEY UPDATE
  severity_name = VALUES(severity_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_import_batch_status (
  import_batch_status_id, status_code, status_name, is_terminal_flag, sort_order
) VALUES
  (1, 'PENDING', 'Pending', 0, 1),
  (2, 'RUNNING', 'Running', 0, 2),
  (3, 'COMPLETED', 'Completed', 1, 3),
  (4, 'FAILED', 'Failed', 1, 4),
  (5, 'CANCELLED', 'Cancelled', 1, 5)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_terminal_flag = VALUES(is_terminal_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_import_record_status (import_record_status_id, status_code, status_name, sort_order) VALUES
  (1, 'SUCCESS', 'Success', 1),
  (2, 'FAILED', 'Failed', 2),
  (3, 'SKIPPED', 'Skipped', 3),
  (4, 'DUPLICATE', 'Duplicate', 4)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

COMMIT;
