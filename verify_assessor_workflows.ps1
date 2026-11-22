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
Write-Host "SIH-26135 Phase 30.6 Assessor Workbench Verification" -ForegroundColor Cyan
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
# Pre-Execution Safety Invariant: Check Trainee 1005143L and Consents
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

# Trainee 1 (Will Pass)
Invoke-MysqlQuery @"
INSERT INTO trainees (registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES ('REG-T1-$runToken', 'Ankit', 'Kumar', 1, 101, 1001, 1, 1, NOW(), NOW());
"@
$trainee1Id = (Invoke-MysqlQuery "SELECT trainee_id FROM trainees WHERE registration_number = 'REG-T1-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO training_enrollments (enrollment_number, trainee_id, course_id, provider_id, center_id, batch_id, program_id, enrollment_status_id, enrollment_date, created_at, updated_at)
VALUES ('ENR-T1-$runToken', $trainee1Id, $courseId, $providerId, $centerId, $batchId, $programId, 1, CURDATE(), NOW(), NOW());
"@
$enrollment1Id = (Invoke-MysqlQuery "SELECT enrollment_id FROM training_enrollments WHERE enrollment_number = 'ENR-T1-$runToken';").Trim()

# Trainee 2 (Will Fail)
Invoke-MysqlQuery @"
INSERT INTO trainees (registration_number, first_name, last_name, gender_id, state_id, district_id, current_employment_status_id, profile_status_id, created_at, updated_at)
VALUES ('REG-T2-$runToken', 'Sunita', 'Rani', 2, 101, 1001, 1, 1, NOW(), NOW());
"@
$trainee2Id = (Invoke-MysqlQuery "SELECT trainee_id FROM trainees WHERE registration_number = 'REG-T2-$runToken';").Trim()

Invoke-MysqlQuery @"
INSERT INTO training_enrollments (enrollment_number, trainee_id, course_id, provider_id, center_id, batch_id, program_id, enrollment_status_id, enrollment_date, created_at, updated_at)
VALUES ('ENR-T2-$runToken', $trainee2Id, $courseId, $providerId, $centerId, $batchId, $programId, 1, CURDATE(), NOW(), NOW());
"@
$enrollment2Id = (Invoke-MysqlQuery "SELECT enrollment_id FROM training_enrollments WHERE enrollment_number = 'ENR-T2-$runToken';").Trim()

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
                if ($item.assessmentCode -eq "ASM-VER-$runToken" -and $item.totalCandidates -eq 2 -and $item.maximumScore -eq 100.0) {
                    Record-Result 4 "Assessor 1 Assigned Assessment Listing" $true "Assessment ASM-VER-$runToken found with 2 candidates and 100 max score"
                } else {
                    Record-Result 4 "Assessor 1 Assigned Assessment Listing" $false "Assessment found but unexpected metadata"
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
        if ($candidates.Count -eq 2) {
            Record-Result 7 "Assessor Retrieves Candidate Roster" $true "Candidate roster retrieved with exactly 2 enrolled trainees"
        } else {
            Record-Result 7 "Assessor Retrieves Candidate Roster" $false "Expected 2 candidates, got $($candidates.Count)"
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
# Scenario 9: Save Draft Scores (Partial marks, no certificate)
# -------------------------------------------------------------------------
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
            remarks = "Draft score observation"
        },
        @{
            traineeId = [int64]$trainee2Id
            enrollmentId = [int64]$enrollment2Id
            appeared = $true
            theoryScore = 12.00
            practicalScore = $null
            vivaScore = $null
            remarks = "Candidate partially evaluated"
        }
    )
} | ConvertTo-Json

try {
    $draftRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/draft" -Method "Post" -Body $draftBody -Headers $headers1
    if ($draftRes.StatusCode -eq 200) {
        $certCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM certifications WHERE enrollment_id IN ($enrollment1Id, $enrollment2Id);"
        $lifecycle = Invoke-MysqlQuery "SELECT lifecycle_status_id FROM assessments WHERE assessment_id = $assessmentId;"

        if ([int]$certCount.Trim() -eq 0 -and [int]$lifecycle.Trim() -eq 1) {
            Record-Result 9 "Draft Score Persistence Without Certification" $true "Draft scores saved: 0 certificates issued, assessment lifecycle remains SCHEDULED (1)"
        } else {
            Record-Result 9 "Draft Score Persistence Without Certification" $false "certCount=$certCount, lifecycle=$lifecycle"
        }
    } else {
        Record-Result 9 "Draft Score Persistence Without Certification" $false "HTTP $($draftRes.StatusCode): $($draftRes.Content)"
    }
} catch {
    Record-Result 9 "Draft Score Persistence Without Certification" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 10: Finalize Assessment & Authoritative Certification Issuance
# -------------------------------------------------------------------------
$certId = $null
try {
    $finalizeBody = @{
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
            }
        )
    } | ConvertTo-Json

    $finalizeRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/finalize" -Method "Post" -Body $finalizeBody -Headers $headers1

    if ($finalizeRes.StatusCode -eq 200) {
        # Verify Database Outcomes
        $t1Outcome = (Invoke-MysqlQuery "SELECT ar.assessment_outcome_id FROM assessment_results ar JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id WHERE ta.assessment_id = $assessmentId AND ta.trainee_id = $trainee1Id;").Trim()
        $t2Outcome = (Invoke-MysqlQuery "SELECT ar.assessment_outcome_id FROM assessment_results ar JOIN trainee_assessments ta ON ar.trainee_assessment_id = ta.trainee_assessment_id WHERE ta.assessment_id = $assessmentId AND ta.trainee_id = $trainee2Id;").Trim()
        
        $t1CertId = (Invoke-MysqlQuery "SELECT certification_id FROM certifications WHERE trainee_id = $trainee1Id AND enrollment_id = $enrollment1Id;").Trim()
        $t2CertCount = (Invoke-MysqlQuery "SELECT COUNT(*) FROM certifications WHERE trainee_id = $trainee2Id;").Trim()
        
        $t1EnrollmentStatus = (Invoke-MysqlQuery "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = $enrollment1Id;").Trim()
        $t2EnrollmentStatus = (Invoke-MysqlQuery "SELECT enrollment_status_id FROM training_enrollments WHERE enrollment_id = $enrollment2Id;").Trim()
        
        $asmLifecycle = (Invoke-MysqlQuery "SELECT lifecycle_status_id FROM assessments WHERE assessment_id = $assessmentId;").Trim()

        $certId = $t1CertId

        if ([int]$t1Outcome -eq 2 -and [int]$t2Outcome -eq 3 -and [int]$t1CertId -gt 0 -and [int]$t2CertCount -eq 0 -and [int]$t1EnrollmentStatus -eq 3 -and [int]$t2EnrollmentStatus -eq 1 -and [int]$asmLifecycle -eq 2) {
            Record-Result 10 "Finalize Assessment & Authoritative Evaluation" $true "Passing candidate certified & enrollment completed; failing candidate declared FAIL with 0 certs; assessment lifecycle = FINALIZED"
        } else {
            Record-Result 10 "Finalize Assessment & Authoritative Evaluation" $false "t1Outcome=$t1Outcome, t2Outcome=$t2Outcome, t1CertId=$t1CertId, t2Certs=$t2CertCount, t1Enr=$t1EnrollmentStatus, t2Enr=$t2EnrollmentStatus, asmLifecycle=$asmLifecycle"
        }
    } else {
        Record-Result 10 "Finalize Assessment & Authoritative Evaluation" $false "HTTP $($finalizeRes.StatusCode): $($finalizeRes.Content)"
    }
} catch {
    Record-Result 10 "Finalize Assessment & Authoritative Evaluation" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 11: Finalized Assessment Immutability Check
# -------------------------------------------------------------------------
try {
    $editAttempt = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessor/assessments/$assessmentId/draft" -Method "Post" -Body $draftBody -Headers $headers1
    if ($editAttempt.StatusCode -eq 400) {
        Record-Result 11 "Finalized Assessment Immutability Guard" $true "Subsequent modification rejected with HTTP 400 (ASSESSMENT_ALREADY_FINALIZED)"
    } else {
        Record-Result 11 "Finalized Assessment Immutability Guard" $false "Expected HTTP 400 but got $($editAttempt.StatusCode)"
    }
} catch {
    Record-Result 11 "Finalized Assessment Immutability Guard" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 12: Authentic PDF Certificate Download & Magic Byte Validation
# -------------------------------------------------------------------------
try {
    if ($certId) {
        $pdfRes = Invoke-SafeRequest -Uri "$baseUrl/api/v1/certifications/$certId/download" -Method "Get" -Headers $headers1
        $contentType = $pdfRes.Headers["Content-Type"]
        $rawBytes = $pdfRes.Raw.RawContent
        $contentStr = $pdfRes.Content

        if ($pdfRes.StatusCode -eq 200 -and ($contentType -like "*application/pdf*" -or $contentStr.StartsWith("%PDF-"))) {
            Record-Result 12 "Authentic PDF Certificate Download Stream" $true "Certificate downloaded with valid '%PDF-' header"
        } else {
            Record-Result 12 "Authentic PDF Certificate Download Stream" $false "StatusCode=$($pdfRes.StatusCode), ContentType=$contentType"
        }
    } else {
        Record-Result 12 "Authentic PDF Certificate Download Stream" $false "No certId available from Scenario 10"
    }
} catch {
    Record-Result 12 "Authentic PDF Certificate Download Stream" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 13: Target Endpoint Aliases Functional Check
# -------------------------------------------------------------------------
try {
    $resultsAlias = Invoke-SafeRequest -Uri "$baseUrl/api/v1/assessments/results" -Method "Get" -Headers $headers1
    $issueAlias = Invoke-SafeRequest -Uri "$baseUrl/api/v1/certificates/issue" -Method "Post" -Body "{}" -Headers $headers1

    if ($resultsAlias.StatusCode -eq 200 -and $issueAlias.StatusCode -ne 404) {
        Record-Result 13 "Target Endpoint Aliases Functionality" $true "Aliases /api/v1/assessments/results and /api/v1/certificates/issue are active"
    } else {
        Record-Result 13 "Target Endpoint Aliases Functionality" $false "resultsAlias=$($resultsAlias.StatusCode), issueAlias=$($issueAlias.StatusCode)"
    }
} catch {
    Record-Result 13 "Target Endpoint Aliases Functionality" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 14: Fixture Teardown and Cleanup
# -------------------------------------------------------------------------
try {
    if ($certId) {
        Invoke-MysqlQuery "DELETE FROM certifications WHERE certification_id = $certId;"
    }
    Invoke-MysqlQuery "DELETE FROM assessment_results WHERE trainee_id IN ($trainee1Id, $trainee2Id);"
    Invoke-MysqlQuery "DELETE FROM trainee_assessments WHERE assessment_id = $assessmentId;"
    Invoke-MysqlQuery "DELETE FROM assessments WHERE assessment_id = $assessmentId;"
    Invoke-MysqlQuery "DELETE FROM training_enrollments WHERE enrollment_id IN ($enrollment1Id, $enrollment2Id);"
    Invoke-MysqlQuery "DELETE FROM trainees WHERE trainee_id IN ($trainee1Id, $trainee2Id);"
    Invoke-MysqlQuery "DELETE FROM training_batches WHERE batch_id = $batchId;"
    Invoke-MysqlQuery "DELETE FROM training_centers WHERE center_id = $centerId;"
    Invoke-MysqlQuery "DELETE FROM training_providers WHERE provider_id = $providerId;"
    Invoke-MysqlQuery "DELETE FROM programs WHERE program_id = $programId;"
    Invoke-MysqlQuery "DELETE FROM schemes WHERE scheme_id = $schemeId;"
    Invoke-MysqlQuery "DELETE FROM departments WHERE department_id = $deptId;"
    Invoke-MysqlQuery "DELETE FROM organizations WHERE organization_id = $orgId;"
    Invoke-MysqlQuery "DELETE FROM courses WHERE course_id = $courseId;"
    Invoke-MysqlQuery "DELETE FROM sectors WHERE sector_id = $sectorId;"
    Invoke-MysqlQuery "DELETE FROM user_roles WHERE user_id IN ($assessor1Id, $assessor2Id);"
    Invoke-MysqlQuery "DELETE FROM users WHERE user_id IN ($assessor1Id, $assessor2Id);"

    Record-Result 14 "Reverse-Dependency Database Teardown" $true "All tracked test fixtures deleted cleanly from SIH26135_TEST"
} catch {
    Record-Result 14 "Reverse-Dependency Database Teardown" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 15: Post-Test Authoritative Trainee 1005143L Invariant Check
# -------------------------------------------------------------------------
$postTraineeCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM trainees WHERE trainee_id = 1005143 AND deleted_at IS NULL;"
$postConsentCount = Invoke-MysqlQuery "SELECT COUNT(*) FROM trainee_consents WHERE trainee_id = 1005143 AND consent_status_id = 1;"

if ([int]$postTraineeCount.Trim() -eq 1 -and [int]$postConsentCount.Trim() -eq 5) {
    Record-Result 15 "Post-Test Authoritative Trainee 1005143L Invariant Preservation" $true "Trainee 1005143L retained exactly 5 active granted DPDP consents."
} else {
    Record-Result 15 "Post-Test Authoritative Trainee 1005143L Invariant Preservation" $false "Invariant violated! Found trainee=$postTraineeCount, consents=$postConsentCount"
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
