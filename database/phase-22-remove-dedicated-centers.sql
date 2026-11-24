-- Run after clearing obsolete centre records in the selected catalogue.
-- This migration changes the allowed types. It contains no record deletion.
SELECT IF(
  NOT EXISTS(SELECT 1 FROM training_centers WHERE venue_type NOT IN ('TEMPORARY','SHARED')),
  1, JSON_EXTRACT('obsolete centre rows still exist', '$'));

SET @venue_types_schema = IF(
  EXISTS(SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_schema=DATABASE() AND table_name='training_centers'
      AND constraint_name='ck_training_centers_bookable_type'),
  'DO 0',
  'ALTER TABLE training_centers DROP CHECK ck_training_centers_venue_type,
    ALTER COLUMN venue_type SET DEFAULT ''TEMPORARY'',
    ADD CONSTRAINT ck_training_centers_bookable_type CHECK (
      (venue_type = ''SHARED'' AND provider_id IS NULL) OR
      (venue_type = ''TEMPORARY'' AND provider_id IS NOT NULL))');
PREPARE venue_types_schema FROM @venue_types_schema;
EXECUTE venue_types_schema;
DEALLOCATE PREPARE venue_types_schema;
