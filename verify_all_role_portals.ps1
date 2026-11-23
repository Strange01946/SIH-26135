# =========================================================================
# SIH-26135 — Phase 30.6B All-Role Portal Recovery & Authorization Audit
# =========================================================================

$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080"
$dbDatabase = if ($env:DB_NAME) { $env:DB_NAME } else { "SIH26135_TEST" }
if ($dbDatabase -ne "SIH26135_TEST") {
    throw "SAFETY DIRECTIVE VIOLATION: Verification must strictly run against SIH26135_TEST, not '$dbDatabase'."
}
$dbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$dbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } elseif ($env:MYSQL_PWD) { $env:MYSQL_PWD } else { "Aditya7033@" }

$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysqlBin)) {
    $mysqlCmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($mysqlCmd) { $mysqlBin = $mysqlCmd.Source }
}

function Invoke-MysqlQuery {
    param([string]$Sql)
    $env:MYSQL_PWD = $dbPassword
    try {
        $result = & $mysqlBin -u $dbUser --database=$dbDatabase -N -s -e "$Sql"
        return $result
    } finally {
        $env:MYSQL_PWD = $null
    }
}

$testResults = [System.Collections.Generic.List[PSCustomObject]]::new()

function Record-Result {
    param(
        [int]$ScenarioNumber,
        [string]$Description,
        [bool]$Success,
        [string]$Details
    )
    $obj = [PSCustomObject]@{
        Scenario = $ScenarioNumber
        Description = $Description
        Passed = $Success
        Details = $Details
    }
    $testResults.Add($obj)
    $status = if ($Success) { "PASS" } else { "FAIL" }
    $color = if ($Success) { "Green" } else { "Red" }
    Write-Host ("[{0}] Scenario {1}: {2} - {3}" -f $status, $ScenarioNumber, $Description, $Details) -ForegroundColor $color
}

function Invoke-SafeRequest {
    param(
        [string]$Uri,
        [string]$Method = "Get",
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [string]$ContentType = "application/json"
    )
    try {
        $params = @{
            Uri = $Uri
            Method = $Method
            UseBasicParsing = $true
        }
        if ($Headers.Count -gt 0) { $params.Headers = $Headers }
        if ($Body) {
            $params.Body = $Body
            $params.ContentType = $ContentType
        }
        $resp = Invoke-WebRequest @params
        $content = $resp.Content
        $statusCode = [int]$resp.StatusCode
        $parsedJson = $null
        try { $parsedJson = $content | ConvertFrom-Json } catch {}
        return [PSCustomObject]@{
            StatusCode = $statusCode
            Content = $content
            Json = $parsedJson
            Success = ($statusCode -ge 200 -and $statusCode -lt 300)
            Exception = $null
        }
    } catch [System.Net.WebException] {
        $ex = $_.Exception
        $statusCode = 0
        $content = ""
        if ($ex.Response) {
            $statusCode = [int]$ex.Response.StatusCode
            $stream = $ex.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $content = $reader.ReadToEnd()
            }
        }
        $parsedJson = $null
        try { $parsedJson = $content | ConvertFrom-Json } catch {}
        return [PSCustomObject]@{
            StatusCode = $statusCode
            Content = $content
            Json = $parsedJson
            Success = $false
            Exception = $ex
        }
    } catch {
        return [PSCustomObject]@{
            StatusCode = 0
            Content = ""
            Json = $null
            Success = $false
            Exception = $_.Exception
        }
    }
}

Write-Host "=========================================================================" -ForegroundColor Cyan
Write-Host "SIH-26135 Phase 30.6B All-Role Portal Recovery & Integrity Audit" -ForegroundColor Cyan
Write-Host "=========================================================================" -ForegroundColor Cyan

# -------------------------------------------------------------------------
# Scenario 1: Pre-Execution Trainee 1005143L Safety Invariant Check
# -------------------------------------------------------------------------
$traineeCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM trainees WHERE trainee_id = 1005143 AND deleted_at IS NULL;"
$consentCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = 1005143 AND consent_status_id = 1;"

