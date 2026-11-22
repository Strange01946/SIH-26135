# teardown_browser_assessor_fixtures.ps1
# Cleans up browser test fixtures safely from SIH26135_TEST

$ErrorActionPreference = "Continue"
$dbDatabase = if ($env:DB_NAME) { $env:DB_NAME } else { "SIH26135_TEST" }
if ($dbDatabase -ne "SIH26135_TEST") {
    throw "SAFETY CHECK FAILED: Teardown script must strictly target SIH26135_TEST, not '$dbDatabase'."
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

Write-Host "Tearing down browser assessor fixtures in $dbDatabase..." -ForegroundColor Yellow

$assessorId = Invoke-Mysql "SELECT user_id FROM users WHERE username = 'assessor_workbench_user';"
if ($assessorId) {
    $assessorId = $assessorId.Trim()
}

$assessmentId = Invoke-Mysql "SELECT assessment_id FROM assessments WHERE assessment_code = 'ASM-LIVE-ELEC-2026';"
if ($assessmentId) {
    $assessmentId = $assessmentId.Trim()
}

$trainee1Id = Invoke-Mysql "SELECT trainee_id FROM trainees WHERE registration_number = 'REG-LIVE-AARAV';"
$trainee2Id = Invoke-Mysql "SELECT trainee_id FROM trainees WHERE registration_number = 'REG-LIVE-MEERA';"

$traineeIds = @()
if ($trainee1Id) { $traineeIds += $trainee1Id.Trim() }
if ($trainee2Id) { $traineeIds += $trainee2Id.Trim() }
$traineeIn = $traineeIds -join ","

if ($traineeIn) {
    Invoke-Mysql "DELETE FROM certifications WHERE trainee_id IN ($traineeIn);"
    Invoke-Mysql "DELETE FROM assessment_results WHERE trainee_id IN ($traineeIn);"
}

if ($assessmentId) {
    Invoke-Mysql "DELETE FROM trainee_assessments WHERE assessment_id = $assessmentId;"
    Invoke-Mysql "DELETE FROM assessments WHERE assessment_id = $assessmentId;"
}

if ($traineeIn) {
    Invoke-Mysql "DELETE FROM training_enrollments WHERE trainee_id IN ($traineeIn);"
    Invoke-Mysql "DELETE FROM trainees WHERE trainee_id IN ($traineeIn);"
}

$batchId = Invoke-Mysql "SELECT batch_id FROM training_batches WHERE batch_code = 'BAT-LIVE-ELEC-01';"
if ($batchId) {
    Invoke-Mysql "DELETE FROM training_batches WHERE batch_id = $($batchId.Trim());"
}

$centerId = Invoke-Mysql "SELECT center_id FROM training_centers WHERE center_code = 'CTR-LIVE-01';"
if ($centerId) {
    Invoke-Mysql "DELETE FROM training_centers WHERE center_id = $($centerId.Trim());"
}

$providerId = Invoke-Mysql "SELECT provider_id FROM training_providers WHERE provider_code = 'PRV-LIVE-ELEC';"
if ($providerId) {
    Invoke-Mysql "DELETE FROM training_providers WHERE provider_id = $($providerId.Trim());"
}

$programId = Invoke-Mysql "SELECT program_id FROM programs WHERE program_code = 'PRG-LIVE-ELEC';"
if ($programId) {
    Invoke-Mysql "DELETE FROM programs WHERE program_id = $($programId.Trim());"
}

$schemeId = Invoke-Mysql "SELECT scheme_id FROM schemes WHERE scheme_code = 'SCH-LIVE-ELEC';"
if ($schemeId) {
    Invoke-Mysql "DELETE FROM schemes WHERE scheme_id = $($schemeId.Trim());"
}

$deptId = Invoke-Mysql "SELECT department_id FROM departments WHERE department_code = 'DEP-LIVE-ELEC';"
if ($deptId) {
    Invoke-Mysql "DELETE FROM departments WHERE department_id = $($deptId.Trim());"
}

$orgId = Invoke-Mysql "SELECT organization_id FROM organizations WHERE organization_code = 'ORG-LIVE-ELEC';"
if ($orgId) {
    Invoke-Mysql "DELETE FROM organizations WHERE organization_id = $($orgId.Trim());"
}

$courseId = Invoke-Mysql "SELECT course_id FROM courses WHERE course_code = 'CRS-LIVE-ELEC';"
if ($courseId) {
    Invoke-Mysql "DELETE FROM courses WHERE course_id = $($courseId.Trim());"
}

$sectorId = Invoke-Mysql "SELECT sector_id FROM sectors WHERE sector_code = 'SEC-LIVE-ELEC';"
if ($sectorId) {
    Invoke-Mysql "DELETE FROM sectors WHERE sector_id = $($sectorId.Trim());"
}

if ($assessorId) {
    Invoke-Mysql "DELETE FROM user_roles WHERE user_id = $assessorId;"
    Invoke-Mysql "DELETE FROM users WHERE user_id = $assessorId;"
}

Write-Host "Teardown complete!" -ForegroundColor Green

# Trainee 1005143L invariant check
$traineeCount = Invoke-Mysql "SELECT COUNT(*) FROM trainees WHERE trainee_id = 1005143 AND deleted_at IS NULL;"
$consentCount = Invoke-Mysql "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = 1005143 AND consent_status_id = 1;"
Write-Host "Invariant Check: Trainee 1005143L Count = $traineeCount, Active Consents = $consentCount" -ForegroundColor Cyan
