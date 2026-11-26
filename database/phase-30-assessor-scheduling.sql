-- A completed batch can be opened before the assessor schedules it.
ALTER TABLE assessments MODIFY COLUMN assessment_date DATE NULL;
ALTER TABLE trainee_assessments MODIFY COLUMN assessment_date DATE NULL;
ALTER TABLE batch_assessment_publications MODIFY COLUMN starts_at DATETIME NULL;
