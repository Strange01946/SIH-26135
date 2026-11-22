# SIH-26135 — Phase 30.6 Assessor Workbench Runtime Verification Script
# Strict verification against SIH26135_TEST and Spring Boot Port 8080

$ErrorActionPreference = "Continue"
$baseUrl = "http://localhost:8080"
$passCount = 0
$failCount = 0
$scenarios = @()

function Record-Result {
    param(
        [int]$ScenarioNumber,
        [string]$Title,
        [bool]$Success,
        [string]$Details
    )
    $status = if ($Success) { "PASS" } else { "FAIL" }
    if ($Success) {
        $script:passCount++
        Write-Host "[$status] Scenario $ScenarioNumber : $Title" -ForegroundColor Green
    } else {
        $script:failCount++
        Write-Host "[$status] Scenario $ScenarioNumber : $Title - $Details" -ForegroundColor Red
    }
    $script:scenarios += [PSCustomObject]@{
        Scenario = $ScenarioNumber
        Title = $Title
        Status = $status
        Details = $Details
    }
}

function Invoke-SafeRequest {
    param(
        [string]$Uri,
        [string]$Method = "Get",
        [hashtable]$Headers,
        [string]$Body = $null,
        [string]$ContentType = "application/json"
    )
    try {
        $p = @{
            Uri = $Uri
            Method = $Method
            Headers = $Headers
            UseBasicParsing = $true
        }
        if ($Body) {
            $p["Body"] = $Body
            $p["ContentType"] = $ContentType
        }
        $resp = Invoke-WebRequest @p
        return @{
            StatusCode = [int]$resp.StatusCode
            Content = $resp.Content
            Raw = $resp
            Headers = $resp.Headers
        }
    } catch [System.Net.WebException] {
        $webEx = $_.Exception
        if ($webEx.Response) {
            $stream = $webEx.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errContent = $reader.ReadToEnd()
            return @{
                StatusCode = [int]$webEx.Response.StatusCode
                Content = $errContent
                Raw = $webEx.Response
                Headers = $webEx.Response.Headers
            }
        }
        throw $_
    } catch {
        throw $_
    }
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "SIH-26135 Phase 30.6 Assessor Workbench Comprehensive Verification" -ForegroundColor Cyan
Write-Host "Target: $baseUrl | Database: SIH26135_TEST" -ForegroundColor Cyan
Write-Host "=================================================================`n" -ForegroundColor Cyan

# -------------------------------------------------------------------------
# Database Safety & MySQL CLI Initialization
# -------------------------------------------------------------------------
$dbDatabase = if ($env:DB_NAME) { $env:DB_NAME } else { "SIH26135_TEST" }
if ($dbDatabase -ne "SIH26135_TEST") {
    throw "SAFETY CHECK FAILED: Verification script must strictly target SIH26135_TEST, not '$dbDatabase'."
}
$dbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$dbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } elseif ($env:MYSQL_PWD) { $env:MYSQL_PWD } else { "Aditya7033@" }

$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysqlBin)) {
    $mysqlCmd = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($mysqlCmd) { $mysqlBin = $mysqlCmd.Source }
}
if (-not (Test-Path $mysqlBin)) {
    throw "MySQL executable not found at '$mysqlBin' or on PATH."
}

function Invoke-MysqlQuery {
    param([string]$Query)
    $env:MYSQL_PWD = $dbPassword
    try {
        $result = & $mysqlBin -u $dbUser --database=$dbDatabase -N -s -e "$Query"
        return $result
    } finally {
        $env:MYSQL_PWD = $null
    }
}

# -------------------------------------------------------------------------
# Scenario 1: Pre-Execution Safety Invariant: Check Trainee 1005143L and Consents
# -------------------------------------------------------------------------
$traineeCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM trainees WHERE trainee_id = 1005143 AND deleted_at IS NULL;"
$consentCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = 1005143 AND consent_status_id = 1;"

if ([int]$traineeCount.Trim() -eq 1 -and [int]$consentCount.Trim() -eq 5) {
    Record-Result 1 "Pre-Test Authoritative Trainee 1005143L Invariant Check" $true "Trainee 1005143L exists with exactly 5 active granted DPDP consents."
} else {
    Record-Result 1 "Pre-Test Authoritative Trainee 1005143L Invariant Check" $false "Expected trainee 1005143 with 5 consents, found trainee=$traineeCount, consents=$consentCount"
}

# -------------------------------------------------------------------------
# Fixture Provisioning for Phase 30.6 Verification
# -------------------------------------------------------------------------
$runToken = [System.Guid]::NewGuid().ToString("N").Substring(0, 8)
$argonHash = '$argon2id$v=19$m=16384,t=2,p=1$fMnBGT6OVGV8R/bIyrB+4w$X8BBsaduQAT4M8Rp2Mot6OwOnNqaXFfwsbzPb1kmoig'