if ([int]$traineeCount.Trim() -eq 1 -and [int]$consentCount.Trim() -eq 5) {
    Record-Result 1 "Pre-Execution Safety Invariant: Trainee 1005143L Check" $true "Trainee 1005143L exists with exactly 5 active granted DPDP consents."
} else {
    Record-Result 1 "Pre-Execution Safety Invariant: Trainee 1005143L Check" $false "Failed invariant: traineeCount=$traineeCount, consentCount=$consentCount"
}

# -------------------------------------------------------------------------
# Scenario 2: Admin Console: Authentication, Profile & User Provisioning Access
# -------------------------------------------------------------------------
$adminLoginBody = @{ usernameOrEmail = "admin_user"; password = "RawPassword123" } | ConvertTo-Json
$adminLogin = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/login" -Method "Post" -Body $adminLoginBody
$adminToken = $adminLogin.Json.data.accessToken
$adminHeaders = @{ Authorization = "Bearer $adminToken" }

$adminProfile = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/me" -Method "Get" -Headers $adminHeaders
$adminUsers = Invoke-SafeRequest -Uri "$baseUrl/api/v1/users" -Method "Get" -Headers $adminHeaders
$adminOnboarding = Invoke-SafeRequest -Uri "$baseUrl/api/v1/admin/onboarding/applications" -Method "Get" -Headers $adminHeaders

if ($adminLogin.Success -and $adminProfile.Success -and $adminUsers.Success -and $adminOnboarding.Success) {
    Record-Result 2 "Admin Console: Authentication, Profile, User Directory & Onboarding Access" $true ("Admin successfully accessed auth/me (username: {0}), /users ({1}), and onboarding applications ({2})" -f $adminProfile.Json.data.username, $adminUsers.StatusCode, $adminOnboarding.StatusCode)
} else {
    Record-Result 2 "Admin Console: Authentication, Profile, User Directory & Onboarding Access" $false ("Admin login/api failed: login={0}, profile={1}, users={2}, onboarding={3}" -f $adminLogin.StatusCode, $adminProfile.StatusCode, $adminUsers.StatusCode, $adminOnboarding.StatusCode)
}

# -------------------------------------------------------------------------
# Scenario 3: Training Provider Console: Operations API Access
# -------------------------------------------------------------------------
$tpLoginBody = @{ usernameOrEmail = "provider_test_user"; password = "RawPassword123" } | ConvertTo-Json
$tpLogin = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/login" -Method "Post" -Body $tpLoginBody
$tpToken = $tpLogin.Json.data.accessToken
$tpHeaders = @{ Authorization = "Bearer $tpToken" }

$tpCenters = Invoke-SafeRequest -Uri "$baseUrl/api/v1/training-centers" -Method "Get" -Headers $tpHeaders
$tpCourses = Invoke-SafeRequest -Uri "$baseUrl/api/v1/courses" -Method "Get" -Headers $tpHeaders
$tpBatches = Invoke-SafeRequest -Uri "$baseUrl/api/v1/training-batches" -Method "Get" -Headers $tpHeaders

if ($tpLogin.Success -and $tpCenters.Success -and $tpCourses.Success -and $tpBatches.Success) {
    Record-Result 3 "Training Provider Console: Operations API Access" $true ("TP successfully accessed centers ({0}), courses ({1}), batches ({2})" -f $tpCenters.StatusCode, $tpCourses.StatusCode, $tpBatches.StatusCode)
} else {
    Record-Result 3 "Training Provider Console: Operations API Access" $false ("TP api failed: centers={0}, courses={1}, batches={2}" -f $tpCenters.StatusCode, $tpCourses.StatusCode, $tpBatches.StatusCode)
}

# -------------------------------------------------------------------------
# Scenario 4: Assessor Workbench: Metrics & Assessment Schedules Access
# -------------------------------------------------------------------------
$assessorLoginBody = @{ usernameOrEmail = "assessor_workbench_user"; password = "RawPassword123" } | ConvertTo-Json
$assessorLogin = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/login" -Method "Post" -Body $assessorLoginBody
$assessorToken = $assessorLogin.Json.data.accessToken
$assessorHeaders = @{ Authorization = "Bearer $assessorToken" }

$assessorAssessments = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments" -Method "Get" -Headers $assessorHeaders
$assessorProfile = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/me" -Method "Get" -Headers $assessorHeaders

