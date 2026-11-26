ALTER TABLE batch_assessment_publications MODIFY COLUMN venue VARCHAR(200) NULL;

SET @assessment_completion_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='batch_assessment_publications' AND column_name='completed_at'),
  'SELECT 1',
  'ALTER TABLE batch_assessment_publications ADD COLUMN completed_at DATETIME NULL'
);
PREPARE assessment_completion_ddl FROM @assessment_completion_ddl;
EXECUTE assessment_completion_ddl;
DEALLOCATE PREPARE assessment_completion_ddl;

-- Existing scored assessments had already progressed beyond the evaluation step.
UPDATE batch_assessment_publications pub
JOIN (
  SELECT ta.assessment_id, MIN(COALESCE(r.result_declared_at,r.created_at)) first_result
  FROM assessment_results r JOIN trainee_assessments ta USING (trainee_assessment_id)
  GROUP BY ta.assessment_id
) scored USING (assessment_id)
SET pub.completed_at=scored.first_result
WHERE pub.completed_at IS NULL;
