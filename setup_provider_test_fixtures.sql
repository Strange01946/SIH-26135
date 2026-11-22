-- SIH-26135 Test Fixtures Setup for Provider Portal Workflows
-- Strictly targeting SIH26135_TEST

-- 1. Ensure Program 1 exists
INSERT INTO programs (program_id, program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
VALUES (1, 'PRG-NSDM-001', 'National Skill Development Mission', 1, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE lifecycle_status_id = 1;

-- 2. Ensure Provider 1010905 exists
INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
VALUES (1010905, 'PRV-295-029e9d8e', 'Apex Technical Academy', 'REG-APEX-001', 5, 101, 1001, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE lifecycle_status_id = 1;

-- 3. Ensure Another Provider (1021922) exists for cross-provider isolation tests
INSERT INTO training_providers (provider_id, provider_code, provider_name, registration_number, organization_type_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
VALUES (1021922, 'TP-7EC13399', 'Race Academy b44176b0', 'REG-RACE-001', 5, 101, 1001, 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE lifecycle_status_id = 1;

-- 4. Ensure Center 99887 belongs to Provider 1010905
INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
VALUES (99887, 1010905, 'CTR-295-029e9d8e', 'Connaught Place Center', 101, 1001, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE provider_id = 1010905, lifecycle_status_id = 1;

-- 5. Ensure Other Center 99991 belongs to Provider 1021922
INSERT INTO training_centers (center_id, provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
VALUES (99991, 1021922, 'CTR-OTHER-99991', 'Other Provider Center', 101, 1001, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE provider_id = 1021922, lifecycle_status_id = 1;

-- 6. Ensure Course 99895 exists and is mapped to Program 1
INSERT INTO courses (course_id, course_code, course_name, sector_id, delivery_mode_id, duration_hours, lifecycle_status_id, created_at, updated_at)
VALUES (99895, 'CRS-SOL-295', 'Solar PV Installation & Maintenance', 1, 1, 160, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE lifecycle_status_id = 1;

INSERT INTO course_programs (course_id, program_id, created_at)
VALUES (99895, 1, NOW())
ON DUPLICATE KEY UPDATE program_id = 1;

-- 7. Ensure test trainees exist with active DATA_PROCESSING consent
INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, email, phone, address_line1, pincode, state_id, district_id, education_level_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES (1005143, 'REG-LIVE-VERIFY-001', 'Aarav', 'Sharma', 1, 'live.test.trainee@example.com', '9876543210', '123 MG Road', '110001', 101, 1001, 6, 1, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE profile_status_id = 2;

INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
VALUES (1005143, 1, 'Statutory data processing consent', 1, 'v1.0', NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 'WEB_PORTAL', NOW(), NOW())
ON DUPLICATE KEY UPDATE consent_status_id = 1, revoked_at = NULL;

INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, email, phone, address_line1, pincode, state_id, district_id, education_level_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES (1005356, 'REG-295-CAND-1795000520586', 'Priya', 'Verma', 2, 'priya.verma295@example.test', '9876543211', '456 Ring Road', '110001', 101, 1001, 6, 1, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE profile_status_id = 2;

INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
VALUES (1005356, 1, 'Statutory data processing consent', 1, 'v1.0', NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 'WEB_PORTAL', NOW(), NOW())
ON DUPLICATE KEY UPDATE consent_status_id = 1, revoked_at = NULL;

INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, email, phone, address_line1, pincode, state_id, district_id, education_level_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES (1005332, 'REG-295-CAND-1794999230791', 'Vikram', 'Singh', 1, 'vikram.singh295@example.test', '9876543212', '789 Link Road', '110001', 101, 1001, 6, 1, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE profile_status_id = 2;

INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
VALUES (1005332, 1, 'Statutory data processing consent', 1, 'v1.0', NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 'WEB_PORTAL', NOW(), NOW())
ON DUPLICATE KEY UPDATE consent_status_id = 1, revoked_at = NULL;

INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, email, phone, address_line1, pincode, state_id, district_id, education_level_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES (1005380, 'REG-CONC-CAND-01', 'Arjun', 'Mehta', 1, 'arjun.mehta295@example.test', '9876543220', '201 Lake Road', '110001', 101, 1001, 6, 1, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE profile_status_id = 2;

INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
VALUES (1005380, 1, 'Statutory data processing consent', 1, 'v1.0', NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 'WEB_PORTAL', NOW(), NOW())
ON DUPLICATE KEY UPDATE consent_status_id = 1, revoked_at = NULL;

INSERT INTO trainees (trainee_id, registration_number, first_name, last_name, gender_id, email, phone, address_line1, pincode, state_id, district_id, education_level_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES (1005381, 'REG-CONC-CAND-02', 'Sunita', 'Rao', 2, 'sunita.rao295@example.test', '9876543221', '202 Lake Road', '110001', 101, 1001, 6, 1, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE profile_status_id = 2;

INSERT INTO trainee_consents (trainee_id, consent_type_id, purpose, consent_status_id, policy_version, granted_at, expires_at, captured_channel, created_at, updated_at)
VALUES (1005381, 1, 'Statutory data processing consent', 1, 'v1.0', NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), 'WEB_PORTAL', NOW(), NOW())
ON DUPLICATE KEY UPDATE consent_status_id = 1, revoked_at = NULL;

