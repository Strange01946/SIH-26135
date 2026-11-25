-- Apply after skills, assessments, trainees and training batches exist.
CREATE TABLE IF NOT EXISTS trainee_career_goals (
  trainee_id BIGINT UNSIGNED PRIMARY KEY,
  desired_job_role_id BIGINT UNSIGNED NULL,
  desired_job_title VARCHAR(200) NULL,
  revision BIGINT UNSIGNED NOT NULL DEFAULT 1,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (trainee_id) REFERENCES trainees(trainee_id) ON DELETE CASCADE,
  FOREIGN KEY (desired_job_role_id) REFERENCES job_roles(job_role_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS trainee_skills (
  trainee_id BIGINT UNSIGNED NOT NULL,
  skill_name VARCHAR(150) NOT NULL,
  skill_id BIGINT UNSIGNED NULL,
  skill_level_id BIGINT UNSIGNED NULL,
  PRIMARY KEY (trainee_id, skill_name),
  UNIQUE KEY uk_trainee_catalog_skill (trainee_id, skill_id),
  FOREIGN KEY (trainee_id) REFERENCES trainees(trainee_id) ON DELETE CASCADE,
  FOREIGN KEY (skill_id) REFERENCES skills(skill_id) ON DELETE SET NULL,
  FOREIGN KEY (skill_level_id) REFERENCES skill_levels(skill_level_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS trainee_skill_evidence (
  assessment_id BIGINT UNSIGNED NOT NULL,
  trainee_id BIGINT UNSIGNED NOT NULL,
  skill_id BIGINT UNSIGNED NOT NULL,
  skill_level_id BIGINT UNSIGNED NOT NULL,
  assessed_by BIGINT UNSIGNED NULL,
  observed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (assessment_id, trainee_id, skill_id),
  KEY ix_trainee_skill_evidence_latest (trainee_id, skill_id, observed_at),
  FOREIGN KEY (assessment_id) REFERENCES assessments(assessment_id) ON DELETE CASCADE,
  FOREIGN KEY (trainee_id) REFERENCES trainees(trainee_id) ON DELETE CASCADE,
  FOREIGN KEY (skill_id) REFERENCES skills(skill_id),
  FOREIGN KEY (skill_level_id) REFERENCES skill_levels(skill_level_id),
  FOREIGN KEY (assessed_by) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS trainee_training_interests (
  trainee_id BIGINT UNSIGNED NOT NULL,
  batch_id BIGINT UNSIGNED NOT NULL,
  decision ENUM('INTERESTED','DISMISSED') NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (trainee_id, batch_id),
  KEY ix_trainee_training_interest_count (batch_id, decision),
  FOREIGN KEY (trainee_id) REFERENCES trainees(trainee_id) ON DELETE CASCADE,
  FOREIGN KEY (batch_id) REFERENCES training_batches(batch_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
