# setup_browser_assessor_fixtures.ps1
# Provisions dedicated fixtures in SIH26135_TEST for browser visual verification

$ErrorActionPreference = "Stop"
$dbDatabase = if ($env:DB_NAME) { $env:DB_NAME } else { "SIH26135_TEST" }
if ($dbDatabase -ne "SIH26135_TEST") {
    throw "SAFETY CHECK FAILED: Setup script must strictly target SIH26135_TEST, not '$dbDatabase'."
}
$dbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$dbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } elseif ($env:MYSQL_PWD) { $env:MYSQL_PWD } else { "Aditya7033@" }

$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysqlBin)) {
    $mysqlCmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($mysqlCmd) { $mysqlBin = $mysqlCmd.Source }
}

function Invoke-Mysql {
    param([string]$Sql)
    $env:MYSQL_PWD = $dbPassword
    try {
        $result = & $mysqlBin -u $dbUser --database=$dbDatabase -N -s -e "$Sql"
        return $result
    } finally {
        $env:MYSQL_PWD = $null
    }
}

Write-Host "Setting up browser assessor fixtures in $dbDatabase..." -ForegroundColor Cyan

# Cleanup any leftover from prior runs
$checkUser = Invoke-Mysql "SELECT user_id FROM users WHERE username = 'assessor_workbench_user';"
if ($checkUser) {
    Write-Host "Cleaning up previous browser assessor fixtures..." -ForegroundColor Yellow
    & "$PSScriptRoot\teardown_browser_assessor_fixtures.ps1"
}

$argonHash = '$argon2id$v=19$m=16384,t=2,p=1$fMnBGT6OVGV8R/bIyrB+4w$X8BBsaduQAT4M8Rp2Mot6OwOnNqaXFfwsbzPb1kmoig'

# Assessor User
Invoke-Mysql @"
INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
VALUES ('assessor_workbench_user', 'assessor_workbench@test.gov.in', '$argonHash', 'argon2id', 2, NOW(), NOW());
"@
$assessorId = (Invoke-Mysql "SELECT user_id FROM users WHERE username = 'assessor_workbench_user';").Trim()

Invoke-Mysql @"
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT $assessorId, role_id, NOW() FROM roles WHERE role_code = 'assessor' LIMIT 1;
"@

# Domain Hierarchy
Invoke-Mysql @"
INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
VALUES ('SEC-LIVE-ELEC', 'Electronics & Hardware', 1, NOW(), NOW());
"@
$sectorId = (Invoke-Mysql "SELECT sector_id FROM sectors WHERE sector_code = 'SEC-LIVE-ELEC';").Trim()