if ($assessorLogin.Success -and $assessorAssessments.Success -and $assessorProfile.Success) {
    $itemCount = if ($assessorAssessments.Json.data) { $assessorAssessments.Json.data.Count } else { 0 }
    Record-Result 4 "Assessor Workbench: Assigned Assessments & Profile Access" $true ("Assessor accessed assessments list (count: {0}) and auth/me ({1})" -f $itemCount, $assessorProfile.StatusCode)
} else {
    Record-Result 4 "Assessor Workbench: Assigned Assessments & Profile Access" $false ("Assessor api failed: assessments={0}, profile={1}" -f $assessorAssessments.StatusCode, $assessorProfile.StatusCode)
}

# -------------------------------------------------------------------------
# Scenario 5: Trainee Portal: Self-Service Identity & Enrolled Courses Access
# -------------------------------------------------------------------------
$traineeLoginBody = @{ usernameOrEmail = "trainee_only_user"; password = "RawPassword123" } | ConvertTo-Json
$traineeLogin = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/login" -Method "Post" -Body $traineeLoginBody
$traineeToken = $traineeLogin.Json.data.accessToken
$traineeHeaders = @{ Authorization = "Bearer $traineeToken" }

$traineeProfile = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/me" -Method "Get" -Headers $traineeHeaders

if ($traineeLogin.Success -and $traineeProfile.Success) {
    Record-Result 5 "Trainee Portal: Self-Service Authentication & Profile Access" $true ("Trainee accessed auth/me (authorities: {0})" -f ($traineeProfile.Json.data.authorities -join ", "))
} else {
    Record-Result 5 "Trainee Portal: Self-Service Authentication & Profile Access" $false ("Trainee api failed: login={0}, profile={1}" -f $traineeLogin.StatusCode, $traineeProfile.StatusCode)
}

# -------------------------------------------------------------------------
# Scenario 6: Employer Portal Authentication
# -------------------------------------------------------------------------
$empLoginBody = @{ usernameOrEmail = "emp_ed1dba73"; password = "RawPassword123" } | ConvertTo-Json
$empLogin = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/login" -Method "Post" -Body $empLoginBody
$empToken = $empLogin.Json.data.accessToken
$empHeaders = @{ Authorization = "Bearer $empToken" }

$empProfile = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/me" -Method "Get" -Headers $empHeaders

if ($empLogin.Success -and $empProfile.Success) {
    Record-Result 6 "Employer Portal: Authentication & Authoritative Profile Access" $true ("Employer accessed auth/me (authorities: {0})" -f ($empProfile.Json.data.authorities -join ", "))
} else {
    Record-Result 6 "Employer Portal: Authentication & Authoritative Profile Access" $false ("Employer api failed: login={0}, me={1}" -f $empLogin.StatusCode, $empProfile.StatusCode)
}

# -------------------------------------------------------------------------
# Scenario 7: Multi-Role User Authentication & Cross-Role Access
# -------------------------------------------------------------------------
$multiLoginBody = @{ usernameOrEmail = "multi_role_admin_assessor"; password = "RawPassword123" } | ConvertTo-Json
$multiLogin = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/login" -Method "Post" -Body $multiLoginBody
$multiToken = $multiLogin.Json.data.accessToken
$multiHeaders = @{ Authorization = "Bearer $multiToken" }

$multiAdminAccess = Invoke-SafeRequest -Uri "$baseUrl/api/v1/users" -Method "Get" -Headers $multiHeaders
$multiAssessorAccess = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments" -Method "Get" -Headers $multiHeaders

if ($multiLogin.Success -and $multiAdminAccess.Success -and $multiAssessorAccess.Success) {
    Record-Result 7 "Multi-Role Context: Admin & Assessor Authorized Workflows" $true ("Multi-role user successfully accessed both /users ({0}) and assessor/assessments ({1})" -f $multiAdminAccess.StatusCode, $multiAssessorAccess.StatusCode)
} else {
    Record-Result 7 "Multi-Role Context: Admin & Assessor Authorized Workflows" $false ("Multi-role access failed: admin={0}, assessor={1}" -f $multiAdminAccess.StatusCode, $multiAssessorAccess.StatusCode)
}

