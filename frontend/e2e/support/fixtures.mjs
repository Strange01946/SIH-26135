import assert from 'node:assert/strict';
import { argon2Sync, randomBytes } from 'node:crypto';
import { assertRunId, quote, sql } from './database.mjs';

const roles = {
  admin: 'super_admin', govadmin: 'government_admin', stateadmin: 'state_admin', provider: 'training_provider', outsider: 'training_provider',
  assessor: 'assessor', assessor2: 'assessor', employer: 'employer', trainee: 'trainee', trainee2: 'trainee',
  applicant: 'training_provider', district: 'district_officer',
};
const publicOrganizations = {
  provider_registration: 'training_provider',
  employer_registration: 'employer',
  onboarding_district_provider: 'training_provider',
  onboarding_district_employer: 'employer',
  onboarding_state_provider: 'training_provider',
  onboarding_state_employer: 'employer',
  onboarding_outside_provider: 'training_provider',
};

function passwordHash(password) {
  const nonce = randomBytes(16);
  const digest = argon2Sync('argon2id', {
    message: Buffer.from(password), nonce, memory: 16_384, passes: 2, parallelism: 1, tagLength: 32,
  });
  const encode = (value) => value.toString('base64').replace(/=+$/, '');
  return '$argon2id$v=19$m=16384,t=2,p=1$' + encode(nonce) + '$' + encode(digest);
}

export function fixtureNames(id) {
  assertRunId(id);
  const names = [...Object.keys(roles), 'registered', 'profile_registration', 'invalid_registration', 'invalid_provisioning',
    ...Object.keys(publicOrganizations)];
  return Object.fromEntries(names.map((name) => [name, id + '_' + name]));
}