Invoke-Mysql @"
INSERT INTO courses (course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
VALUES ('CRS-LIVE-ELEC', 'Field Technician — Electrical Appliances', $sectorId, 1, 1, NOW(), NOW());
"@
$courseId = (Invoke-Mysql "SELECT course_id FROM courses WHERE course_code = 'CRS-LIVE-ELEC';").Trim()

Invoke-Mysql @"
INSERT INTO organizations (organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
VALUES ('ORG-LIVE-ELEC', 'Electronics Sector Skills Council', 1, 1, NOW(), NOW());
"@
$orgId = (Invoke-Mysql "SELECT organization_id FROM organizations WHERE organization_code = 'ORG-LIVE-ELEC';").Trim()

Invoke-Mysql @"
INSERT INTO departments (organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
VALUES ($orgId, 'DEP-LIVE-ELEC', 'Consumer Electronics Division', 1, NOW(), NOW());
"@
$deptId = (Invoke-Mysql "SELECT department_id FROM departments WHERE department_code = 'DEP-LIVE-ELEC';").Trim()

Invoke-Mysql @"
INSERT INTO schemes (scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
VALUES ('SCH-LIVE-ELEC', 'National Skilling Mission for Electronics', $deptId, 'INR', 1, NOW(), NOW());
"@
$schemeId = (Invoke-Mysql "SELECT scheme_id FROM schemes WHERE scheme_code = 'SCH-LIVE-ELEC';").Trim()

Invoke-Mysql @"
INSERT INTO programs (program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
VALUES ('PRG-LIVE-ELEC', 'Advanced Technician Skilling Initiative', $deptId, $schemeId, 1, NOW(), NOW());
"@
$programId = (Invoke-Mysql "SELECT program_id FROM programs WHERE program_code = 'PRG-LIVE-ELEC';").Trim()

Invoke-Mysql @"
INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
VALUES ('PRV-LIVE-ELEC', 'Apex Technical Academy', 'REG-PRV-LIVE-ELEC', 5, $orgId, 101, 1001, 1, 1, NOW(), NOW());
"@
$providerId = (Invoke-Mysql "SELECT provider_id FROM training_providers WHERE provider_code = 'PRV-LIVE-ELEC';").Trim()

Invoke-Mysql @"
INSERT INTO training_centers (provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
VALUES ($providerId, 'CTR-LIVE-01', 'Delhi Skill Development Hub', 101, 1001, 1, NOW(), NOW());
"@
$centerId = (Invoke-Mysql "SELECT center_id FROM training_centers WHERE center_code = 'CTR-LIVE-01';").Trim()

Invoke-Mysql @"
INSERT INTO training_batches (batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
VALUES ('BAT-LIVE-ELEC-01', $courseId, $providerId, $centerId, $programId, '2026-01-01', 30, 1, NOW(), NOW());
"@
$batchId = (Invoke-Mysql "SELECT batch_id FROM training_batches WHERE batch_code = 'BAT-LIVE-ELEC-01';").Trim()

# Trainee 1 (Aarav Sharma)
Invoke-Mysql @"
INSERT INTO trainees (registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES ('REG-LIVE-AARAV', 'Aarav', 'Sharma', 1, 101, 1001, 1, 1, NOW(), NOW());
"@
$trainee1Id = (Invoke-Mysql "SELECT trainee_id FROM trainees WHERE registration_number = 'REG-LIVE-AARAV';").Trim()

Invoke-Mysql @"
INSERT INTO training_enrollments (enrollment_number, trainee_id, course_id, provider_id, center_id, batch_id, program_id, enrollment_status_id, enrollment_date, created_at, updated_at)
VALUES ('ENR-LIVE-AARAV', $trainee1Id, $courseId, $providerId, $centerId, $batchId, $programId, 1, CURDATE(), NOW(), NOW());
"@
$enrollment1Id = (Invoke-Mysql "SELECT enrollment_id FROM training_enrollments WHERE enrollment_number = 'ENR-LIVE-AARAV';").Trim()

# Trainee 2 (Meera Patel)
Invoke-Mysql @"
INSERT INTO trainees (registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES ('REG-LIVE-MEERA', 'Meera', 'Patel', 2, 101, 1001, 1, 1, NOW(), NOW());
"@
$trainee2Id = (Invoke-Mysql "SELECT trainee_id FROM trainees WHERE registration_number = 'REG-LIVE-MEERA';").Trim()

Invoke-Mysql @"
INSERT INTO training_enrollments (enrollment_number, trainee_id, course_id, provider_id, center_id, batch_id, program_id, enrollment_status_id, enrollment_date, created_at, updated_at)
VALUES ('ENR-LIVE-MEERA', $trainee2Id, $courseId, $providerId, $centerId, $batchId, $programId, 1, CURDATE(), NOW(), NOW());
"@
$enrollment2Id = (Invoke-Mysql "SELECT enrollment_id FROM training_enrollments WHERE enrollment_number = 'ENR-LIVE-MEERA';").Trim()

# Assessment assigned to Assessor
Invoke-Mysql @"
INSERT INTO assessments (assessment_code, assessment_name, course_id, batch_id, program_id, assessment_type_id, maximum_score, pass_score, assessment_date, evaluator_user_id, evaluator_name, lifecycle_status_id, created_at, updated_at)
VALUES ('ASM-LIVE-ELEC-2026', 'Field Practical & Viva Certification', $courseId, $batchId, $programId, 1, 100.00, 50.00, CURDATE(), $assessorId, 'Aarav Evaluator', 1, NOW(), NOW());
"@
$assessmentId = (Invoke-Mysql "SELECT assessment_id FROM assessments WHERE assessment_code = 'ASM-LIVE-ELEC-2026';").Trim()

Write-Host "Browser assessor fixtures provisioned successfully!" -ForegroundColor Green
Write-Host "Assessor: assessor_workbench_user / RawPassword123 (ID: $assessorId)"
Write-Host "Assessment: ASM-LIVE-ELEC-2026 (ID: $assessmentId)"
Write-Host "Trainees: Aarav Sharma ($trainee1Id), Meera Patel ($trainee2Id)"
