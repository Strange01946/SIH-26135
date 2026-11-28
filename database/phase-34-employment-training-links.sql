CREATE TABLE IF NOT EXISTS employment_training_links (
  employment_id BIGINT UNSIGNED NOT NULL,
  enrollment_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (employment_id, enrollment_id),
  KEY idx_employment_training_enrollment (enrollment_id),
  CONSTRAINT fk_employment_training_job FOREIGN KEY (employment_id)
    REFERENCES employment_records (employment_id) ON DELETE CASCADE,
  CONSTRAINT fk_employment_training_enrollment FOREIGN KEY (enrollment_id)
    REFERENCES training_enrollments (enrollment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
