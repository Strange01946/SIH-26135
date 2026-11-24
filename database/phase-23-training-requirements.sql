-- Apply after the training venue schema in the selected catalogue.
CREATE TABLE IF NOT EXISTS training_requirements (
  requirement_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  skills_to_teach VARCHAR(600) NOT NULL,
  description VARCHAR(1500) NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  program_id BIGINT UNSIGNED NOT NULL,
  state_id BIGINT UNSIGNED NOT NULL,
  district_id BIGINT UNSIGNED NOT NULL,
  seats INT UNSIGNED NOT NULL,
  earliest_date DATE NOT NULL,
  latest_date DATE NOT NULL,
  daily_start_time TIME NOT NULL,
  daily_end_time TIME NOT NULL,
  requirement_status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
  published_by BIGINT UNSIGNED NOT NULL,
  published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  selected_provider_id BIGINT UNSIGNED NULL,
  selected_by BIGINT UNSIGNED NULL,
  selected_at DATETIME NULL,
  root_booking_id BIGINT UNSIGNED NULL,
  KEY ix_training_requirements_scope (state_id,district_id,requirement_status,requirement_id),
  KEY ix_training_requirements_open (requirement_status,latest_date,requirement_id),
  KEY ix_training_requirements_provider (selected_provider_id,requirement_id),
  FOREIGN KEY(course_id) REFERENCES courses(course_id),
  FOREIGN KEY(program_id) REFERENCES programs(program_id),
  FOREIGN KEY(state_id) REFERENCES states(state_id),
  FOREIGN KEY(district_id) REFERENCES districts(district_id),
  FOREIGN KEY(published_by) REFERENCES users(user_id),
  FOREIGN KEY(selected_provider_id) REFERENCES training_providers(provider_id),
  FOREIGN KEY(selected_by) REFERENCES users(user_id),
  FOREIGN KEY(root_booking_id) REFERENCES venue_bookings(booking_id),
  CHECK (seats > 0 AND earliest_date <= latest_date AND daily_start_time < daily_end_time),
  CHECK ((requirement_status='OPEN' AND selected_provider_id IS NULL AND selected_by IS NULL AND selected_at IS NULL AND root_booking_id IS NULL)
    OR (requirement_status='AWARDED' AND selected_provider_id IS NOT NULL AND selected_by IS NOT NULL AND selected_at IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS training_requirement_applications (
  application_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  requirement_id BIGINT UNSIGNED NOT NULL,
  provider_id BIGINT UNSIGNED NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  daily_start_time TIME NOT NULL,
  daily_end_time TIME NOT NULL,
  delivery_notes VARCHAR(1500) NULL,
  application_status VARCHAR(16) NOT NULL DEFAULT 'APPLIED',
  submitted_by BIGINT UNSIGNED NOT NULL,
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  selected_requirement BIGINT UNSIGNED GENERATED ALWAYS AS
    (CASE WHEN application_status='SELECTED' THEN requirement_id ELSE NULL END) STORED,
  UNIQUE KEY uk_training_requirement_provider (requirement_id,provider_id),
  UNIQUE KEY uk_training_requirement_selected (selected_requirement),
  KEY ix_training_requirement_applicant (provider_id,application_id),
  FOREIGN KEY(requirement_id) REFERENCES training_requirements(requirement_id),
  FOREIGN KEY(provider_id) REFERENCES training_providers(provider_id),
  FOREIGN KEY(submitted_by) REFERENCES users(user_id),
  CHECK (application_status IN ('APPLIED','SELECTED','NOT_SELECTED')),
  CHECK (start_date <= end_date AND daily_start_time < daily_end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS venue_booking_training_plans (
  booking_id BIGINT UNSIGNED PRIMARY KEY,
  requirement_id BIGINT UNSIGNED NULL,
  skills_to_teach VARCHAR(600) NOT NULL,
  training_notes VARCHAR(1500) NULL,
  FOREIGN KEY(booking_id) REFERENCES venue_bookings(booking_id) ON DELETE CASCADE,
  FOREIGN KEY(requirement_id) REFERENCES training_requirements(requirement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