# Provision Assessor 1 (Assigned, user_status_id = 2 ACTIVE)
Invoke-MysqlQuery @"
INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
VALUES ('assessor_eval_$runToken', 'eval_$runToken@test.gov.in', '$argonHash', 'argon2id', 2, NOW(), NOW());
"@
$assessor1Id = (Invoke-MysqlQuery "SELECT user_id FROM users WHERE username = 'assessor_eval_$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT $assessor1Id, role_id, NOW() FROM roles WHERE role_code = 'assessor' LIMIT 1;
"@

# Provision Assessor 2 (Unassigned, user_status_id = 2 ACTIVE)
Invoke-MysqlQuery @"
INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
VALUES ('assessor_other_$runToken', 'other_$runToken@test.gov.in', '$argonHash', 'argon2id', 2, NOW(), NOW());
"@
$assessor2Id = (Invoke-MysqlQuery "SELECT user_id FROM users WHERE username = 'assessor_other_$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT $assessor2Id, role_id, NOW() FROM roles WHERE role_code = 'assessor' LIMIT 1;
"@

# Provision Trainee User 1 (For Trainee Self-Access)
Invoke-MysqlQuery @"
INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
VALUES ('trainee_user1_$runToken', 'trainee1_$runToken@test.gov.in', '$argonHash', 'argon2id', 2, NOW(), NOW());
"@
$trainee1UserId = (Invoke-MysqlQuery "SELECT user_id FROM users WHERE username = 'trainee_user1_$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT $trainee1UserId, role_id, NOW() FROM roles WHERE role_code = 'trainee' LIMIT 1;
"@

# Provision Trainee User 2 (For Cross-Trainee Access Rejection)
Invoke-MysqlQuery @"
INSERT INTO users (username, email, password_hash, password_algo, user_status_id, created_at, updated_at)
VALUES ('trainee_user2_$runToken', 'trainee2_$runToken@test.gov.in', '$argonHash', 'argon2id', 2, NOW(), NOW());
"@
$trainee2UserId = (Invoke-MysqlQuery "SELECT user_id FROM users WHERE username = 'trainee_user2_$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT $trainee2UserId, role_id, NOW() FROM roles WHERE role_code = 'trainee' LIMIT 1;
"@

# Domain Hierarchy
Invoke-MysqlQuery @"
INSERT INTO sectors (sector_code, sector_name, lifecycle_status_id, created_at, updated_at)
VALUES ('SEC-VER-$runToken', 'Power & Solar Energy $runToken', 1, NOW(), NOW());
"@
$sectorId = (Invoke-MysqlQuery "SELECT sector_id FROM sectors WHERE sector_code = 'SEC-VER-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO courses (course_code, course_name, sector_id, delivery_mode_id, lifecycle_status_id, created_at, updated_at)
VALUES ('CRS-VER-$runToken', 'Solar PV Installer $runToken', $sectorId, 1, 1, NOW(), NOW());
"@
$courseId = (Invoke-MysqlQuery "SELECT course_id FROM courses WHERE course_code = 'CRS-VER-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO organizations (organization_code, organization_name, organization_type_id, lifecycle_status_id, created_at, updated_at)
VALUES ('ORG-VER-$runToken', 'Green Skills Council $runToken', 1, 1, NOW(), NOW());
"@
$orgId = (Invoke-MysqlQuery "SELECT organization_id FROM organizations WHERE organization_code = 'ORG-VER-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO departments (organization_id, department_code, department_name, lifecycle_status_id, created_at, updated_at)
VALUES ($orgId, 'DEP-VER-$runToken', 'Renewable Energy $runToken', 1, NOW(), NOW());
"@
$deptId = (Invoke-MysqlQuery "SELECT department_id FROM departments WHERE department_code = 'DEP-VER-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO schemes (scheme_code, scheme_name, department_id, currency_code, lifecycle_status_id, created_at, updated_at)
VALUES ('SCH-VER-$runToken', 'National Solar Mission $runToken', $deptId, 'INR', 1, NOW(), NOW());
"@
$schemeId = (Invoke-MysqlQuery "SELECT scheme_id FROM schemes WHERE scheme_code = 'SCH-VER-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO programs (program_code, program_name, department_id, scheme_id, lifecycle_status_id, created_at, updated_at)
VALUES ('PRG-VER-$runToken', 'Suryamitra Accelerated Program $runToken', $deptId, $schemeId, 1, NOW(), NOW());
"@
$programId = (Invoke-MysqlQuery "SELECT program_id FROM programs WHERE program_code = 'PRG-VER-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO training_providers (provider_code, provider_name, registration_number, organization_type_id, organization_id, state_id, district_id, accreditation_status_id, lifecycle_status_id, created_at, updated_at)
VALUES ('PRV-VER-$runToken', 'Apex Solar Institute $runToken', 'REG-PRV-VER-$runToken', 5, $orgId, 101, 1001, 1, 1, NOW(), NOW());
"@
$providerId = (Invoke-MysqlQuery "SELECT provider_id FROM training_providers WHERE provider_code = 'PRV-VER-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO training_centers (provider_id, center_code, center_name, state_id, district_id, lifecycle_status_id, created_at, updated_at)
VALUES ($providerId, 'CTR-VER-$runToken', 'Apex Solar Center $runToken', 101, 1001, 1, NOW(), NOW());
"@
$centerId = (Invoke-MysqlQuery "SELECT center_id FROM training_centers WHERE center_code = 'CTR-VER-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO training_batches (batch_code, course_id, provider_id, center_id, program_id, start_date, capacity, batch_status_id, created_at, updated_at)
VALUES ('BAT-VER-$runToken', $courseId, $providerId, $centerId, $programId, '2026-01-01', 30, 1, NOW(), NOW());
"@
$batchId = (Invoke-MysqlQuery "SELECT batch_id FROM training_batches WHERE batch_code = 'BAT-VER-$runToken';").Trim()

# Trainee 1 (Will Pass - Linked to trainee_user1)
Invoke-MysqlQuery @"
INSERT INTO trainees (user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES ($trainee1UserId, 'REG-T1-$runToken', 'Ankit', 'Kumar', 1, 101, 1001, 1, 1, NOW(), NOW());
"@
$trainee1Id = (Invoke-MysqlQuery "SELECT trainee_id FROM trainees WHERE registration_number = 'REG-T1-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO training_enrollments (enrollment_number, trainee_id, course_id, provider_id, center_id, batch_id, program_id, enrollment_status_id, enrollment_date, created_at, updated_at)
VALUES ('ENR-T1-$runToken', $trainee1Id, $courseId, $providerId, $centerId, $batchId, $programId, 1, CURDATE(), NOW(), NOW());
"@
$enrollment1Id = (Invoke-MysqlQuery "SELECT enrollment_id FROM training_enrollments WHERE enrollment_number = 'ENR-T1-$runToken';").Trim()

# Trainee 2 (Will Fail - Linked to trainee_user2)
Invoke-MysqlQuery @"
INSERT INTO trainees (user_id, registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES ($trainee2UserId, 'REG-T2-$runToken', 'Sunita', 'Rani', 2, 101, 1001, 1, 1, NOW(), NOW());
"@
$trainee2Id = (Invoke-MysqlQuery "SELECT trainee_id FROM trainees WHERE registration_number = 'REG-T2-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO training_enrollments (enrollment_number, trainee_id, course_id, provider_id, center_id, batch_id, program_id, enrollment_status_id, enrollment_date, created_at, updated_at)
VALUES ('ENR-T2-$runToken', $trainee2Id, $courseId, $providerId, $centerId, $batchId, $programId, 1, CURDATE(), NOW(), NOW());
"@
$enrollment2Id = (Invoke-MysqlQuery "SELECT enrollment_id FROM training_enrollments WHERE enrollment_number = 'ENR-T2-$runToken';").Trim()

# Trainee 3 (Will Be Absent)
Invoke-MysqlQuery @"
INSERT INTO trainees (registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES ('REG-T3-$runToken', 'Manish', 'Tiwari', 1, 101, 1001, 1, 1, NOW(), NOW());
"@
$trainee3Id = (Invoke-MysqlQuery "SELECT trainee_id FROM trainees WHERE registration_number = 'REG-T3-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO training_enrollments (enrollment_number, trainee_id, course_id, provider_id, center_id, batch_id, program_id, enrollment_status_id, enrollment_date, created_at, updated_at)
VALUES ('ENR-T3-$runToken', $trainee3Id, $courseId, $providerId, $centerId, $batchId, $programId, 1, CURDATE(), NOW(), NOW());
"@
$enrollment3Id = (Invoke-MysqlQuery "SELECT enrollment_id FROM training_enrollments WHERE enrollment_number = 'ENR-T3-$runToken';").Trim()

# Assessment assigned to Assessor 1
Invoke-MysqlQuery @"
INSERT INTO assessments (assessment_code, assessment_name, course_id, batch_id, program_id, assessment_type_id, maximum_score, pass_score, assessment_date, evaluator_user_id, evaluator_name, lifecycle_status_id, created_at, updated_at)
VALUES ('ASM-VER-$runToken', 'Solar Installation & Safety Exam', $courseId, $batchId, $programId, 1, 100.00, 50.00, CURDATE(), $assessor1Id, 'Assessor Evaluator', 1, NOW(), NOW());
"@
$assessmentId = (Invoke-MysqlQuery "SELECT assessment_id FROM assessments WHERE assessment_code = 'ASM-VER-$runToken';").Trim()

# -------------------------------------------------------------------------
# Scenario 2: Assessor 1 Login & Authentication
# -------------------------------------------------------------------------
$token1 = $null
$headers1 = @{}
try {
    $loginBody = @{
        usernameOrEmail = "assessor_eval_$runToken"
        password = "RawPassword123"
    } | ConvertTo-Json
    $res = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/login" -Method "Post" -Body $loginBody
    if ($res.StatusCode -eq 200) {
        $json = $res.Content | ConvertFrom-Json
        $token1 = $json.data.accessToken
        $headers1 = @{
            "Authorization" = "Bearer $token1"
            "Content-Type" = "application/json"
        }
        Record-Result 2 "Assessor 1 Login & JWT Token Retrieval" $true "Authenticated as assessor_eval_$runToken with ROLE_assessor"
    } else {
        Record-Result 2 "Assessor 1 Login & JWT Token Retrieval" $false "HTTP $($res.StatusCode): $($res.Content)"
    }
} catch {
    Record-Result 2 "Assessor 1 Login & JWT Token Retrieval" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 3: Assessor 2 Login & Authentication
# -------------------------------------------------------------------------
$token2 = $null
$headers2 = @{}
try {
    $loginBody = @{
        usernameOrEmail = "assessor_other_$runToken"
        password = "RawPassword123"
    } | ConvertTo-Json
    $res = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/login" -Method "Post" -Body $loginBody
    if ($res.StatusCode -eq 200) {
        $json = $res.Content | ConvertFrom-Json
        $token2 = $json.data.accessToken
        $headers2 = @{
            "Authorization" = "Bearer $token2"
            "Content-Type" = "application/json"
        }
        Record-Result 3 "Assessor 2 Login & JWT Token Retrieval" $true "Authenticated as assessor_other_$runToken with ROLE_assessor"
    } else {
        Record-Result 3 "Assessor 2 Login & JWT Token Retrieval" $false "HTTP $($res.StatusCode): $($res.Content)"
    }
} catch {
    Record-Result 3 "Assessor 2 Login & JWT Token Retrieval" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Trainee 1 & 2 Login for Direct Self-Access Testing
# -------------------------------------------------------------------------
$trainee1Token = $null
$trainee1Headers = @{}
try {
    $t1LoginBody = @{
        usernameOrEmail = "trainee_user1_$runToken"
        password = "RawPassword123"
    } | ConvertTo-Json
    $res = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/login" -Method "Post" -Body $t1LoginBody
    if ($res.StatusCode -eq 200) {
        $json = $res.Content | ConvertFrom-Json
        $trainee1Token = $json.data.accessToken
        $trainee1Headers = @{
            "Authorization" = "Bearer $trainee1Token"
            "Content-Type" = "application/json"
        }
    }
} catch {}

$trainee2Token = $null
$trainee2Headers = @{}
try {
    $t2LoginBody = @{
        usernameOrEmail = "trainee_user2_$runToken"
        password = "RawPassword123"
    } | ConvertTo-Json
    $res = Invoke-SafeRequest -Uri "$baseUrl/api/v1/auth/login" -Method "Post" -Body $t2LoginBody
    if ($res.StatusCode -eq 200) {
        $json = $res.Content | ConvertFrom-Json
        $trainee2Token = $json.data.accessToken
        $trainee2Headers = @{
            "Authorization" = "Bearer $trainee2Token"
            "Content-Type" = "application/json"
        }
    }
} catch {}

# -------------------------------------------------------------------------
# Scenario 4: Fetch Assigned Assessments for Assessor 1
# -------------------------------------------------------------------------
try {
    $res = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments" -Method "Get" -Headers $headers1
    if ($res.StatusCode -eq 200) {
        $json = $res.Content | ConvertFrom-Json
        $found = $false
        foreach ($item in $json.data) {
            if ($item.id -eq [int64]$assessmentId) {
                $found = $true
                if ($item.assessmentCode -eq "ASM-VER-$runToken" -and $item.totalCandidates -eq 3 -and $item.maximumScore -eq 100.0) {
                    Record-Result 4 "Assessor 1 Assigned Assessment Listing" $true "Assessment ASM-VER-$runToken found with 3 candidates and 100 max score"
                } else {
                    Record-Result 4 "Assessor 1 Assigned Assessment Listing" $false "Assessment found but unexpected metadata (totalCandidates=$($item.totalCandidates))"
                }
            }
        }
        if (-not $found) {
            Record-Result 4 "Assessor 1 Assigned Assessment Listing" $false "Assessment ID $assessmentId not found in assigned assessments list"
        }
    } else {
        Record-Result 4 "Assessor 1 Assigned Assessment Listing" $false "HTTP $($res.StatusCode): $($res.Content)"
    }
} catch {
    Record-Result 4 "Assessor 1 Assigned Assessment Listing" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 5: Multi-Assessor Isolation (Assessor 2 cannot see Assessor 1's assessment)
# -------------------------------------------------------------------------
try {
    $res = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments" -Method "Get" -Headers $headers2
    if ($res.StatusCode -eq 200) {
        $json = $res.Content | ConvertFrom-Json
        $leaked = $false
        foreach ($item in $json.data) {
            if ($item.id -eq [int64]$assessmentId) {
                $leaked = $true
            }
        }
        if (-not $leaked) {
            Record-Result 5 "Multi-Assessor Assignment Tenancy Isolation" $true "Assessor 2 correctly does NOT see assessment assigned to Assessor 1"
        } else {
            Record-Result 5 "Multi-Assessor Assignment Tenancy Isolation" $false "Assessor 2 leaked access to Assessor 1's assessment"
        }
    } else {
        Record-Result 5 "Multi-Assessor Assignment Tenancy Isolation" $false "HTTP $($res.StatusCode): $($res.Content)"
    }
} catch {
    Record-Result 5 "Multi-Assessor Assignment Tenancy Isolation" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 6: Unauthorized Assessor Roster Access Rejection (403 Forbidden)
# -------------------------------------------------------------------------
try {
    $unauthRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/roster" -Method "Get" -Headers $headers2
    if ($unauthRes.StatusCode -eq 403) {
        Record-Result 6 "Unauthorized Assessment Roster Access Returns 403" $true "HTTP 403 Forbidden returned when unassigned assessor attempts to access roster"
    } else {
        Record-Result 6 "Unauthorized Assessment Roster Access Returns 403" $false "Expected HTTP 403 but got $($unauthRes.StatusCode)"
    }
} catch {
    Record-Result 6 "Unauthorized Assessment Roster Access Returns 403" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 7: Authorized Assessor Retrieves Candidate Roster
# -------------------------------------------------------------------------
try {
    $rosterRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/roster" -Method "Get" -Headers $headers1
    if ($rosterRes.StatusCode -eq 200) {
        $json = $rosterRes.Content | ConvertFrom-Json
        $candidates = $json.data.candidates
        if ($candidates.Count -eq 3) {
            Record-Result 7 "Assessor Retrieves Candidate Roster" $true "Candidate roster retrieved with exactly 3 enrolled trainees"
        } else {
            Record-Result 7 "Assessor Retrieves Candidate Roster" $false "Expected 3 candidates, got $($candidates.Count)"
        }
    } else {
        Record-Result 7 "Assessor Retrieves Candidate Roster" $false "HTTP $($rosterRes.StatusCode): $($rosterRes.Content)"
    }
} catch {
    Record-Result 7 "Assessor Retrieves Candidate Roster" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 8: Server-Side Score Validations (Negative, Exceeding Max Component)
# -------------------------------------------------------------------------
try {
    # 8A. Negative Score
    $negBody = @{
        isDraft = $true
        candidates = @(
            @{
                traineeId = [int64]$trainee1Id
                enrollmentId = [int64]$enrollment1Id
                appeared = $true
                theoryScore = -5.0
            }
        )
    } | ConvertTo-Json
    $negRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/draft" -Method "Post" -Body $negBody -Headers $headers1

    # 8B. Theory > 30.0
    $overTheoryBody = @{
        isDraft = $true
        candidates = @(
            @{
                traineeId = [int64]$trainee1Id
                enrollmentId = [int64]$enrollment1Id
                appeared = $true
                theoryScore = 35.0
            }
        )
    } | ConvertTo-Json
    $overTheoryRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/draft" -Method "Post" -Body $overTheoryBody -Headers $headers1

    # 8C. Practical > 50.0
    $overPracBody = @{
        isDraft = $true
        candidates = @(
            @{
                traineeId = [int64]$trainee1Id
                enrollmentId = [int64]$enrollment1Id
                appeared = $true
                practicalScore = 55.0
            }
        )
    } | ConvertTo-Json
    $overPracRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/draft" -Method "Post" -Body $overPracBody -Headers $headers1

    if ($negRes.StatusCode -eq 400 -and $overTheoryRes.StatusCode -eq 400 -and $overPracRes.StatusCode -eq 400) {
        Record-Result 8 "Server-Side Score Range & Component Validation" $true "Negative scores and component maximum violations correctly rejected with HTTP 400"
    } else {
        Record-Result 8 "Server-Side Score Range & Component Validation" $false "Status codes: Neg=$($negRes.StatusCode), OverTheory=$($overTheoryRes.StatusCode), OverPrac=$($overPracRes.StatusCode)"
    }
} catch {
    Record-Result 8 "Server-Side Score Range & Component Validation" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 9: Duplicate Candidate ID & Mismatched Enrollment Rejection
# -------------------------------------------------------------------------
try {
    $dupBody = @{
        isDraft = $true
        candidates = @(
            @{
                traineeId = [int64]$trainee1Id
                enrollmentId = [int64]$enrollment1Id
                appeared = $true
                theoryScore = 20.0
                practicalScore = 40.0
                vivaScore = 15.0
            },
            @{
                traineeId = [int64]$trainee1Id
                enrollmentId = [int64]$enrollment1Id
                appeared = $true
                theoryScore = 22.0
                practicalScore = 42.0
                vivaScore = 16.0
            }
        )
    } | ConvertTo-Json
    $dupRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/draft" -Method "Post" -Body $dupBody -Headers $headers1

    if ($dupRes.StatusCode -eq 400 -and $dupRes.Content -like "*DUPLICATE_CANDIDATE*") {
        Record-Result 9 "Duplicate Candidate Payload Rejection" $true "Duplicate candidate IDs in payload rejected with HTTP 400 DUPLICATE_CANDIDATE"
    } else {
        Record-Result 9 "Duplicate Candidate Payload Rejection" $false "Expected HTTP 400 DUPLICATE_CANDIDATE, got $($dupRes.StatusCode): $($dupRes.Content)"
    }
} catch {
    Record-Result 9 "Duplicate Candidate Payload Rejection" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 10: Foreign Candidate Injection Rejection
# -------------------------------------------------------------------------
try {
    $foreignTraineeId = (Invoke-MysqlQuery "SELECT trainee_id FROM trainees LIMIT 1;").Trim()
    $foreignBody = @{
        isDraft = $true
        candidates = @(
            @{
                traineeId = 999999
                enrollmentId = 999999
                appeared = $true
                theoryScore = 20.0
            }
        )
    } | ConvertTo-Json
    $foreignRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/draft" -Method "Post" -Body $foreignBody -Headers $headers1

    if ($foreignRes.StatusCode -eq 400 -and $foreignRes.Content -like "*INVALID_CANDIDATE*") {
        Record-Result 10 "Foreign Candidate Injection Rejection" $true "Foreign candidate outside batch rejected with HTTP 400 INVALID_CANDIDATE"
    } else {
        Record-Result 10 "Foreign Candidate Injection Rejection" $false "Expected HTTP 400 INVALID_CANDIDATE, got $($foreignRes.StatusCode): $($foreignRes.Content)"
    }
} catch {
    Record-Result 10 "Foreign Candidate Injection Rejection" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 11: Incomplete Roster Finalization Rejection (Batch has 3, payload has 2)
# -------------------------------------------------------------------------
try {
    $incompleteBody = @{
        isDraft = $false
        candidates = @(
            @{
                traineeId = [int64]$trainee1Id
                enrollmentId = [int64]$enrollment1Id
                appeared = $true
                theoryScore = 25.00
                practicalScore = 45.00
                vivaScore = 18.00
            },
            @{
                traineeId = [int64]$trainee2Id
                enrollmentId = [int64]$enrollment2Id
                appeared = $true
                theoryScore = 10.00
                practicalScore = 15.00
                vivaScore = 5.00
            }
        )
    } | ConvertTo-Json

    $incompRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/finalize" -Method "Post" -Body $incompleteBody -Headers $headers1

    if ($incompRes.StatusCode -eq 400 -and $incompRes.Content -like "*INCOMPLETE_ROSTER*") {
        Record-Result 11 "Incomplete Roster Finalization Rejection" $true "Finalization missing candidate Trainee #$trainee3Id correctly rejected with HTTP 400 INCOMPLETE_ROSTER"
    } else {
        Record-Result 11 "Incomplete Roster Finalization Rejection" $false "Expected HTTP 400 INCOMPLETE_ROSTER, got $($incompRes.StatusCode): $($incompRes.Content)"
    }
} catch {
    Record-Result 11 "Incomplete Roster Finalization Rejection" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 12: Save Draft Scores (Partial marks, all candidates, no certificate)
# -------------------------------------------------------------------------
$specialRemarks = 'Assessor notes: "Passed with Distinction" \ path: C:\assessments' + [char]10 + 'Status: In Progress'
$draftBody = @{
    isDraft = $true
    candidates = @(
        @{
            traineeId = [int64]$trainee1Id
            enrollmentId = [int64]$enrollment1Id
            appeared = $true
            theoryScore = 22.50
            practicalScore = 38.00
            vivaScore = $null
            remarks = $specialRemarks
        },
        @{
            traineeId = [int64]$trainee2Id
            enrollmentId = [int64]$enrollment2Id
            appeared = $true
            theoryScore = 12.00
            practicalScore = $null
            vivaScore = $null
            remarks = "Candidate partially evaluated"
        },
        @{
            traineeId = [int64]$trainee3Id
            enrollmentId = [int64]$enrollment3Id
            appeared = $false
            remarks = "Absent during draft evaluation"
        }
    )
} | ConvertTo-Json

try {
    $draftRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/draft" -Method "Post" -Body $draftBody -Headers $headers1
    if ($draftRes.StatusCode -eq 200) {
        $certCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM certifications WHERE enrollment_id IN ($enrollment1Id, $enrollment2Id, $enrollment3Id);"
        $lifecycle = Invoke-MysqlQuery "SELECT lifecycle_status_id FROM assessments WHERE assessment_id = $assessmentId;"

        if ([int]$certCount.Trim() -eq 0 -and [int]$lifecycle.Trim() -eq 1) {
            Record-Result 12 "Draft Score Persistence Without Certification" $true "Draft scores saved: 0 certificates issued, assessment lifecycle remains SCHEDULED (1)"
        } else {
            Record-Result 12 "Draft Score Persistence Without Certification" $false "certCount=$certCount, lifecycle=$lifecycle"
        }
    } else {
        Record-Result 12 "Draft Score Persistence Without Certification" $false "HTTP $($draftRes.StatusCode): $($draftRes.Content)"
    }
} catch {
    Record-Result 12 "Draft Score Persistence Without Certification" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 13: Finalize Assessment & Authoritative Certification Issuance
# -------------------------------------------------------------------------
$certId = $null
$fullFinalizeBody = @{
    isDraft = $false
    candidates = @(
        @{
            traineeId = [int64]$trainee1Id
            enrollmentId = [int64]$enrollment1Id
            appeared = $true
            theoryScore = 25.00
            practicalScore = 45.00
            vivaScore = 18.00
            remarks = "Outstanding performance in inverter wiring"
        },
        @{
            traineeId = [int64]$trainee2Id
            enrollmentId = [int64]$enrollment2Id
            appeared = $true
            theoryScore = 10.00
            practicalScore = 15.00
            vivaScore = 5.00
            remarks = "Failed minimum safety threshold"
        },
        @{
            traineeId = [int64]$trainee3Id
            enrollmentId = [int64]$enrollment3Id
            appeared = $false
            remarks = "Absent during final assessment"
        }
    )
} | ConvertTo-Json

try {
    $finalizeRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/finalize" -Method "Post" -Body $fullFinalizeBody -Headers $headers1

    if ($finalizeRes.StatusCode -eq 200) {
        # Verify Database Outcomes
        $t1Outcome = (Invoke-MysqlQuery "SELECT ar.assessment_outcome_id FROM assessment_results ar JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id WHERE ta.assessment_id = $assessmentId AND ta.trainee_id = $trainee1Id;").Trim()
        $t2Outcome = (Invoke-MysqlQuery "SELECT ar.assessment_outcome_id FROM assessment_results ar JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id WHERE ta.assessment_id = $assessmentId AND ta.trainee_id = $trainee2Id;").Trim()
        $t3Outcome = (Invoke-MysqlQuery "SELECT ar.assessment_outcome_id FROM assessment_results ar JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id WHERE ta.assessment_id = $assessmentId AND ta.trainee_id = $trainee3Id;").Trim()

        $t1CertId = (Invoke-MysqlQuery "SELECT certification_id FROM certifications WHERE trainee_id = $trainee1Id AND enrollment_id = $enrollment1Id;").Trim()
        $t2CertCount = (Invoke-MysqlQuery "SELECT COUNT(*) FROM certifications WHERE trainee_id = $trainee2Id;").Trim()
        $t3CertCount = (Invoke-MysqlQuery "SELECT COUNT(*) FROM certifications WHERE trainee_id = $trainee3Id;").Trim()

        $t1EnrollmentStatus = (Invoke-MysqlQuery "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = $enrollment1Id;").Trim()
        $t2EnrollmentStatus = (Invoke-MysqlQuery "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = $enrollment2Id;").Trim()
        $t3EnrollmentStatus = (Invoke-MysqlQuery "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = $enrollment3Id;").Trim()

        $asmLifecycle = (Invoke-MysqlQuery "SELECT lifecycle_status_id FROM assessments WHERE assessment_id = $assessmentId;").Trim()

        # Certificate status and verification status check
        $certStatus = (Invoke-MysqlQuery "SELECT certificate_status_id FROM certifications WHERE certification_id = $t1CertId;").Trim()
        $certVerif = (Invoke-MysqlQuery "SELECT certificate_verification_status_id FROM certifications WHERE certification_id = $t1CertId;").Trim()
        $verifiedAt = (Invoke-MysqlQuery "SELECT IFNULL(verified_at, 'NULL') FROM certifications WHERE certification_id = $t1CertId;").Trim()
        $issuingBody = (Invoke-MysqlQuery "SELECT issuing_body FROM certifications WHERE certification_id = $t1CertId;").Trim()

        $certId = $t1CertId

        $validCert = ([int]$certStatus -eq 1 -and [int]$certVerif -eq 2 -and $verifiedAt -eq "NULL" -and $issuingBody -like "*Green Skills Council*")

        if ([int]$t1Outcome -eq 2 -and [int]$t2Outcome -eq 3 -and [int]$t3Outcome -eq 4 -and [int]$t1CertId -gt 0 -and [int]$t2CertCount -eq 0 -and [int]$t3CertCount -eq 0 -and [int]$t1EnrollmentStatus -eq 3 -and [int]$t2EnrollmentStatus -eq 1 -and [int]$t3EnrollmentStatus -eq 1 -and [int]$asmLifecycle -eq 2 -and $validCert) {
            Record-Result 13 "Finalize Assessment & Authoritative Certification" $true "Passing candidate certified with status=ISSUED (1), verif=PENDING (2), verified_at=NULL, issuing_body='$issuingBody'; failing/absent candidates 0 certs"
        } else {
            Record-Result 13 "Finalize Assessment & Authoritative Certification" $false "t1Out=$t1Outcome, t2Out=$t2Outcome, t3Out=$t3Outcome, certStatus=$certStatus, certVerif=$certVerif, verifiedAt=$verifiedAt, issuingBody=$issuingBody"
        }
    } else {
        Record-Result 13 "Finalize Assessment & Authoritative Certification" $false "HTTP $($finalizeRes.StatusCode): $($finalizeRes.Content)"
    }
} catch {
    Record-Result 13 "Finalize Assessment & Authoritative Certification" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 14: Idempotent Re-finalization Returns 200 With Zero Extra Certs
# -------------------------------------------------------------------------
try {
    $reFinRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/finalize" -Method "Post" -Body $fullFinalizeBody -Headers $headers1
    if ($reFinRes.StatusCode -eq 200) {
        $certCountAfter = (Invoke-MysqlQuery "SELECT COUNT(*) FROM certifications WHERE trainee_id = $trainee1Id AND enrollment_id = $enrollment1Id;").Trim()
        if ([int]$certCountAfter -eq 1) {
            Record-Result 14 "Idempotent Re-finalization Safe Handling" $true "Identical finalization payload returned HTTP 200 read-only response with zero duplicate certs"
        } else {
            Record-Result 14 "Idempotent Re-finalization Safe Handling" $false "Duplicate certificates generated! Count=$certCountAfter"
        }
    } else {
        Record-Result 14 "Idempotent Re-finalization Safe Handling" $false "Expected HTTP 200, got $($reFinRes.StatusCode): $($reFinRes.Content)"
    }
} catch {
    Record-Result 14 "Idempotent Re-finalization Safe Handling" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 15: Modifying Marks on Finalized Assessment Rejected
# -------------------------------------------------------------------------
try {
    $altFinalizeBody = @{
        isDraft = $false
        candidates = @(
            @{
                traineeId = [int64]$trainee1Id
                enrollmentId = [int64]$enrollment1Id
                appeared = $true
                theoryScore = 28.00  # Altered mark
                practicalScore = 48.00
                vivaScore = 19.00
            },
            @{
                traineeId = [int64]$trainee2Id
                enrollmentId = [int64]$enrollment2Id
                appeared = $true
                theoryScore = 10.00
                practicalScore = 15.00
                vivaScore = 5.00
            },
            @{
                traineeId = [int64]$trainee3Id
                enrollmentId = [int64]$enrollment3Id
                appeared = $false
            }
        )
    } | ConvertTo-Json

    $altFinRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/finalize" -Method "Post" -Body $altFinalizeBody -Headers $headers1

    if ($altFinRes.StatusCode -eq 400 -and $altFinRes.Content -like "*ASSESSMENT_ALREADY_FINALIZED*") {
        Record-Result 15 "Altered Finalization Marks Rejection Guard" $true "Re-finalization with altered marks rejected with HTTP 400 ASSESSMENT_ALREADY_FINALIZED"
    } else {
        Record-Result 15 "Altered Finalization Marks Rejection Guard" $false "Expected HTTP 400 ASSESSMENT_ALREADY_FINALIZED, got $($altFinRes.StatusCode): $($altFinRes.Content)"
    }
} catch {
    Record-Result 15 "Altered Finalization Marks Rejection Guard" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 16: Draft Submission on Finalized Assessment Rejected
# -------------------------------------------------------------------------
try {
    $editAttempt = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/draft" -Method "Post" -Body $draftBody -Headers $headers1
    if ($editAttempt.StatusCode -eq 400 -and $editAttempt.Content -like "*ASSESSMENT_ALREADY_FINALIZED*") {
        Record-Result 16 "Draft Modification on Finalized Assessment Guard" $true "Draft submission rejected with HTTP 400 ASSESSMENT_ALREADY_FINALIZED"
    } else {
        Record-Result 16 "Draft Modification on Finalized Assessment Guard" $false "Expected HTTP 400 but got $($editAttempt.StatusCode)"
    }
} catch {
    Record-Result 16 "Draft Modification on Finalized Assessment Guard" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 17: Historical Enrollment Completion Date Preservation
# -------------------------------------------------------------------------
try {
    # Set Trainee 1 enrollment to historical completion date
    Invoke-MysqlQuery "UPDATE training_enrollments SET actual_completion_date = '2025-05-10' WHERE enrollment_id = $enrollment1Id;"

    # Re-trigger idempotent finalization
    $resp = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/finalize" -Method "Post" -Body $fullFinalizeBody -Headers $headers1
    $savedCompletionDate = (Invoke-MysqlQuery "SELECT actual_completion_date FROM training_enrollments WHERE enrollment_id = $enrollment1Id;").Trim()

    if ($savedCompletionDate -eq "2025-05-10") {
        Record-Result 17 "Historical Completion Date Preservation" $true "Historical completion date '2025-05-10' safely preserved without being overwritten"
    } else {
        Record-Result 17 "Historical Completion Date Preservation" $false "Expected '2025-05-10', found '$savedCompletionDate'"
    }
} catch {
    Record-Result 17 "Historical Completion Date Preservation" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 18: Cross-Assessor & Unauthenticated Certificate Access Guard
# -------------------------------------------------------------------------
try {
    if ($certId) {
        # Assessor 1 (Assigned) -> 200 OK
        $pdf1Res = Invoke-SafeRequest -Uri "$baseUrl/api/v1/certifications/$certId/download" -Method "Get" -Headers $headers1
        # Assessor 2 (Unassigned) -> 403 Forbidden
        $pdf2Res = Invoke-SafeRequest -Uri "$baseUrl/api/v1/certifications/$certId/download" -Method "Get" -Headers $headers2
        # Unauthenticated -> 401 Unauthorized
        $unauthPdfRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/certifications/$certId/download" -Method "Get" -Headers @{}

        if ($pdf1Res.StatusCode -eq 200 -and $pdf2Res.StatusCode -eq 403 -and $unauthPdfRes.StatusCode -eq 401) {
            Record-Result 18 "Certificate Download Access Authorization Guard" $true "Assigned assessor: 200 OK; Unassigned assessor: 403 Forbidden; Unauthenticated: 401 Unauthorized"
        } else {
            Record-Result 18 "Certificate Download Access Authorization Guard" $false "Assessor1=$($pdf1Res.StatusCode), Assessor2=$($pdf2Res.StatusCode), Unauth=$($unauthPdfRes.StatusCode)"
        }
    } else {
        Record-Result 18 "Certificate Download Access Authorization Guard" $false "No certId available"
    }
} catch {
    Record-Result 18 "Certificate Download Access Authorization Guard" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 19: Trainee Tenancy Certificate Access (Owner allowed, Cross-Trainee denied)
# -------------------------------------------------------------------------
try {
    if ($certId -and $trainee1Headers.Count -gt 0 -and $trainee2Headers.Count -gt 0) {
        # Trainee 1 (Owner) -> 200 OK
        $t1PdfRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/certifications/$certId/download" -Method "Get" -Headers $trainee1Headers
        # Trainee 2 (Non-owner) -> 403 Forbidden
        $t2PdfRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/certifications/$certId/download" -Method "Get" -Headers $trainee2Headers

        if ($t1PdfRes.StatusCode -eq 200 -and $t2PdfRes.StatusCode -eq 403) {
            Record-Result 19 "Trainee Tenancy Certificate Isolation Guard" $true "Certificate owner trainee: 200 OK; Cross-trainee: 403 Forbidden"
        } else {
            Record-Result 19 "Trainee Tenancy Certificate Isolation Guard" $false "Owner=$($t1PdfRes.StatusCode), Cross-Trainee=$($t2PdfRes.StatusCode)"
        }
    } else {
        Record-Result 19 "Trainee Tenancy Certificate Isolation Guard" $false "Trainee tokens or certId unavailable"
    }
} catch {
    Record-Result 19 "Trainee Tenancy Certificate Isolation Guard" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 20: Authentic PDF Generation & Content Stream Validation
# -------------------------------------------------------------------------
try {
    if ($certId) {
        $pdfRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/certifications/$certId/download" -Method "Get" -Headers $headers1
        $contentStr = if ($pdfRes.Content -is [byte[]]) {
            [System.Text.Encoding]::ASCII.GetString($pdfRes.Content)
        } elseif ($pdfRes.Content -is [string]) {
            $pdfRes.Content
        } else {
            [System.Text.Encoding]::ASCII.GetString($pdfRes.Raw.RawContent)
        }

        $hasMagic = $contentStr.StartsWith("%PDF-1.4")
        $hasEof = $contentStr.Contains("%%EOF")
        $hasCourse = $contentStr.Contains("CRS-VER-$runToken")
        $hasCertNum = $contentStr.Contains("CERT-CRS-VER-")
        $hasStatus = $contentStr.Contains("Credential Status: ACTIVE / ISSUED")
        $hasVerif = $contentStr.Contains("Verification Status: PENDING VERIFICATION")

        if ($pdfRes.StatusCode -eq 200 -and $hasMagic -and $hasEof -and $hasCourse -and $hasCertNum -and $hasStatus -and $hasVerif) {
            Record-Result 20 "Authentic PDF Content Stream & Status Integrity" $true "Valid PDF-1.4 with %%EOF, matching Course Code, Certificate Number, ACTIVE status, and PENDING verification banner"
        } else {
            Record-Result 20 "Authentic PDF Content Stream & Status Integrity" $false "Magic=$hasMagic, EOF=$hasEof, Course=$hasCourse, Cert=$hasCertNum, Status=$hasStatus, Verif=$hasVerif"
        }
    } else {
        Record-Result 20 "Authentic PDF Content Stream & Status Integrity" $false "No certId available"
    }
} catch {
    Record-Result 20 "Authentic PDF Content Stream & Status Integrity" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 21: Jackson Remarks Special Characters & JSON Formatting Integrity
# -------------------------------------------------------------------------
try {
    $rawRemarks = (Invoke-MysqlQuery "SELECT ar.remarks FROM assessment_results ar JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id WHERE ta.assessment_id = $assessmentId AND ta.trainee_id = $trainee1Id;").Trim()
    
    $parsedRemarks = $rawRemarks | ConvertFrom-Json
    $hasTheory = $parsedRemarks.theory -ne $null
    $hasPractical = $parsedRemarks.practical -ne $null
    $hasStatus = $parsedRemarks.status -eq "FINALIZED"
    $hasNote = $parsedRemarks.note -like "*inverter wiring*"

    if ($hasTheory -and $hasPractical -and $hasStatus -and $hasNote) {
        Record-Result 21 "Jackson Remarks Serialization & Breakdown Storage" $true "Component breakdown safely stored as typed JSON without string concatenation or quote corruption"
    } else {
        Record-Result 21 "Jackson Remarks Serialization & Breakdown Storage" $false "Raw JSON: $rawRemarks"
    }
} catch {
    Record-Result 21 "Jackson Remarks Serialization & Breakdown Storage" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 22: Target Endpoint Aliases Functional Check
# -------------------------------------------------------------------------
try {
    $resultsAlias = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessments/results" -Method "Get" -Headers $headers1
    $issueAlias = Invoke-SafeRequest -Uri "$baseUrl/api/v1/certificates/issue" -Method "Post" -Body "{}" -Headers $headers1

    if ($resultsAlias.StatusCode -eq 200 -and $issueAlias.StatusCode -ne 404) {
        Record-Result 22 "Target Endpoint Aliases Functionality" $true "Aliases /api/v1/assessments/results and /api/v1/certificates/issue are active"
    } else {
        Record-Result 22 "Target Endpoint Aliases Functionality" $false "resultsAlias=$($resultsAlias.StatusCode), issueAlias=$($issueAlias.StatusCode)"
    }
} catch {
    Record-Result 22 "Target Endpoint Aliases Functionality" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 23: Reverse-Dependency Database Teardown
# -------------------------------------------------------------------------
try {
    if ($certId) {
        Invoke-MysqlQuery "DELETE FROM certifications WHERE certification_id = $certId;"
    }
    Invoke-MysqlQuery "DELETE FROM assessment_results WHERE trainee_id IN ($trainee1Id, $trainee2Id, $trainee3Id);"
    Invoke-MysqlQuery "DELETE FROM trainee_assessments WHERE assessment_id = $assessmentId;"
    Invoke-MysqlQuery "DELETE FROM assessments WHERE assessment_id = $assessmentId;"
    Invoke-MysqlQuery "DELETE FROM training_enrollments WHERE enrollment_id IN ($enrollment1Id, $enrollment2Id, $enrollment3Id);"
    Invoke-MysqlQuery "DELETE FROM trainees WHERE trainee_id IN ($trainee1Id, $trainee2Id, $trainee3Id);"
    Invoke-MysqlQuery "DELETE FROM training_batches WHERE batch_id = $batchId;"
    Invoke-MysqlQuery "DELETE FROM training_centers WHERE center_id = $centerId;"
    Invoke-MysqlQuery "DELETE FROM training_providers WHERE provider_id = $providerId;"
    Invoke-MysqlQuery "DELETE FROM programs WHERE program_id = $programId;"
    Invoke-MysqlQuery "DELETE FROM schemes WHERE scheme_id = $schemeId;"
    Invoke-MysqlQuery "DELETE FROM departments WHERE department_id = $deptId;"
    Invoke-MysqlQuery "DELETE FROM organizations WHERE organization_id = $orgId;"
    Invoke-MysqlQuery "DELETE FROM courses WHERE course_id = $courseId;"
    Invoke-MysqlQuery "DELETE FROM sectors WHERE sector_id = $sectorId;"
    Invoke-MysqlQuery "DELETE FROM user_roles WHERE user_id IN ($assessor1Id, $assessor2Id, $trainee1UserId, $trainee2UserId);"
    Invoke-MysqlQuery "DELETE FROM users WHERE user_id IN ($assessor1Id, $assessor2Id, $trainee1UserId, $trainee2UserId);"

    Record-Result 23 "Reverse-Dependency Database Teardown" $true "All tracked test fixtures deleted cleanly from SIH26135_TEST"
} catch {
    Record-Result 23 "Reverse-Dependency Database Teardown" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 24: Post-Test Authoritative Trainee 1005143L Invariant Preservation
# -------------------------------------------------------------------------
$postTraineeCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM trainees WHERE trainee_id = 1005143 AND deleted_at IS NULL;"
$postConsentCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = 1005143 AND consent_status_id = 1;"

if ([int]$postTraineeCount.Trim() -eq 1 -and [int]$postConsentCount.Trim() -eq 5) {
    Record-Result 24 "Post-Test Authoritative Trainee 1005143L Invariant Preservation" $true "Trainee 1005143L retained exactly 5 active granted DPDP consents."
} else {
    Record-Result 24 "Post-Test Authoritative Trainee 1005143L Invariant Preservation" $false "Invariant violated! Found trainee=$postTraineeCount, consents=$postConsentCount"
}

# -------------------------------------------------------------------------
# Final Summary Output
# -------------------------------------------------------------------------
Write-Host "`n=================================================================" -ForegroundColor Cyan
$summaryColor = if ($failCount -eq 0) { "Green" } else { "Red" }
Write-Host "Verification Summary: $passCount Passed, $failCount Failed" -ForegroundColor $summaryColor
Write-Host "=================================================================" -ForegroundColor Cyan

if ($failCount -gt 0) {
    exit 1
} else {
    exit 0
}
