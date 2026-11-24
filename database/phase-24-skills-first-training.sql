-- Requests can exist before any reporting references are assigned. Historical references remain valid.
SET @skills_requirements_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='training_requirements' AND column_name IN ('course_id','program_id') AND is_nullable='NO'),
  'ALTER TABLE training_requirements MODIFY course_id BIGINT UNSIGNED NULL, MODIFY program_id BIGINT UNSIGNED NULL',
  'DO 0');
PREPARE skills_requirements_ddl FROM @skills_requirements_ddl;
EXECUTE skills_requirements_ddl;
DEALLOCATE PREPARE skills_requirements_ddl;

SET @skills_bookings_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='venue_bookings' AND column_name IN ('course_id','program_id') AND is_nullable='NO'),
  'ALTER TABLE venue_bookings MODIFY course_id BIGINT UNSIGNED NULL, MODIFY program_id BIGINT UNSIGNED NULL',
  'DO 0');
PREPARE skills_bookings_ddl FROM @skills_bookings_ddl;
EXECUTE skills_bookings_ddl;
DEALLOCATE PREPARE skills_bookings_ddl;

-- Internal reporting references for skills-based training without a named funding scheme.
START TRANSACTION;
INSERT INTO organizations(organization_code,organization_name,organization_type_id,lifecycle_status_id)
SELECT 'SKILLS-REPORTING','Skills training reporting',organization_type_id,1
FROM ref_organization_type WHERE type_code='OTHER'
ON DUPLICATE KEY UPDATE organization_id=organizations.organization_id;

INSERT INTO departments(organization_id,department_code,department_name,lifecycle_status_id)
SELECT organization_id,'SKILLS-REPORTING','Skills training reporting',1
FROM organizations WHERE organization_code='SKILLS-REPORTING'
ON DUPLICATE KEY UPDATE department_id=departments.department_id;

INSERT INTO schemes(scheme_code,scheme_name,description,department_id,lifecycle_status_id)
SELECT 'SKILLS-REPORTING','Skills-based training','Internal reporting group for training without a named funding scheme.',d.department_id,1
FROM departments d JOIN organizations o USING (organization_id)
WHERE o.organization_code='SKILLS-REPORTING' AND d.department_code='SKILLS-REPORTING'
ON DUPLICATE KEY UPDATE scheme_id=schemes.scheme_id;

INSERT INTO programs(program_code,program_name,description,department_id,scheme_id,lifecycle_status_id)
SELECT 'SKILLS-REPORTING','Skills-based training','Internal reference for approved skills-based training batches.',department_id,scheme_id,1
FROM schemes WHERE scheme_code='SKILLS-REPORTING'
ON DUPLICATE KEY UPDATE program_id=programs.program_id;

INSERT INTO sectors(sector_code,sector_name,description,lifecycle_status_id)
VALUES ('SKILLS-UNCLASSIFIED','Skills training (unclassified)','Skills-based training with no assigned industry sector.',1)
ON DUPLICATE KEY UPDATE sector_id=sectors.sector_id;
COMMIT;
