-- Apply to the selected catalogue after the existing schema and role seeds.
-- Dedicated centres retain their original provider and IDs.
SET @venue_center_schema = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='training_centers' AND column_name='venue_type'),
  'DO 0',
  'ALTER TABLE training_centers MODIFY provider_id BIGINT UNSIGNED NULL,
    ADD venue_type VARCHAR(16) NOT NULL DEFAULT ''DEDICATED'',
    ADD CONSTRAINT ck_training_centers_venue_type CHECK (
      (venue_type = ''SHARED'' AND provider_id IS NULL) OR
      (venue_type IN (''DEDICATED'',''TEMPORARY'') AND provider_id IS NOT NULL))');
PREPARE venue_center_schema FROM @venue_center_schema;
EXECUTE venue_center_schema;
DEALLOCATE PREPARE venue_center_schema;

CREATE TABLE IF NOT EXISTS venue_bookings (
  booking_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  provider_id BIGINT UNSIGNED NOT NULL,
  center_id BIGINT UNSIGNED NULL,
  batch_id BIGINT UNSIGNED NULL,
  replaces_booking_id BIGINT UNSIGNED NULL,
  venue_type VARCHAR(16) NOT NULL,
  booking_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  batch_code VARCHAR(32) NOT NULL,
  course_id BIGINT UNSIGNED NOT NULL,
  program_id BIGINT UNSIGNED NOT NULL,
  seats INT UNSIGNED NOT NULL,
  venue_name VARCHAR(200) NOT NULL,
  venue_address VARCHAR(200) NULL,
  state_id BIGINT UNSIGNED NOT NULL,
  district_id BIGINT UNSIGNED NOT NULL,
  venue_capacity INT UNSIGNED NOT NULL,
  premises_permission VARCHAR(400) NULL,
  requested_by BIGINT UNSIGNED NOT NULL,
  reviewed_by BIGINT UNSIGNED NULL,
  decision_note VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY ix_venue_booking_queue (state_id,district_id,booking_status,booking_id),
  KEY ix_venue_booking_provider (provider_id,booking_id),
  KEY ix_venue_booking_center (center_id,booking_id),
  KEY ix_venue_booking_batch (batch_id,booking_status),
  KEY ix_venue_booking_replacement (replaces_booking_id,booking_status),
  CONSTRAINT fk_venue_booking_provider FOREIGN KEY(provider_id) REFERENCES training_providers(provider_id),
  CONSTRAINT fk_venue_booking_center FOREIGN KEY(center_id) REFERENCES training_centers(center_id),
  CONSTRAINT fk_venue_booking_batch FOREIGN KEY(batch_id) REFERENCES training_batches(batch_id),
  CONSTRAINT fk_venue_booking_replaces FOREIGN KEY(replaces_booking_id) REFERENCES venue_bookings(booking_id),
  CONSTRAINT fk_venue_booking_course FOREIGN KEY(course_id) REFERENCES courses(course_id),
  CONSTRAINT fk_venue_booking_program FOREIGN KEY(program_id) REFERENCES programs(program_id),
  CONSTRAINT fk_venue_booking_district FOREIGN KEY(district_id) REFERENCES districts(district_id),
  CONSTRAINT fk_venue_booking_state FOREIGN KEY(state_id) REFERENCES states(state_id),
  CONSTRAINT fk_venue_booking_requested_by FOREIGN KEY(requested_by) REFERENCES users(user_id),
  CONSTRAINT fk_venue_booking_reviewed_by FOREIGN KEY(reviewed_by) REFERENCES users(user_id),
  CONSTRAINT ck_venue_booking_type CHECK (venue_type IN ('SHARED','TEMPORARY')),
  CONSTRAINT ck_venue_booking_status CHECK (booking_status IN ('PENDING','APPROVED','REJECTED','CANCELLED','COMPLETED','SUPERSEDED')),
  CONSTRAINT ck_venue_booking_seats CHECK (seats > 0 AND venue_capacity >= seats)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS venue_booking_sessions (
  session_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  booking_id BIGINT UNSIGNED NOT NULL,
  starts_at DATETIME NOT NULL,
  ends_at DATETIME NOT NULL,
  reserved_at DATETIME NULL,
  released_at DATETIME NULL,
  UNIQUE KEY uk_venue_session_booking_start (booking_id, starts_at),
  KEY ix_venue_session_overlap (booking_id, starts_at, ends_at, released_at),
  CONSTRAINT fk_venue_session_booking FOREIGN KEY(booking_id) REFERENCES venue_bookings(booking_id) ON DELETE CASCADE,
  CONSTRAINT ck_venue_session_time CHECK (starts_at < ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS venue_blackouts (
  blackout_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  center_id BIGINT UNSIGNED NOT NULL,
  starts_at DATETIME NOT NULL,
  ends_at DATETIME NOT NULL,
  reason VARCHAR(300) NOT NULL,
  created_by BIGINT UNSIGNED NOT NULL,
  released_at DATETIME NULL,
  KEY ix_venue_blackout_overlap (center_id, starts_at, ends_at, released_at),
  CONSTRAINT fk_venue_blackout_center FOREIGN KEY(center_id) REFERENCES training_centers(center_id),
  CONSTRAINT fk_venue_blackout_creator FOREIGN KEY(created_by) REFERENCES users(user_id),
  CONSTRAINT ck_venue_blackout_time CHECK (starts_at < ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO roles(role_code,role_name,description,is_system_role,lifecycle_status_id)
VALUES ('state_admin','State Administrator','State-scoped oversight',1,1)
ON DUPLICATE KEY UPDATE role_code=VALUES(role_code);

INSERT INTO permissions(permission_code,permission_name,module_code,description) VALUES
  ('venue.booking.request','Request training venue bookings','training','Request and manage bookings for the affiliated provider'),
  ('venue.booking.review','Review training venue bookings','training','Decide venue requests within assigned jurisdiction'),
  ('venue.shared.manage','Manage shared training venues','training','Manage shared venue directory and closures within assigned jurisdiction')
ON DUPLICATE KEY UPDATE permission_name=VALUES(permission_name);

INSERT INTO role_permissions(role_id,permission_id)
SELECT r.role_id,p.permission_id FROM roles r JOIN permissions p
WHERE (r.role_code='training_provider' AND p.permission_code='venue.booking.request')
   OR (r.role_code IN ('super_admin','government_admin','state_admin','district_officer') AND p.permission_code='venue.booking.review')
   OR (r.role_code IN ('super_admin','government_admin','state_admin','district_officer') AND p.permission_code='venue.shared.manage')
ON DUPLICATE KEY UPDATE granted_at=role_permissions.granted_at;