# -------------------------------------------------------------------------
# Scenario 8: Cross-Role Security Boundary: Assessor Blocked from Provider Batches
# -------------------------------------------------------------------------
$assessorBadCall = Invoke-SafeRequest -Uri "$baseUrl/api/v1/training-batches" -Method "Get" -Headers $assessorHeaders

if ($assessorBadCall.StatusCode -eq 403) {
    Record-Result 8 "Security Isolation: Assessor Forbidden from Provider Batch Operations" $true "HTTP 403 Forbidden correctly returned."
} else {
    Record-Result 8 "Security Isolation: Assessor Forbidden from Provider Batch Operations" $false ("Expected 403 Forbidden, received HTTP {0}" -f $assessorBadCall.StatusCode)
}

# -------------------------------------------------------------------------
# Scenario 9: Cross-Role Security Boundary: Trainee Blocked from Admin Users
# -------------------------------------------------------------------------
$traineeBadCall = Invoke-SafeRequest -Uri "$baseUrl/api/v1/users" -Method "Get" -Headers $traineeHeaders

if ($traineeBadCall.StatusCode -eq 403) {
    Record-Result 9 "Security Isolation: Trainee Forbidden from Admin User Management" $true "HTTP 403 Forbidden correctly returned."
} else {
    Record-Result 9 "Security Isolation: Trainee Forbidden from Admin User Management" $false ("Expected 403 Forbidden, received HTTP {0}" -f $traineeBadCall.StatusCode)
}

# -------------------------------------------------------------------------
# Scenario 10: Cross-Role Security Boundary: Trainee Blocked from Assessor Workbench
# -------------------------------------------------------------------------
$traineeScoreCall = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments" -Method "Get" -Headers $traineeHeaders

if ($traineeScoreCall.StatusCode -eq 403) {
    Record-Result 10 "Security Isolation: Trainee Forbidden from Assessor Workbench" $true "HTTP 403 Forbidden correctly returned."
} else {
    Record-Result 10 "Security Isolation: Trainee Forbidden from Assessor Workbench" $false ("Expected 403 Forbidden, received HTTP {0}" -f $traineeScoreCall.StatusCode)
}

# -------------------------------------------------------------------------
# Scenario 11: Cross-Role Security Boundary: Provider Blocked from Admin Users
# -------------------------------------------------------------------------
$tpBadCall = Invoke-SafeRequest -Uri "$baseUrl/api/v1/users" -Method "Get" -Headers $tpHeaders

if ($tpBadCall.StatusCode -eq 403) {
    Record-Result 11 "Security Isolation: Provider Forbidden from Admin User Provisioning" $true "HTTP 403 Forbidden correctly returned."
} else {
    Record-Result 11 "Security Isolation: Provider Forbidden from Admin User Provisioning" $false ("Expected 403 Forbidden, received HTTP {0}" -f $tpBadCall.StatusCode)
}

# -------------------------------------------------------------------------
# Scenario 12: Post-Execution Safety Invariant: Trainee 1005143L Invariant Check
# -------------------------------------------------------------------------
$postTraineeCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM trainees WHERE trainee_id = 1005143 AND deleted_at IS NULL;"
$postConsentCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = 1005143 AND consent_status_id = 1;"

if ([int]$postTraineeCount.Trim() -eq 1 -and [int]$postConsentCount.Trim() -eq 5) {
    Record-Result 12 "Post-Execution Safety Invariant: Trainee 1005143L Check" $true "Trainee 1005143L remains intact with exactly 5 active granted DPDP consents."
} else {
    Record-Result 12 "Post-Execution Safety Invariant: Trainee 1005143L Check" $false "Failed invariant: traineeCount=$postTraineeCount, consentCount=$postConsentCount"
}

# -------------------------------------------------------------------------
# Summary
# -------------------------------------------------------------------------
$total = $testResults.Count
$passed = ($testResults | Where-Object { $_.Passed }).Count
$failed = $total - $passed

Write-Host "`n=========================================================================" -ForegroundColor Cyan
Write-Host ("All-Role Portal Verification Completed: {0} Total, {1} Passed, {2} Failed" -f $total, $passed, $failed) -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "=========================================================================" -ForegroundColor Cyan

if ($failed -gt 0) {
    exit 1
} else {
    exit 0
}