export function seedFixtures(id) {
  assertRunId(id);
  const users = fixtureNames(id);
  const password = 'Smoke!' + randomBytes(18).toString('hex');
  const hash = quote(passwordHash(password));
  const candidateCode = id + '-learner';
  const enrollmentCandidate = id + '-enroll';
  const uploadCandidate = id + '-upload';
  const batchCode = id + '-venue-enroll';
  const reference = sql(`
    SELECT
      (SELECT program_id FROM programs WHERE deleted_at IS NULL AND lifecycle_status_id=1 ORDER BY program_id LIMIT 1),
      (SELECT state_id FROM states WHERE deleted_at IS NULL AND lifecycle_status_id=1 ORDER BY state_id LIMIT 1),
      (SELECT sector_id FROM sectors WHERE deleted_at IS NULL AND lifecycle_status_id=1 ORDER BY sector_id LIMIT 1),
      (SELECT delivery_mode_id FROM ref_delivery_mode ORDER BY sort_order,delivery_mode_id LIMIT 1);
  `).split('\t').map(Number);
  assert(reference.length === 4 && reference.every((value) => Number.isSafeInteger(value) && value > 0),
    'Active program, state, sector and delivery mode for the fixture course');
  const [program, state, sector, deliveryMode] = reference;
  const insertUsers = Object.entries(roles).map(([name, role]) => `
    INSERT INTO users (username,email,password_hash,password_algo,user_status_id,training_provider_id,employer_id,state_id,district_id)
    VALUES (${quote(users[name])},${quote(users[name] + '@example.invalid')},${hash},'argon2id',${name === 'applicant' ? 1 : 2},
      ${name === 'provider' ? '@provider' : ['outsider', 'applicant'].includes(name) ? '@outsider' : 'NULL'},
      ${name === 'employer' ? '@employer' : 'NULL'},${name === 'district' ? `${state},@district` : name === 'stateadmin' ? `${state},NULL` : 'NULL,NULL'});
    SET @u_${name}=LAST_INSERT_ID();
    INSERT INTO user_roles (user_id,role_id) SELECT @u_${name},role_id FROM roles WHERE role_code=${quote(role)};
  `).join('\n');
  const data = sql(`
    START TRANSACTION;
    INSERT INTO courses(course_code,course_name,sector_id,delivery_mode_id,lifecycle_status_id)
      VALUES ('${id}-fixture-course','Fixture training course ${id}',${sector},${deliveryMode},1);
    SET @course=LAST_INSERT_ID();
    INSERT INTO course_programs(course_id,program_id) VALUES (@course,${program});
    INSERT INTO districts(state_id,district_code,district_name,lifecycle_status_id)
      VALUES (${state},${quote(id)},${quote('Smoke district ' + id)},1);
    SET @district=LAST_INSERT_ID();
    INSERT INTO job_roles (job_role_code,job_role_name,sector_id,lifecycle_status_id)
      VALUES ('${id}-role','Smoke Technician',${sector},1);
    SET @jobRole=LAST_INSERT_ID();
    INSERT INTO organizations(organization_code,organization_name,organization_type_id,state_id,district_id,lifecycle_status_id)
      VALUES ('${id}-provider','Smoke Sector Skill Council',4,${state},@district,1);
    SET @providerOrg=LAST_INSERT_ID();
    INSERT INTO organizations(organization_code,organization_name,organization_type_id,state_id,district_id,lifecycle_status_id)
      VALUES ('${id}-outsider','Smoke Outside Organization',5,${state},@district,1);
    SET @outsiderOrg=LAST_INSERT_ID();
    INSERT INTO organizations(organization_code,organization_name,organization_type_id,state_id,district_id,lifecycle_status_id)
      VALUES ('${id}-employer','Smoke Employer Organization',6,${state},@district,1);
    SET @employerOrg=LAST_INSERT_ID();
    INSERT INTO training_providers(provider_code,provider_name,registration_number,organization_type_id,state_id,district_id,accreditation_status_id,lifecycle_status_id,organization_id)
      VALUES ('${id}-provider','Smoke Training Provider','${id}-provider',5,${state},@district,4,1,@providerOrg);
    SET @provider=LAST_INSERT_ID();
    INSERT INTO training_providers(provider_code,provider_name,registration_number,organization_type_id,state_id,district_id,accreditation_status_id,lifecycle_status_id,organization_id)
      VALUES ('${id}-outsider','Smoke Outside Provider','${id}-outsider',5,${state},@district,2,1,@outsiderOrg);
    SET @outsider=LAST_INSERT_ID();
    INSERT INTO training_centers(provider_id,center_code,center_name,state_id,district_id,lifecycle_status_id,venue_type)
      VALUES (@provider,'${id}-center','Smoke Skills Centre',${state},@district,1,'TEMPORARY');
    SET @center=LAST_INSERT_ID();
    INSERT INTO employers(employer_code,employer_name,registration_number,organization_type_id,state_id,district_id,record_verification_status_id,lifecycle_status_id,organization_id)
      VALUES ('${id}-employer','Smoke Solar Services','${id}-employer',6,${state},@district,4,1,@employerOrg);
    SET @employer=LAST_INSERT_ID();
    ${insertUsers}
    INSERT INTO onboarding_applications(application_number,applicant_type,user_id,entity_type,entity_id,
      organization_name,contact_person,contact_email,status)
      VALUES ('${id}-application','TRAINING_PROVIDER',@u_applicant,'TRAINING_PROVIDER',@outsider,
        'Smoke onboarding ${id}','Smoke applicant','${id}-contact@example.invalid','PENDING');
    SET @application=LAST_INSERT_ID();
    INSERT INTO trainees(user_id,registration_number,first_name,last_name,gender_id,phone,email,state_id,district_id,
      current_employment_status_id,profile_status_id)
      VALUES (@u_trainee,'${candidateCode}','Asha','Smoke',1,'+919900000314','${id}-learner@example.invalid',${state},@district,3,3);
    SET @trainee=LAST_INSERT_ID();
    INSERT INTO trainees(registration_number,first_name,last_name,gender_id,state_id,district_id,current_employment_status_id,profile_status_id)
      VALUES ('${enrollmentCandidate}','Neel','Smoke',2,${state},@district,1,3);
    SET @prospect=LAST_INSERT_ID();
    INSERT INTO trainee_consents(trainee_id,consent_type_id,consent_status_id,purpose,policy_version,granted_at)
      VALUES (@trainee,1,1,'Smoke processing','1.0',NOW()-INTERVAL 1 DAY),
             (@prospect,1,1,'Smoke enrollment','1.0',NOW()-INTERVAL 1 DAY);
    INSERT INTO training_batches(batch_code,course_id,provider_id,center_id,program_id,start_date,end_date,capacity,batch_status_id)
      VALUES ('${id}-assess-batch',@course,@provider,@center,${program},CURDATE()-INTERVAL 30 DAY,CURDATE()-INTERVAL 1 DAY,10,3);
    SET @batch=LAST_INSERT_ID();
    INSERT INTO training_enrollments(enrollment_number,trainee_id,course_id,provider_id,center_id,batch_id,program_id,
      enrollment_status_id,enrollment_date,start_date)
      VALUES ('${id}-assess-enroll',@trainee,@course,@provider,@center,@batch,${program},1,CURDATE()-INTERVAL 30 DAY,CURDATE()-INTERVAL 30 DAY);
    SET @enrollment=LAST_INSERT_ID();
    INSERT INTO attendance_records(enrollment_id,trainee_id,batch_id,session_date,session_sequence,attendance_status_id,marked_at)
      SELECT @enrollment,@trainee,@batch,CURDATE()-INTERVAL days.n DAY,1,1,NOW()
      FROM (SELECT 5 AS n UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) days;
    INSERT INTO assessments(assessment_code,assessment_name,course_id,batch_id,program_id,assessment_type_id,maximum_score,pass_score,
      assessment_date,evaluator_user_id,evaluator_name,lifecycle_status_id)
      VALUES ('${id}-assessment','Smoke Solar Assessment',@course,@batch,${program},1,100,50,CURDATE(),@u_assessor,'Smoke assessor',1);
    SET @assessment=LAST_INSERT_ID();
    INSERT INTO employment_records(employment_number,trainee_id,enrollment_id,employer_id,job_role_id,engagement_type_id,
      employment_spell_status_id,start_date,starting_salary,salary_frequency_id,work_state_id,work_district_id,
      employment_info_source_id,record_verification_status_id)
      VALUES ('${id}-employment',@trainee,@enrollment,@employer,@jobRole,1,1,CURDATE()-INTERVAL 8 MONTH,18500,1,${state},@district,1,1);
    SET @employment=LAST_INSERT_ID();
    INSERT INTO employment_verification_requests(request_number,employment_id,trainee_id,employer_id,employment_verification_request_status_id,due_at)
      VALUES ('${id}-request',@employment,@trainee,@employer,1,NOW()+INTERVAL 5 DAY);
    SET @request=LAST_INSERT_ID();
    INSERT INTO survey_templates(template_code,template_name,survey_purpose_id,lifecycle_status_id)
      VALUES ('${id}-template','Smoke employment follow-up',1,1);
    SET @template=LAST_INSERT_ID();
    INSERT INTO survey_template_versions(survey_template_id,version_number,version_label,effective_from,lifecycle_status_id)
      VALUES (@template,1,'v1',CURDATE(),1);
    SET @version=LAST_INSERT_ID();
    INSERT INTO surveys(survey_code,survey_name,survey_template_version_id,survey_purpose_id,scheduled_start_date,survey_instance_status_id)
      VALUES ('${id}-survey','Smoke Six-month Follow-up',@version,3,CURDATE(),3);
    SET @survey=LAST_INSERT_ID();
    INSERT INTO survey_questions(survey_template_version_id,question_code,question_text,question_type_id,display_order,is_required)
      VALUES (@version,'WORKING','Are you currently employed?',1,1,1);
    SET @question=LAST_INSERT_ID();
    INSERT INTO followup_campaigns(campaign_code,campaign_name,followup_type_id,survey_id,scheduled_start_date,lifecycle_status_id)
      VALUES ('${id}-campaign','Smoke Six-month Campaign',2,@survey,CURDATE(),1);
    SET @campaign=LAST_INSERT_ID();
    INSERT INTO followup_tasks(followup_campaign_id,trainee_id,scheduled_date,followup_status_id)
      VALUES (@campaign,@trainee,CURDATE(),1);
    SET @task=LAST_INSERT_ID();
    COMMIT;
    SELECT @district,@provider,@center,@employer,@trainee,@prospect,@assessment,@request,@task,@version,@question,
      CURDATE()+INTERVAL 2 DAY,CURDATE()+INTERVAL 28 DAY,
      (SELECT district_code FROM districts WHERE district_id=@district),
      (SELECT state_code FROM states WHERE state_id=${state}),@application,@course;
  `).split('\t');
  const [district, provider, center, employer, trainee, prospect, assessment, request, task, version, question] = data.slice(0, 11).map(Number);
  const course = Number(data[16]);
  assert([district, provider, center, employer, trainee, prospect, assessment, request, task, version, question, course]
    .every((value) => Number.isSafeInteger(value) && value > 0), 'All run-scoped fixture IDs were created');
  return { id, users, password, candidateCode, enrollmentCandidate, uploadCandidate, batchCode,
    course, program, state, district, provider, center, employer, trainee, prospect, assessment, request, task, version, question,
    starts: data[11], ends: data[12], districtCode: data[13], stateCode: data[14], application: Number(data[15]) };
}

