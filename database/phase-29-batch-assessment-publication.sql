CREATE TABLE IF NOT EXISTS batch_assessment_publications (
  assessment_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  batch_id BIGINT UNSIGNED NOT NULL UNIQUE,
  starts_at DATETIME NOT NULL,
  venue VARCHAR(200) NOT NULL,
  brief VARCHAR(1200) NOT NULL,
  published_by BIGINT UNSIGNED NOT NULL,
  checked_in_at DATETIME NULL,
  FOREIGN KEY (assessment_id) REFERENCES assessments (assessment_id) ON DELETE CASCADE,
  FOREIGN KEY (batch_id) REFERENCES training_batches (batch_id),
  FOREIGN KEY (published_by) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
