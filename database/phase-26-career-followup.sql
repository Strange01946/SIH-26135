-- Standard employment outcome questionnaire for trainee reports and staff follow-up assignments.
-- Submitted responses and their template version are retained on repeat installations.
START TRANSACTION;
SET @career_purpose = (SELECT survey_purpose_id FROM ref_survey_purpose WHERE purpose_code='EMPLOYMENT_OUTCOME');
SET @career_active = (SELECT lifecycle_status_id FROM ref_lifecycle_status WHERE status_code='ACTIVE');
SET @career_open = (SELECT survey_instance_status_id FROM ref_survey_instance_status WHERE status_code='OPEN');
SET @career_adhoc = (SELECT followup_type_id FROM ref_followup_type WHERE type_code='AD_HOC');

INSERT INTO survey_templates(template_code,template_name,survey_purpose_id,lifecycle_status_id)
VALUES ('SIH-CAREER-UPDATE','Employment outcome update',@career_purpose,@career_active)
ON DUPLICATE KEY UPDATE survey_template_id=survey_templates.survey_template_id;
SET @career_template = (SELECT survey_template_id FROM survey_templates WHERE template_code='SIH-CAREER-UPDATE');

INSERT INTO survey_template_versions(survey_template_id,version_number,version_label,effective_from,lifecycle_status_id)
VALUES (@career_template,1,'Version 1',CURRENT_DATE,@career_active)
ON DUPLICATE KEY UPDATE survey_template_version_id=survey_template_versions.survey_template_version_id;
SET @career_version = (SELECT survey_template_version_id FROM survey_template_versions
  WHERE survey_template_id=@career_template AND version_number=1);

INSERT INTO survey_questions(survey_template_version_id,question_code,question_text,question_type_id,display_order,is_required)
SELECT @career_version,q.code,q.label,qt.question_type_id,q.position,q.required
FROM (
  SELECT 'WORK_STATUS' AS code,'What is your current work or education status?' AS label,
    'SINGLE_CHOICE' AS type,1 AS position,1 AS required
  UNION ALL SELECT 'ORGANIZATION','What is the name of your employer or business, if applicable?','TEXT',2,0
  UNION ALL SELECT 'WORK_ROLE','What role, occupation or apprenticeship are you doing?','TEXT',3,0
  UNION ALL SELECT 'CHANGE_DATE','When did your current situation begin?','DATE',4,0
  UNION ALL SELECT 'MONTHLY_INCOME','What is your current monthly wage, stipend or business income in INR?','DECIMAL',5,0
  UNION ALL SELECT 'WORK_LOCATION','Where do you work or run your business? Enter a town or district.','TEXT',6,0
  UNION ALL SELECT 'SKILL_USE','Which skills from your training are you using or would you like to improve?','TEXT',7,0
  UNION ALL SELECT 'BARRIERS','What has made it difficult to find or keep work?','TEXT',8,0
) q JOIN ref_question_type qt ON qt.type_code=q.type
ON DUPLICATE KEY UPDATE survey_question_id=survey_questions.survey_question_id;

INSERT INTO survey_question_options(survey_question_id,option_code,option_label,display_order)
SELECT q.survey_question_id,es.status_code,es.status_name,es.sort_order
FROM survey_questions q CROSS JOIN ref_employment_status es
WHERE q.survey_template_version_id=@career_version AND q.question_code='WORK_STATUS'
ON DUPLICATE KEY UPDATE survey_question_option_id=survey_question_options.survey_question_option_id;

INSERT INTO surveys(survey_code,survey_name,survey_template_version_id,survey_purpose_id,
  scheduled_start_date,survey_instance_status_id)
VALUES ('SIH-CAREER-UPDATE','Employment outcome update',@career_version,@career_purpose,CURRENT_DATE,@career_open)
ON DUPLICATE KEY UPDATE survey_id=surveys.survey_id;

INSERT INTO followup_campaigns(campaign_code,campaign_name,followup_type_id,survey_id,scheduled_start_date,lifecycle_status_id)
SELECT 'SIH-CAREER-FOLLOWUP','Employment outcome follow-up',@career_adhoc,survey_id,CURRENT_DATE,@career_active
FROM surveys WHERE survey_code='SIH-CAREER-UPDATE'
ON DUPLICATE KEY UPDATE followup_campaign_id=followup_campaigns.followup_campaign_id;
COMMIT;

SET @career_recommendation_ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
    AND table_name='skill_gap_recommendations' AND column_name='rejected_on'),
  'DO 0',
  'ALTER TABLE skill_gap_recommendations ADD rejected_on DATE NULL');
PREPARE career_recommendation_ddl FROM @career_recommendation_ddl;
EXECUTE career_recommendation_ddl;
DEALLOCATE PREPARE career_recommendation_ddl;