export function cleanFixtures(id) {
  assertRunId(id);
  const names = fixtureNames(id);
  const users = Object.values(names).map(quote).join(',');
  const publicRegistrants = [names.registered, names.profile_registration, names.invalid_registration].map(quote).join(',');
  const publicOrganizationUsers = Object.keys(publicOrganizations).map((name) => quote(names[name])).join(',');
  const publicOrganizationNames = Object.keys(publicOrganizations).map((name) => quote(id + '-' + name.replaceAll('_', '-'))).join(',');
  const collectPublicOrganizations = Object.entries(publicOrganizations).map(([name, type]) => `
    SET @${name}_entity=(SELECT ${type}_id FROM users WHERE username=${quote(names[name])});
    SET @${name}_org=(SELECT CONCAT('ONB-',application_id) FROM onboarding_applications JOIN users USING (user_id)
      WHERE username=${quote(names[name])});
  `).join('\n');
  const removePublicOrganizations = Object.entries(publicOrganizations).map(([name, type]) => {
    const entityType = type === 'training_provider' ? 'provider' : 'employer';
    const organizationName = quote(id + '-' + name.replaceAll('_', '-'));
    return `DELETE FROM ${type}s WHERE ${entityType}_id=@${name}_entity AND ${entityType}_name=${organizationName};`;
  }).join('\n');
  const publicOrganizationCodes = Object.keys(publicOrganizations).map((name) => '@' + name + '_org').join(',');
  const candidates = ['learner', 'enroll', 'upload'].map((name) => quote(id + '-' + name)).join(',');
  const rows = sql(`
    START TRANSACTION;
    SET @provider=(SELECT provider_id FROM training_providers WHERE provider_code='${id}-provider');
    SET @employer=(SELECT employer_id FROM employers WHERE employer_code='${id}-employer');
    SET @assess=(SELECT assessment_id FROM assessments WHERE assessment_code='${id}-assessment');
    SET @employment=(SELECT employment_id FROM employment_records WHERE employment_number='${id}-employment');
    SET @verreq=(SELECT employment_verification_request_id FROM employment_verification_requests WHERE request_number='${id}-request');
    SET @survey=(SELECT survey_id FROM surveys WHERE survey_code='${id}-survey');
    SET @template=(SELECT survey_template_id FROM survey_templates WHERE template_code='${id}-template');
    SET @campaign=(SELECT followup_campaign_id FROM followup_campaigns WHERE campaign_code='${id}-campaign');
    ${collectPublicOrganizations}
    SET @venue_shared=(SELECT center_id FROM training_centers WHERE center_code='${id}-shared');
    SET @venue_career=(SELECT center_id FROM venue_bookings WHERE batch_code='${id}-venue-career'
      AND venue_type='TEMPORARY' ORDER BY booking_id DESC LIMIT 1);
    SET @venue_shared_admin=(SELECT center_id FROM training_centers WHERE center_code='${id}-center-admin' AND venue_type='SHARED');
    SET @venue_enrollment=(SELECT center_id FROM venue_bookings WHERE batch_code='${id}-venue-enroll'
      AND venue_type='TEMPORARY' AND center_id IS NOT NULL LIMIT 1);
    SET @venue_self_enrollment=(SELECT center_id FROM venue_bookings WHERE batch_code='${id}-venue-self'
      AND venue_type='TEMPORARY' AND center_id IS NOT NULL LIMIT 1);
    SET @venue_race=(SELECT center_id FROM training_centers WHERE center_code='${id}-shared-race');
    SET @venue_temporary=(SELECT center_id FROM venue_bookings WHERE batch_code='${id}-venue-temp'
      AND venue_type='TEMPORARY' AND center_id IS NOT NULL LIMIT 1);
    CREATE TEMPORARY TABLE fixture_government_bookings AS
      SELECT DISTINCT CONCAT('GOV-REQ-',requirement_id) AS batch_code FROM training_requirements
      WHERE LEFT(title,LENGTH('${id}-requirement-'))='${id}-requirement-';
    UPDATE training_requirements SET root_booking_id=NULL WHERE LEFT(title,LENGTH('${id}-requirement-'))='${id}-requirement-';
    DELETE FROM venue_blackouts WHERE center_id IN (@venue_shared,@venue_race);
    DELETE vb FROM venue_blackouts vb JOIN training_centers tc USING(center_id)
      WHERE LEFT(tc.center_code,LENGTH('${id}-gov-lab-'))='${id}-gov-lab-';
    DELETE b FROM venue_bookings b JOIN fixture_government_bookings f USING(batch_code) WHERE b.replaces_booking_id IS NOT NULL;
    DELETE b FROM venue_bookings b JOIN fixture_government_bookings f USING(batch_code);
    DELETE FROM venue_bookings WHERE LEFT(batch_code,LENGTH('${id}-venue-'))='${id}-venue-' AND replaces_booking_id IS NOT NULL;
    DELETE FROM venue_bookings WHERE LEFT(batch_code,LENGTH('${id}-venue-'))='${id}-venue-';
    DELETE a FROM training_requirement_applications a JOIN training_requirements r USING (requirement_id)
      WHERE LEFT(r.title,LENGTH('${id}-requirement-'))='${id}-requirement-';
    DELETE FROM training_requirements WHERE LEFT(title,LENGTH('${id}-requirement-'))='${id}-requirement-';
    DELETE l FROM audit_logs l JOIN users u ON u.user_id=l.actor_user_id WHERE u.username IN (${users});
    DELETE l FROM system_event_logs l JOIN users u ON u.user_id=l.actor_user_id WHERE u.username IN (${users});
    DELETE r FROM import_records r JOIN import_batches b USING (import_batch_id) JOIN users u ON u.user_id=b.initiated_by_user_id WHERE u.username IN (${users});
    DELETE b FROM import_batches b JOIN users u ON u.user_id=b.initiated_by_user_id WHERE u.username IN (${users});
    DELETE c FROM communication_logs c JOIN trainees t USING (trainee_id) WHERE t.registration_number IN (${candidates});
    DELETE q FROM communication_dispatch_attempts q JOIN communication_dispatch_jobs j USING (job_id)
      JOIN trainees t ON t.trainee_id=j.trainee_id WHERE t.registration_number IN (${candidates});
    DELETE j FROM communication_dispatch_jobs j JOIN trainees t USING (trainee_id) WHERE t.registration_number IN (${candidates});
    DELETE x FROM employment_verification_evidence x JOIN employment_verifications v USING (employment_verification_id)
      WHERE v.employment_verification_request_id=@verreq;
    DELETE FROM employment_verification_attempts WHERE employment_verification_request_id=@verreq;
    DELETE FROM employment_verifications WHERE employment_verification_request_id=@verreq;
    DELETE FROM employment_verification_requests WHERE employment_verification_request_id=@verreq;
    DELETE a FROM survey_response_answers a JOIN survey_responses r USING (survey_response_id) WHERE r.survey_id=@survey;
    DELETE a FROM survey_response_answers a JOIN survey_responses r USING (survey_response_id)
      JOIN trainees t USING (trainee_id) JOIN surveys s USING (survey_id)
      WHERE s.survey_code='SIH-CAREER-UPDATE' AND t.registration_number IN (${candidates});
    DELETE r FROM survey_responses r JOIN trainees t USING (trainee_id) JOIN surveys s USING (survey_id)
      WHERE s.survey_code='SIH-CAREER-UPDATE' AND t.registration_number IN (${candidates});
    DELETE FROM survey_responses WHERE survey_id=@survey;
    DELETE f FROM followup_tasks f JOIN trainees t USING (trainee_id) JOIN followup_campaigns c USING (followup_campaign_id)
    WHERE c.campaign_code LIKE 'SIH-CAREER-%' AND t.registration_number IN (${candidates});
    DELETE c FROM followup_campaigns c JOIN users u ON u.user_id=c.created_by_user_id
      LEFT JOIN followup_tasks t USING (followup_campaign_id)
      WHERE c.campaign_code LIKE 'SIH-CAREER-MANUAL-%' AND u.username IN (${users}) AND t.followup_task_id IS NULL;
    DELETE FROM followup_tasks WHERE followup_campaign_id=@campaign;
    DELETE FROM followup_campaigns WHERE followup_campaign_id=@campaign;
    DELETE FROM surveys WHERE survey_id=@survey;
    DELETE q FROM survey_questions q JOIN survey_template_versions v USING (survey_template_version_id) WHERE v.survey_template_id=@template;
    DELETE FROM survey_template_versions WHERE survey_template_id=@template;
    DELETE FROM survey_templates WHERE survey_template_id=@template;
    DELETE s FROM salary_history s JOIN trainees t USING (trainee_id) WHERE t.registration_number IN (${candidates});
    DELETE e FROM employment_records e JOIN trainees t USING (trainee_id) WHERE t.registration_number IN (${candidates});
    DELETE p FROM placement_records p JOIN trainees t USING (trainee_id) WHERE t.registration_number IN (${candidates});
    DELETE c FROM certifications c JOIN trainees t USING (trainee_id) WHERE t.registration_number IN (${candidates});
    DELETE r FROM assessment_results r JOIN trainees t USING (trainee_id) WHERE t.registration_number IN (${candidates});
    DELETE ta FROM trainee_assessments ta JOIN batch_assessment_publications pub USING (assessment_id)
      JOIN training_batches b ON b.batch_id=pub.batch_id WHERE b.batch_code='${id}-venue-self';
    DELETE a FROM assessments a JOIN batch_assessment_publications pub USING (assessment_id)
      JOIN training_batches b ON b.batch_id=pub.batch_id WHERE b.batch_code='${id}-venue-self';
    DELETE FROM trainee_assessments WHERE assessment_id=@assess;
    DELETE FROM assessments WHERE assessment_id=@assess;
    DELETE a FROM attendance_records a JOIN trainees t USING (trainee_id) WHERE t.registration_number IN (${candidates});
    DELETE e FROM training_enrollments e JOIN trainees t USING (trainee_id) WHERE e.provider_id=@provider AND t.registration_number IN (${candidates});
    DELETE FROM training_batches WHERE LEFT(batch_code,LENGTH('${id}-venue-'))='${id}-venue-';
    DELETE b FROM training_batches b JOIN fixture_government_bookings f USING(batch_code);
    DELETE FROM training_batches WHERE provider_id=@provider AND batch_code IN ('${id}-assess-batch','${id}-create');
    DELETE c FROM trainee_consents c JOIN trainees t USING (trainee_id) WHERE t.registration_number IN (${candidates});
    DELETE FROM trainees WHERE registration_number IN (${candidates});
    DELETE c FROM trainee_consents c JOIN trainees t USING (trainee_id) JOIN users u USING (user_id)
      WHERE u.username IN (${publicRegistrants});
    DELETE t FROM trainees t JOIN users u ON u.user_id=t.user_id WHERE u.username IN (${publicRegistrants});
    DELETE a FROM onboarding_applications a JOIN users u ON u.user_id=a.user_id
      WHERE a.application_number='${id}-application' AND u.username=${quote(fixtureNames(id).applicant)};
    DELETE a FROM onboarding_applications a JOIN users u USING (user_id) WHERE u.username IN (${publicOrganizationUsers});
    DELETE ur FROM user_roles ur JOIN users u USING (user_id) WHERE u.username IN (${users});
    DELETE FROM users WHERE username IN (${users});
    DELETE FROM training_centers WHERE center_id IN (@venue_shared,@venue_race,@venue_temporary,@venue_shared_admin,@venue_enrollment,
      @venue_career,@venue_self_enrollment);
    DELETE FROM training_centers WHERE LEFT(center_code,LENGTH('${id}-gov-lab-'))='${id}-gov-lab-';
    DELETE FROM training_centers WHERE provider_id=@provider AND center_code IN ('${id}-center','${id}-center-provider');
    DELETE c FROM training_centers c JOIN training_providers p USING (provider_id)
      WHERE c.center_code='${id}-center-admin' AND p.provider_code='${id}-outsider';
    DELETE FROM training_providers WHERE provider_code IN ('${id}-provider','${id}-outsider');
    DELETE FROM job_postings WHERE employer_id=@employer AND posting_title IN
      ('${id} career skills vacancy','${id} hiring vacancy');
    DELETE FROM employers WHERE employer_id=@employer AND employer_code='${id}-employer';
    ${removePublicOrganizations}
    DELETE FROM organizations WHERE organization_code IN ('${id}-provider','${id}-outsider','${id}-employer',${publicOrganizationCodes});
    DELETE FROM job_roles WHERE job_role_code='${id}-role' OR job_role_name='${id} Torque Technician';
    DELETE FROM skills WHERE skill_code='${id}-career-skill' OR skill_name IN ('Torque diagnostics ${id}','${id} Solar panel installation and electrical wiring');
    DELETE FROM skill_categories WHERE category_name='${id} Workshop Skills';
    DELETE c FROM courses c JOIN sectors s USING(sector_id)
      WHERE s.sector_code='SKILLS-UNCLASSIFIED' AND c.course_code LIKE 'SKL-%'
        AND LEFT(LOWER(c.description),LENGTH('${id} '))='${id} ';
    DELETE FROM courses WHERE course_code IN ('${id}-fixture-course','${id}-sql-course');
    DELETE FROM districts WHERE district_code='${id}';
    COMMIT;
    SET @remaining_government_bookings=(SELECT COUNT(*) FROM venue_bookings JOIN fixture_government_bookings USING(batch_code));
    SET @remaining_government_batches=(SELECT COUNT(*) FROM training_batches JOIN fixture_government_bookings USING(batch_code));
    SELECT (SELECT COUNT(*) FROM users WHERE username IN (${users})) +
      (SELECT COUNT(*) FROM trainees WHERE registration_number IN (${candidates})) +
      (SELECT COUNT(*) FROM training_providers WHERE provider_code IN ('${id}-provider','${id}-outsider')) +
      (SELECT COUNT(*) FROM training_centers WHERE center_code IN ('${id}-center','${id}-center-provider','${id}-center-admin')) +
      (SELECT COUNT(*) FROM venue_bookings WHERE LEFT(batch_code,LENGTH('${id}-venue-'))='${id}-venue-') +
      (SELECT COUNT(*) FROM training_batches WHERE LEFT(batch_code,LENGTH('${id}-venue-'))='${id}-venue-') +
      (SELECT COUNT(*) FROM training_centers WHERE center_id IN (@venue_shared,@venue_race,@venue_temporary,@venue_shared_admin,@venue_enrollment,@venue_career)) +
      (SELECT COUNT(*) FROM skills WHERE skill_code='${id}-career-skill' OR skill_name IN ('Torque diagnostics ${id}','${id} Solar panel installation and electrical wiring')) +
      (SELECT COUNT(*) FROM skill_categories WHERE category_name='${id} Workshop Skills') +
      (SELECT COUNT(*) FROM job_roles WHERE job_role_name='${id} Torque Technician') +
      (SELECT COUNT(*) FROM training_centers WHERE LEFT(center_code,LENGTH('${id}-gov-lab-'))='${id}-gov-lab-') +
      @remaining_government_bookings + @remaining_government_batches +
      (SELECT COUNT(*) FROM training_requirements WHERE LEFT(title,LENGTH('${id}-requirement-'))='${id}-requirement-') +
      (SELECT COUNT(*) FROM districts WHERE district_code='${id}') +
      (SELECT COUNT(*) FROM courses WHERE course_code IN ('${id}-fixture-course','${id}-sql-course')) +
      (SELECT COUNT(*) FROM courses c JOIN sectors s USING(sector_id)
        WHERE s.sector_code='SKILLS-UNCLASSIFIED' AND c.course_code LIKE 'SKL-%'
          AND LEFT(LOWER(c.description),LENGTH('${id} '))='${id} ') +
      (SELECT COUNT(*) FROM training_providers WHERE provider_name IN (${publicOrganizationNames})) +
      (SELECT COUNT(*) FROM employers WHERE employer_name IN (${publicOrganizationNames}));
  `);
  assert.equal(rows, '0', 'Run-scoped fixtures removed');
}
