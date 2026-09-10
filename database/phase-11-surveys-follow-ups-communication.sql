-- Phase 11 — Surveys, follow-ups & communication
-- Dependencies: Phases 00–10
-- MySQL 8.0+
-- Do not duplicate trainees or Phase 03 consent tables. Enforce consent in the application
-- by checking trainee_consents + ref_consent_type before sending communication.
-- Do not store API keys, auth tokens, plaintext Aadhaar, or full unnecessary message bodies.
-- Historical responses and communication attempts are insert-only; do not overwrite SUBMITTED surveys.

USE SIH26135;

CREATE TABLE IF NOT EXISTS ref_question_type (
  question_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(32) NOT NULL,
  type_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (question_type_id),
  UNIQUE KEY uk_ref_question_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_survey_purpose (
  survey_purpose_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  purpose_code VARCHAR(32) NOT NULL,
  purpose_name VARCHAR(150) NOT NULL,
  followup_offset_months SMALLINT UNSIGNED NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_purpose_id),
  UNIQUE KEY uk_ref_survey_purpose_code (purpose_code),
  CONSTRAINT ck_ref_survey_purpose_offset
    CHECK (
      followup_offset_months IS NULL
      OR followup_offset_months IN (3, 6, 12, 24, 36)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_survey_instance_status (
  survey_instance_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_instance_status_id),
  UNIQUE KEY uk_ref_survey_instance_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_survey_response_status (
  survey_response_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_submitted_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_response_status_id),
  UNIQUE KEY uk_ref_survey_response_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_followup_type (
  followup_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(32) NOT NULL,
  type_name VARCHAR(150) NOT NULL,
  followup_offset_months SMALLINT UNSIGNED NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (followup_type_id),
  UNIQUE KEY uk_ref_followup_type_code (type_code),
  CONSTRAINT ck_ref_followup_type_offset
    CHECK (
      followup_offset_months IS NULL
      OR followup_offset_months IN (3, 6, 12, 24, 36)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_followup_status (
  followup_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_open_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (followup_status_id),
  UNIQUE KEY uk_ref_followup_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_followup_outcome (
  followup_outcome_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  outcome_code VARCHAR(32) NOT NULL,
  outcome_name VARCHAR(150) NOT NULL,
  is_success_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_unreachable_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_no_response_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (followup_outcome_id),
  UNIQUE KEY uk_ref_followup_outcome_code (outcome_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_non_response_reason (
  non_response_reason_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reason_code VARCHAR(32) NOT NULL,
  reason_name VARCHAR(150) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (non_response_reason_id),
  UNIQUE KEY uk_ref_non_response_reason_code (reason_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_communication_channel (
  communication_channel_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  channel_code VARCHAR(32) NOT NULL,
  channel_name VARCHAR(100) NOT NULL,
  required_consent_type_id BIGINT UNSIGNED NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (communication_channel_id),
  UNIQUE KEY uk_ref_communication_channel_code (channel_code),
  KEY idx_ref_communication_channel_consent (required_consent_type_id),
  CONSTRAINT fk_ref_communication_channel_consent
    FOREIGN KEY (required_consent_type_id) REFERENCES ref_consent_type (consent_type_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_communication_direction (
  communication_direction_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  direction_code VARCHAR(32) NOT NULL,
  direction_name VARCHAR(100) NOT NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (communication_direction_id),
  UNIQUE KEY uk_ref_communication_direction_code (direction_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_communication_purpose (
  communication_purpose_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  purpose_code VARCHAR(32) NOT NULL,
  purpose_name VARCHAR(150) NOT NULL,
  required_consent_type_id BIGINT UNSIGNED NULL,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (communication_purpose_id),
  UNIQUE KEY uk_ref_communication_purpose_code (purpose_code),
  KEY idx_ref_communication_purpose_consent (required_consent_type_id),
  CONSTRAINT fk_ref_communication_purpose_consent
    FOREIGN KEY (required_consent_type_id) REFERENCES ref_consent_type (consent_type_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ref_communication_status (
  communication_status_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status_code VARCHAR(32) NOT NULL,
  status_name VARCHAR(100) NOT NULL,
  is_success_flag TINYINT(1) NOT NULL DEFAULT 0,
  is_failure_flag TINYINT(1) NOT NULL DEFAULT 0,
  sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (communication_status_id),
  UNIQUE KEY uk_ref_communication_status_code (status_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Preferred channel only. Phone/email remain on trainees. Consent remains on trainee_consents.
CREATE TABLE IF NOT EXISTS trainee_channel_preferences (
  trainee_id BIGINT UNSIGNED NOT NULL,
  preferred_channel_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (trainee_id),
  KEY idx_trainee_channel_preferences_channel (preferred_channel_id),
  CONSTRAINT fk_trainee_channel_preferences_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_trainee_channel_preferences_channel
    FOREIGN KEY (preferred_channel_id) REFERENCES ref_communication_channel (communication_channel_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Survey catalog: template → version → questions. Instance (surveys) is a wave.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS survey_templates (
  survey_template_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_code VARCHAR(32) NOT NULL,
  template_name VARCHAR(200) NOT NULL,
  description VARCHAR(1000) NULL,
  survey_purpose_id BIGINT UNSIGNED NOT NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (survey_template_id),
  UNIQUE KEY uk_survey_templates_code (template_code),
  KEY idx_survey_templates_purpose (survey_purpose_id),
  KEY idx_survey_templates_status (lifecycle_status_id),
  CONSTRAINT fk_survey_templates_purpose
    FOREIGN KEY (survey_purpose_id) REFERENCES ref_survey_purpose (survey_purpose_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_survey_templates_lifecycle
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS survey_template_versions (
  survey_template_version_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  survey_template_id BIGINT UNSIGNED NOT NULL,
  version_number SMALLINT UNSIGNED NOT NULL,
  version_label VARCHAR(32) NOT NULL,
  effective_from DATE NOT NULL,
  effective_to DATE NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_template_version_id),
  UNIQUE KEY uk_survey_template_versions (survey_template_id, version_number),
  KEY idx_survey_template_versions_status (lifecycle_status_id),
  CONSTRAINT fk_survey_template_versions_template
    FOREIGN KEY (survey_template_id) REFERENCES survey_templates (survey_template_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_survey_template_versions_lifecycle
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_survey_template_versions_number
    CHECK (version_number >= 1),
  CONSTRAINT ck_survey_template_versions_dates
    CHECK (effective_to IS NULL OR effective_to >= effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS survey_questions (
  survey_question_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  survey_template_version_id BIGINT UNSIGNED NOT NULL,
  question_code VARCHAR(32) NOT NULL,
  question_text VARCHAR(500) NOT NULL,
  question_type_id BIGINT UNSIGNED NOT NULL,
  display_order SMALLINT UNSIGNED NOT NULL,
  is_required TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_question_id),
  UNIQUE KEY uk_survey_questions_version_code (survey_template_version_id, question_code),
  UNIQUE KEY uk_survey_questions_version_order (survey_template_version_id, display_order),
  KEY idx_survey_questions_type (question_type_id),
  CONSTRAINT fk_survey_questions_version
    FOREIGN KEY (survey_template_version_id) REFERENCES survey_template_versions (survey_template_version_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_survey_questions_type
    FOREIGN KEY (question_type_id) REFERENCES ref_question_type (question_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_survey_questions_order
    CHECK (display_order >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS survey_question_options (
  survey_question_option_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  survey_question_id BIGINT UNSIGNED NOT NULL,
  option_code VARCHAR(32) NOT NULL,
  option_label VARCHAR(200) NOT NULL,
  display_order SMALLINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_question_option_id),
  UNIQUE KEY uk_survey_question_options_code (survey_question_id, option_code),
  UNIQUE KEY uk_survey_question_options_order (survey_question_id, display_order),
  CONSTRAINT fk_survey_question_options_question
    FOREIGN KEY (survey_question_id) REFERENCES survey_questions (survey_question_id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  CONSTRAINT ck_survey_question_options_order
    CHECK (display_order >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- A survey wave. Repeat 3/6/12/24/36 month follow-ups as separate survey rows.
CREATE TABLE IF NOT EXISTS surveys (
  survey_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  survey_code VARCHAR(32) NOT NULL,
  survey_name VARCHAR(200) NOT NULL,
  survey_template_version_id BIGINT UNSIGNED NOT NULL,
  survey_purpose_id BIGINT UNSIGNED NOT NULL,
  program_id BIGINT UNSIGNED NULL,
  course_id BIGINT UNSIGNED NULL,
  batch_id BIGINT UNSIGNED NULL,
  followup_offset_months SMALLINT UNSIGNED NULL,
  scheduled_start_date DATE NOT NULL,
  scheduled_end_date DATE NULL,
  survey_instance_status_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (survey_id),
  UNIQUE KEY uk_surveys_code (survey_code),
  KEY idx_surveys_version (survey_template_version_id),
  KEY idx_surveys_purpose (survey_purpose_id),
  KEY idx_surveys_program (program_id),
  KEY idx_surveys_course (course_id),
  KEY idx_surveys_batch (batch_id),
  KEY idx_surveys_scheduled (scheduled_start_date),
  KEY idx_surveys_status (survey_instance_status_id),
  CONSTRAINT fk_surveys_version
    FOREIGN KEY (survey_template_version_id) REFERENCES survey_template_versions (survey_template_version_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_surveys_purpose
    FOREIGN KEY (survey_purpose_id) REFERENCES ref_survey_purpose (survey_purpose_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_surveys_program
    FOREIGN KEY (program_id) REFERENCES programs (program_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_surveys_course
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_surveys_batch
    FOREIGN KEY (batch_id) REFERENCES training_batches (batch_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_surveys_status
    FOREIGN KEY (survey_instance_status_id) REFERENCES ref_survey_instance_status (survey_instance_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_surveys_offset
    CHECK (
      followup_offset_months IS NULL
      OR followup_offset_months IN (3, 6, 12, 24, 36)
    ),
  CONSTRAINT ck_surveys_dates
    CHECK (scheduled_end_date IS NULL OR scheduled_end_date >= scheduled_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS followup_campaigns (
  followup_campaign_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  campaign_code VARCHAR(32) NOT NULL,
  campaign_name VARCHAR(200) NOT NULL,
  followup_type_id BIGINT UNSIGNED NOT NULL,
  survey_id BIGINT UNSIGNED NULL,
  program_id BIGINT UNSIGNED NULL,
  course_id BIGINT UNSIGNED NULL,
  scheduled_start_date DATE NOT NULL,
  scheduled_end_date DATE NULL,
  lifecycle_status_id BIGINT UNSIGNED NOT NULL,
  created_by_user_id BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (followup_campaign_id),
  UNIQUE KEY uk_followup_campaigns_code (campaign_code),
  KEY idx_followup_campaigns_type (followup_type_id),
  KEY idx_followup_campaigns_survey (survey_id),
  KEY idx_followup_campaigns_program (program_id),
  KEY idx_followup_campaigns_course (course_id),
  KEY idx_followup_campaigns_scheduled (scheduled_start_date),
  KEY idx_followup_campaigns_status (lifecycle_status_id),
  KEY idx_followup_campaigns_created_by (created_by_user_id),
  CONSTRAINT fk_followup_campaigns_type
    FOREIGN KEY (followup_type_id) REFERENCES ref_followup_type (followup_type_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_followup_campaigns_survey
    FOREIGN KEY (survey_id) REFERENCES surveys (survey_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_followup_campaigns_program
    FOREIGN KEY (program_id) REFERENCES programs (program_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_followup_campaigns_course
    FOREIGN KEY (course_id) REFERENCES courses (course_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_followup_campaigns_lifecycle
    FOREIGN KEY (lifecycle_status_id) REFERENCES ref_lifecycle_status (lifecycle_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_followup_campaigns_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_followup_campaigns_dates
    CHECK (scheduled_end_date IS NULL OR scheduled_end_date >= scheduled_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- One task per trainee per campaign. Repeat waves by creating a new campaign.
CREATE TABLE IF NOT EXISTS followup_tasks (
  followup_task_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  followup_campaign_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NULL,
  placement_id BIGINT UNSIGNED NULL,
  employment_id BIGINT UNSIGNED NULL,
  survey_id BIGINT UNSIGNED NULL,
  scheduled_date DATE NOT NULL,
  next_followup_date DATE NULL,
  followup_status_id BIGINT UNSIGNED NOT NULL,
  followup_outcome_id BIGINT UNSIGNED NULL,
  non_response_reason_id BIGINT UNSIGNED NULL,
  assigned_user_id BIGINT UNSIGNED NULL,
  last_channel_id BIGINT UNSIGNED NULL,
  remarks VARCHAR(500) NULL,
  completed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (followup_task_id),
  UNIQUE KEY uk_followup_tasks_campaign_trainee (followup_campaign_id, trainee_id),
  KEY idx_followup_tasks_trainee (trainee_id),
  KEY idx_followup_tasks_enrollment (enrollment_id),
  KEY idx_followup_tasks_scheduled (scheduled_date),
  KEY idx_followup_tasks_next (next_followup_date),
  KEY idx_followup_tasks_status (followup_status_id),
  KEY idx_followup_tasks_outcome (followup_outcome_id),
  KEY idx_followup_tasks_assigned (assigned_user_id),
  KEY idx_followup_tasks_placement (placement_id),
  KEY idx_followup_tasks_employment (employment_id),
  KEY idx_followup_tasks_survey (survey_id),
  KEY idx_followup_tasks_non_response (non_response_reason_id),
  KEY idx_followup_tasks_channel (last_channel_id),
  KEY idx_followup_tasks_trainee_scheduled (trainee_id, scheduled_date),
  CONSTRAINT fk_followup_tasks_campaign
    FOREIGN KEY (followup_campaign_id) REFERENCES followup_campaigns (followup_campaign_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_followup_tasks_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_followup_tasks_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_followup_tasks_placement
    FOREIGN KEY (placement_id) REFERENCES placement_records (placement_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_followup_tasks_employment
    FOREIGN KEY (employment_id) REFERENCES employment_records (employment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_followup_tasks_survey
    FOREIGN KEY (survey_id) REFERENCES surveys (survey_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_followup_tasks_status
    FOREIGN KEY (followup_status_id) REFERENCES ref_followup_status (followup_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_followup_tasks_outcome
    FOREIGN KEY (followup_outcome_id) REFERENCES ref_followup_outcome (followup_outcome_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_followup_tasks_non_response
    FOREIGN KEY (non_response_reason_id) REFERENCES ref_non_response_reason (non_response_reason_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_followup_tasks_assigned
    FOREIGN KEY (assigned_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_followup_tasks_channel
    FOREIGN KEY (last_channel_id) REFERENCES ref_communication_channel (communication_channel_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT ck_followup_tasks_next_date
    CHECK (next_followup_date IS NULL OR next_followup_date >= scheduled_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- New attempt_number for a correction. Do not update a SUBMITTED response in place.
CREATE TABLE IF NOT EXISTS survey_responses (
  survey_response_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  survey_id BIGINT UNSIGNED NOT NULL,
  survey_template_version_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NULL,
  followup_task_id BIGINT UNSIGNED NULL,
  attempt_number SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  survey_response_status_id BIGINT UNSIGNED NOT NULL,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  submitted_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_response_id),
  UNIQUE KEY uk_survey_responses_attempt (survey_id, trainee_id, attempt_number),
  KEY idx_survey_responses_trainee (trainee_id),
  KEY idx_survey_responses_enrollment (enrollment_id),
  KEY idx_survey_responses_status (survey_response_status_id),
  KEY idx_survey_responses_submitted (submitted_at),
  KEY idx_survey_responses_followup (followup_task_id),
  KEY idx_survey_responses_version (survey_template_version_id),
  KEY idx_survey_responses_trainee_survey (trainee_id, survey_id),
  CONSTRAINT fk_survey_responses_survey
    FOREIGN KEY (survey_id) REFERENCES surveys (survey_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_survey_responses_version
    FOREIGN KEY (survey_template_version_id) REFERENCES survey_template_versions (survey_template_version_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_survey_responses_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_survey_responses_enrollment
    FOREIGN KEY (enrollment_id) REFERENCES training_enrollments (enrollment_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_survey_responses_followup
    FOREIGN KEY (followup_task_id) REFERENCES followup_tasks (followup_task_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_survey_responses_status
    FOREIGN KEY (survey_response_status_id) REFERENCES ref_survey_response_status (survey_response_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT ck_survey_responses_attempt
    CHECK (attempt_number >= 1),
  CONSTRAINT ck_survey_responses_submitted
    CHECK (submitted_at IS NULL OR submitted_at >= started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS survey_response_answers (
  survey_response_answer_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  survey_response_id BIGINT UNSIGNED NOT NULL,
  survey_question_id BIGINT UNSIGNED NOT NULL,
  selected_option_id BIGINT UNSIGNED NULL,
  answer_text VARCHAR(1000) NULL,
  numeric_value DECIMAL(12,2) NULL,
  boolean_value TINYINT(1) NULL,
  date_value DATE NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_response_answer_id),
  UNIQUE KEY uk_survey_response_answers_question (survey_response_id, survey_question_id),
  KEY idx_survey_response_answers_question (survey_question_id),
  KEY idx_survey_response_answers_option (selected_option_id),
  CONSTRAINT fk_survey_response_answers_response
    FOREIGN KEY (survey_response_id) REFERENCES survey_responses (survey_response_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_survey_response_answers_question
    FOREIGN KEY (survey_question_id) REFERENCES survey_questions (survey_question_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_survey_response_answers_option
    FOREIGN KEY (selected_option_id) REFERENCES survey_question_options (survey_question_option_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Multi-select choices. CASCADE only this junction if an answer row is removed.
CREATE TABLE IF NOT EXISTS survey_response_answer_options (
  survey_response_answer_id BIGINT UNSIGNED NOT NULL,
  survey_question_option_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_response_answer_id, survey_question_option_id),
  KEY idx_srao_option (survey_question_option_id),
  CONSTRAINT fk_srao_answer
    FOREIGN KEY (survey_response_answer_id) REFERENCES survey_response_answers (survey_response_answer_id)
    ON UPDATE RESTRICT
    ON DELETE CASCADE,
  CONSTRAINT fk_srao_option
    FOREIGN KEY (survey_question_option_id) REFERENCES survey_question_options (survey_question_option_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Contact attempts. Store template_code and provider_message_id, not secrets or full PII bodies.
CREATE TABLE IF NOT EXISTS communication_logs (
  communication_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  trainee_id BIGINT UNSIGNED NOT NULL,
  followup_task_id BIGINT UNSIGNED NULL,
  survey_id BIGINT UNSIGNED NULL,
  survey_response_id BIGINT UNSIGNED NULL,
  communication_channel_id BIGINT UNSIGNED NOT NULL,
  communication_direction_id BIGINT UNSIGNED NOT NULL,
  communication_purpose_id BIGINT UNSIGNED NOT NULL,
  communication_status_id BIGINT UNSIGNED NOT NULL,
  consent_type_id BIGINT UNSIGNED NULL,
  consent_checked_at DATETIME NULL,
  initiated_by_user_id BIGINT UNSIGNED NULL,
  provider_message_id VARCHAR(64) NULL,
  message_template_code VARCHAR(64) NULL,
  failure_reason VARCHAR(255) NULL,
  queued_at DATETIME NULL,
  sent_at DATETIME NULL,
  delivered_at DATETIME NULL,
  read_at DATETIME NULL,
  failed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (communication_log_id),
  UNIQUE KEY uk_communication_logs_provider_message (provider_message_id),
  KEY idx_communication_logs_trainee (trainee_id),
  KEY idx_communication_logs_followup (followup_task_id),
  KEY idx_communication_logs_survey (survey_id),
  KEY idx_communication_logs_response (survey_response_id),
  KEY idx_communication_logs_channel (communication_channel_id),
  KEY idx_communication_logs_status (communication_status_id),
  KEY idx_communication_logs_purpose (communication_purpose_id),
  KEY idx_communication_logs_sent (sent_at),
  KEY idx_communication_logs_trainee_channel (trainee_id, communication_channel_id),
  KEY idx_communication_logs_initiated_by (initiated_by_user_id),
  KEY idx_communication_logs_direction (communication_direction_id),
  KEY idx_communication_logs_consent (consent_type_id),
  CONSTRAINT fk_communication_logs_trainee
    FOREIGN KEY (trainee_id) REFERENCES trainees (trainee_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_communication_logs_followup
    FOREIGN KEY (followup_task_id) REFERENCES followup_tasks (followup_task_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_communication_logs_survey
    FOREIGN KEY (survey_id) REFERENCES surveys (survey_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_communication_logs_response
    FOREIGN KEY (survey_response_id) REFERENCES survey_responses (survey_response_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_communication_logs_channel
    FOREIGN KEY (communication_channel_id) REFERENCES ref_communication_channel (communication_channel_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_communication_logs_direction
    FOREIGN KEY (communication_direction_id) REFERENCES ref_communication_direction (communication_direction_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_communication_logs_purpose
    FOREIGN KEY (communication_purpose_id) REFERENCES ref_communication_purpose (communication_purpose_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_communication_logs_status
    FOREIGN KEY (communication_status_id) REFERENCES ref_communication_status (communication_status_id)
    ON UPDATE RESTRICT
    ON DELETE RESTRICT,
  CONSTRAINT fk_communication_logs_consent
    FOREIGN KEY (consent_type_id) REFERENCES ref_consent_type (consent_type_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL,
  CONSTRAINT fk_communication_logs_initiated_by
    FOREIGN KEY (initiated_by_user_id) REFERENCES users (user_id)
    ON UPDATE RESTRICT
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

START TRANSACTION;

INSERT INTO ref_question_type (question_type_id, type_code, type_name, sort_order) VALUES
  (1, 'BOOLEAN', 'Yes / No', 1),
  (2, 'SINGLE_CHOICE', 'Single choice', 2),
  (3, 'MULTI_CHOICE', 'Multiple choice', 3),
  (4, 'NUMBER', 'Integer number', 4),
  (5, 'DECIMAL', 'Decimal / salary', 5),
  (6, 'TEXT', 'Short text', 6),
  (7, 'DATE', 'Date', 7),
  (8, 'SCALE', 'Scale / rating', 8)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_survey_purpose (
  survey_purpose_id, purpose_code, purpose_name, followup_offset_months, sort_order
) VALUES
  (1, 'EMPLOYMENT_OUTCOME', 'Employment outcome', NULL, 1),
  (2, 'RETENTION_3M', 'Retention follow-up (3 months)', 3, 2),
  (3, 'RETENTION_6M', 'Retention / wage follow-up (6 months)', 6, 3),
  (4, 'RETENTION_12M', 'Retention / wage follow-up (12 months)', 12, 4),
  (5, 'RETENTION_24M', 'Retention / wage follow-up (24 months)', 24, 5),
  (6, 'RETENTION_36M', 'Retention / wage follow-up (36 months)', 36, 6),
  (7, 'PLACEMENT_CONVERSION', 'Placement-to-employment conversion', NULL, 7),
  (8, 'SKILL_USE', 'Skills used and skills needed', NULL, 8),
  (9, 'TRAINING_RELEVANCE', 'Training relevance', NULL, 9),
  (10, 'UNEMPLOYMENT', 'Unemployment reasons', NULL, 10)
ON DUPLICATE KEY UPDATE
  purpose_name = VALUES(purpose_name),
  followup_offset_months = VALUES(followup_offset_months),
  sort_order = VALUES(sort_order);

INSERT INTO ref_survey_instance_status (survey_instance_status_id, status_code, status_name, sort_order) VALUES
  (1, 'DRAFT', 'Draft', 1),
  (2, 'SCHEDULED', 'Scheduled', 2),
  (3, 'OPEN', 'Open', 3),
  (4, 'CLOSED', 'Closed', 4),
  (5, 'CANCELLED', 'Cancelled', 5)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_survey_response_status (
  survey_response_status_id, status_code, status_name, is_submitted_flag, sort_order
) VALUES
  (1, 'STARTED', 'Started', 0, 1),
  (2, 'PARTIAL', 'Partial', 0, 2),
  (3, 'SUBMITTED', 'Submitted', 1, 3),
  (4, 'EXPIRED', 'Expired', 0, 4),
  (5, 'DISCARDED', 'Discarded', 0, 5)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_submitted_flag = VALUES(is_submitted_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_followup_type (
  followup_type_id, type_code, type_name, followup_offset_months, sort_order
) VALUES
  (1, 'MONTH_3', '3-month follow-up', 3, 1),
  (2, 'MONTH_6', '6-month follow-up', 6, 2),
  (3, 'MONTH_12', '12-month follow-up', 12, 3),
  (4, 'MONTH_24', '24-month follow-up', 24, 4),
  (5, 'MONTH_36', '36-month follow-up', 36, 5),
  (6, 'PLACEMENT_CONVERSION', 'Placement-to-employment follow-up', NULL, 6),
  (7, 'AD_HOC', 'Ad-hoc follow-up', NULL, 7)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  followup_offset_months = VALUES(followup_offset_months),
  sort_order = VALUES(sort_order);

INSERT INTO ref_followup_status (
  followup_status_id, status_code, status_name, is_open_flag, sort_order
) VALUES
  (1, 'PENDING', 'Pending', 1, 1),
  (2, 'IN_PROGRESS', 'In progress', 1, 2),
  (3, 'COMPLETED', 'Completed', 0, 3),
  (4, 'DEFERRED', 'Deferred', 1, 4),
  (5, 'CANCELLED', 'Cancelled', 0, 5)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_open_flag = VALUES(is_open_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_followup_outcome (
  followup_outcome_id, outcome_code, outcome_name,
  is_success_flag, is_unreachable_flag, is_no_response_flag, sort_order
) VALUES
  (1, 'COMPLETED', 'Response completed', 1, 0, 0, 1),
  (2, 'NO_RESPONSE', 'No response', 0, 0, 1, 2),
  (3, 'UNREACHABLE', 'Unreachable', 0, 1, 0, 3),
  (4, 'REFUSED', 'Refused', 0, 0, 1, 4),
  (5, 'CONSENT_REVOKED', 'Consent revoked / not granted', 0, 0, 0, 5),
  (6, 'WRONG_CONTACT', 'Wrong or invalid contact', 0, 1, 0, 6),
  (7, 'RESCHEDULED', 'Rescheduled', 0, 0, 0, 7)
ON DUPLICATE KEY UPDATE
  outcome_name = VALUES(outcome_name),
  is_success_flag = VALUES(is_success_flag),
  is_unreachable_flag = VALUES(is_unreachable_flag),
  is_no_response_flag = VALUES(is_no_response_flag),
  sort_order = VALUES(sort_order);

INSERT INTO ref_non_response_reason (non_response_reason_id, reason_code, reason_name, sort_order) VALUES
  (1, 'NO_ANSWER', 'No answer', 1),
  (2, 'SWITCHED_OFF', 'Phone switched off', 2),
  (3, 'INVALID_NUMBER', 'Invalid number', 3),
  (4, 'EMAIL_BOUNCE', 'Email bounce', 4),
  (5, 'BUSY', 'Busy / asked to call later', 5),
  (6, 'LANGUAGE_BARRIER', 'Language barrier', 6),
  (7, 'OUT_OF_AREA', 'Out of coverage / migrated', 7),
  (8, 'OTHER', 'Other', 8)
ON DUPLICATE KEY UPDATE
  reason_name = VALUES(reason_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_communication_channel (
  communication_channel_id, channel_code, channel_name, required_consent_type_id, sort_order
) VALUES
  (1, 'SMS', 'SMS', NULL, 1),
  (2, 'WHATSAPP', 'WhatsApp', NULL, 2),
  (3, 'EMAIL', 'Email', NULL, 3),
  (4, 'PHONE', 'Phone', NULL, 4),
  (5, 'WEB', 'Web', NULL, 5),
  (6, 'MOBILE_APP', 'Mobile app', NULL, 6),
  (7, 'IN_PERSON', 'In person', NULL, 7)
ON DUPLICATE KEY UPDATE
  channel_name = VALUES(channel_name),
  sort_order = VALUES(sort_order);

UPDATE ref_communication_channel ch
JOIN ref_consent_type ct ON ct.consent_code = 'COMMUNICATION_SMS'
SET ch.required_consent_type_id = ct.consent_type_id
WHERE ch.channel_code = 'SMS';

UPDATE ref_communication_channel ch
JOIN ref_consent_type ct ON ct.consent_code = 'COMMUNICATION_WHATSAPP'
SET ch.required_consent_type_id = ct.consent_type_id
WHERE ch.channel_code = 'WHATSAPP';

UPDATE ref_communication_channel ch
JOIN ref_consent_type ct ON ct.consent_code = 'COMMUNICATION_EMAIL'
SET ch.required_consent_type_id = ct.consent_type_id
WHERE ch.channel_code = 'EMAIL';

UPDATE ref_communication_channel ch
JOIN ref_consent_type ct ON ct.consent_code IN ('COMMUNICATION_PHONE')
SET ch.required_consent_type_id = ct.consent_type_id
WHERE ch.channel_code IN ('PHONE', 'IN_PERSON');

UPDATE ref_communication_channel ch
JOIN ref_consent_type ct ON ct.consent_code = 'EMPLOYMENT_FOLLOW_UP'
SET ch.required_consent_type_id = ct.consent_type_id
WHERE ch.channel_code IN ('WEB', 'MOBILE_APP');

INSERT INTO ref_communication_direction (communication_direction_id, direction_code, direction_name, sort_order) VALUES
  (1, 'OUTBOUND', 'Outbound', 1),
  (2, 'INBOUND', 'Inbound', 2)
ON DUPLICATE KEY UPDATE
  direction_name = VALUES(direction_name),
  sort_order = VALUES(sort_order);

INSERT INTO ref_communication_purpose (
  communication_purpose_id, purpose_code, purpose_name, required_consent_type_id, sort_order
) VALUES
  (1, 'SURVEY_INVITE', 'Survey invitation', NULL, 1),
  (2, 'FOLLOWUP_REMINDER', 'Follow-up reminder', NULL, 2),
  (3, 'EMPLOYMENT_CHECK', 'Employment status check', NULL, 3),
  (4, 'WAGE_CHECK', 'Wage progression check', NULL, 4),
  (5, 'RETENTION_CHECK', 'Retention check', NULL, 5),
  (6, 'OPERATIONAL', 'Operational notification', NULL, 6)
ON DUPLICATE KEY UPDATE
  purpose_name = VALUES(purpose_name),
  sort_order = VALUES(sort_order);

UPDATE ref_communication_purpose p
JOIN ref_consent_type ct ON ct.consent_code = 'EMPLOYMENT_FOLLOW_UP'
SET p.required_consent_type_id = ct.consent_type_id
WHERE p.purpose_code IN ('SURVEY_INVITE', 'FOLLOWUP_REMINDER', 'EMPLOYMENT_CHECK', 'WAGE_CHECK', 'RETENTION_CHECK');

UPDATE ref_communication_purpose p
JOIN ref_consent_type ct ON ct.consent_code = 'DATA_PROCESSING'
SET p.required_consent_type_id = ct.consent_type_id
WHERE p.purpose_code = 'OPERATIONAL';

INSERT INTO ref_communication_status (
  communication_status_id, status_code, status_name, is_success_flag, is_failure_flag, sort_order
) VALUES
  (1, 'QUEUED', 'Queued', 0, 0, 1),
  (2, 'ATTEMPTED', 'Attempted', 0, 0, 2),
  (3, 'SENT', 'Sent', 1, 0, 3),
  (4, 'DELIVERED', 'Delivered', 1, 0, 4),
  (5, 'READ', 'Read', 1, 0, 5),
  (6, 'REPLIED', 'Replied', 1, 0, 6),
  (7, 'FAILED', 'Failed', 0, 1, 7),
  (8, 'BLOCKED_NO_CONSENT', 'Blocked — consent not granted', 0, 1, 8)
ON DUPLICATE KEY UPDATE
  status_name = VALUES(status_name),
  is_success_flag = VALUES(is_success_flag),
  is_failure_flag = VALUES(is_failure_flag),
  sort_order = VALUES(sort_order);

COMMIT;
