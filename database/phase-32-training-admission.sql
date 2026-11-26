ALTER TABLE training_requirements
  ADD COLUMN minimum_education_level_id BIGINT UNSIGNED NULL,
  ADD COLUMN recommended_prerequisites VARCHAR(1000) NULL,
  ADD CONSTRAINT fk_requirement_minimum_education FOREIGN KEY (minimum_education_level_id)
    REFERENCES ref_education_level (education_level_id);

ALTER TABLE venue_booking_training_plans
  ADD COLUMN minimum_education_level_id BIGINT UNSIGNED NULL,
  ADD COLUMN recommended_prerequisites VARCHAR(1000) NULL,
  ADD CONSTRAINT fk_booking_minimum_education FOREIGN KEY (minimum_education_level_id)
    REFERENCES ref_education_level (education_level_id);
