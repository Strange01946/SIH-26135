-- A published government requirement fixes the shared centre for its eventual reservation.
-- Older requirements can retain their existing booking history or be assigned a centre by a reviewer.
SET @government_centre_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='training_requirements' AND column_name='center_id'),
  'DO 0',
  'ALTER TABLE training_requirements ADD center_id BIGINT UNSIGNED NULL,
    ADD CONSTRAINT fk_training_requirement_center FOREIGN KEY(center_id) REFERENCES training_centers(center_id)');
PREPARE government_centre_ddl FROM @government_centre_ddl;
EXECUTE government_centre_ddl;
DEALLOCATE PREPARE government_centre_ddl;
