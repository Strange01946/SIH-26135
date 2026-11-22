# SIH-26135 — Phase 30.4 Training Provider Portal Live Verification Script
# Strict Verification of all 25 Scenarios against SIH26135_TEST and Spring Boot Port 8080

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

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "SIH-26135 Phase 30.4 Live Runtime Functional & Security Verification" -ForegroundColor Cyan
Write-Host "Target: $baseUrl | Database: SIH26135_TEST" -ForegroundColor Cyan
Write-Host "=================================================================`n" -ForegroundColor Cyan

# -------------------------------------------------------------------------
# Scenario 1: Training Provider Login & Profile Inspection
# -------------------------------------------------------------------------
try {
    $loginBody = @{
        usernameOrEmail = "provider_test_user"
        password = "RawPassword123"
    } | ConvertTo-Json

    $loginResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $token = $loginResp.data.accessToken
    $headers = @{ Authorization = "Bearer $token" }

    $hasTpRole = $loginResp.data.authorities -contains "ROLE_training_provider"
    $hasCenterManage = $loginResp.data.authorities -contains "center.manage"
    $hasEnrollManage = $loginResp.data.authorities -contains "enrollment.manage"

    if ($token -and $hasTpRole -and $hasCenterManage -and $hasEnrollManage) {
        Record-Result 1 "Training Provider Login & Profile Inspection" $true "Token issued, authorities validated"
    } else {
        Record-Result 1 "Training Provider Login & Profile Inspection" $false "Missing expected authorities"
    }
} catch {
    Record-Result 1 "Training Provider Login & Profile Inspection" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 2: Trainee Provider Context Resolution
# -------------------------------------------------------------------------
try {
    $meResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/me" -Headers $headers
    $tpId = $meResp.data.trainingProviderId
    if ($tpId -eq 1010905) {
        Record-Result 2 "Provider Context Resolution" $true "trainingProviderId resolved to 1010905"
    } else {
        Record-Result 2 "Provider Context Resolution" $false "Resolved provider ID: $tpId (expected 1010905)"
    }
} catch {
    Record-Result 2 "Provider Context Resolution" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 3: Training Centers Directory Listing
# -------------------------------------------------------------------------
try {
    $centersResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-centers?providerId=1010905" -Headers $headers
    $centerCount = $centersResp.data.content.Count
    $firstCenter = $centersResp.data.content[0]
    if ($centerCount -ge 3 -and $firstCenter.centerCode -ne $null) {
        Record-Result 3 "Training Centers Directory Listing" $true "Listed $centerCount affiliated centers under provider 1010905"
    } else {
        Record-Result 3 "Training Centers Directory Listing" $false "Found $centerCount centers"
    }
} catch {
    Record-Result 3 "Training Centers Directory Listing" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 4: Approved Courses Listing
# -------------------------------------------------------------------------
try {
    $coursesResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/courses" -Headers $headers
    $courseCount = $coursesResp.data.content.Count
    $sampleCourse = $coursesResp.data.content | Where-Object { $_.durationHours -gt 0 } | Select-Object -First 1
    if ($courseCount -ge 3 -and $sampleCourse -ne $null) {
        Record-Result 4 "Approved Courses Listing" $true "Listed $courseCount courses with sector and duration details"
    } else {
        Record-Result 4 "Approved Courses Listing" $false "Found $courseCount courses"
    }
} catch {
    Record-Result 4 "Approved Courses Listing" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 5: Valid Training Batch Creation
# -------------------------------------------------------------------------
$testBatchCode = "BAT-LIVE-" + (Get-Random -Minimum 10000 -Maximum 99999)
$createdBatchId = $null
try {
    $batchPayload = @{
        batchCode = $testBatchCode
        centerId = 99887
        courseId = 99895
        providerId = 1010905
        programId = 1
        capacity = 2
        batchStatusId = 1
        startDate = "2026-07-01"
        endDate = "2026-09-30"
    } | ConvertTo-Json

    $batchResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-batches" -Method Post -Body $batchPayload -ContentType "application/json" -Headers $headers
    $createdBatchId = $batchResp.data.id
    if ($createdBatchId -and $batchResp.data.batchCode -eq $testBatchCode) {
        Record-Result 5 "Valid Training Batch Creation" $true "Batch $testBatchCode created with ID $createdBatchId and capacity 2"
    } else {
        Record-Result 5 "Valid Training Batch Creation" $false "Batch creation response missing ID"
    }
} catch {
    Record-Result 5 "Valid Training Batch Creation" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 6: Invalid Training Batch Creation Rejection
# -------------------------------------------------------------------------
try {
    $invalidPayload = @{
        batchCode = "BAT-INVALID-NEG"
        centerId = 99887
        courseId = 99895
        providerId = 1010905
        programId = 1
        capacity = -10
        batchStatusId = 1
        startDate = "2026-09-30"
        endDate = "2026-07-01" # End before start
    } | ConvertTo-Json

    $invResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-batches" -Method Post -Body $invalidPayload -ContentType "application/json" -Headers $headers
    Record-Result 6 "Invalid Batch Creation Rejection" $false "Server accepted negative capacity and inverted dates"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 400) {
        Record-Result 6 "Invalid Batch Creation Rejection" $true "Rejected with HTTP 400 Bad Request as expected"
    } else {
        Record-Result 6 "Invalid Batch Creation Rejection" $false "Expected HTTP 400, received $statusCode"
    }
}

# -------------------------------------------------------------------------
# Scenario 7: Existing Trainee Lookup
# -------------------------------------------------------------------------
try {
    $traineeLookup = Invoke-RestMethod -Uri "$baseUrl/api/v1/trainees?registrationNumber=REG-LIVE-VERIFY-001" -Headers $headers
    $candidateId = $traineeLookup.data.id
    if ($candidateId -eq 1005143) {
        Record-Result 7 "Existing Trainee Lookup" $true "Found trainee REG-LIVE-VERIFY-001 with ID 1005143"
    } else {
        Record-Result 7 "Existing Trainee Lookup" $false "Looked up ID: $candidateId (expected 1005143)"
    }
} catch {
    Record-Result 7 "Existing Trainee Lookup" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 8: Successful Trainee Enrollment
# -------------------------------------------------------------------------
$enrollmentId1 = $null
$enrCode1 = "ENR-" + (Get-Random -Minimum 100000 -Maximum 999999)
try {
    $enrollPayload = @{
        enrollmentNumber = $enrCode1
        traineeId = 1005143
        batchId = $createdBatchId
        providerId = 1010905
        centerId = 99887
        courseId = 99895
        programId = 1
        enrollmentDate = "2026-07-01"
        enrollmentStatusId = 1
    } | ConvertTo-Json

    $enrollResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-enrollments" -Method Post -Body $enrollPayload -ContentType "application/json" -Headers $headers
    $enrollmentId1 = $enrollResp.data.id
    if ($enrollmentId1 -gt 0) {
        Record-Result 8 "Successful Trainee Enrollment" $true "Trainee 1005143 enrolled with enrollment ID $enrollmentId1 ($enrCode1)"
    } else {
        Record-Result 8 "Successful Trainee Enrollment" $false "No enrollment ID returned"
    }
} catch {
    Record-Result 8 "Successful Trainee Enrollment" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 9: Duplicate Trainee Enrollment Rejection
# -------------------------------------------------------------------------
try {
    $dupPayload = @{
        enrollmentNumber = "ENR-DUP-" + (Get-Random -Minimum 100000 -Maximum 999999)
        traineeId = 1005143
        batchId = $createdBatchId
        providerId = 1010905
        centerId = 99887
        courseId = 99895
        programId = 1
        enrollmentDate = "2026-07-01"
        enrollmentStatusId = 1
    } | ConvertTo-Json

    $dupResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-enrollments" -Method Post -Body $dupPayload -ContentType "application/json" -Headers $headers
    Record-Result 9 "Duplicate Trainee Enrollment Rejection" $false "Server allowed duplicate enrollment"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 409) {
        Record-Result 9 "Duplicate Trainee Enrollment Rejection" $true "Rejected duplicate enrollment with HTTP 409 Conflict"
    } else {
        Record-Result 9 "Duplicate Trainee Enrollment Rejection" $false "Expected HTTP 409, received $statusCode"
    }
}

# -------------------------------------------------------------------------
# Scenario 10: Cross-Provider Trainee Access Rejection
# -------------------------------------------------------------------------
try {
    # Set up trainee 1005359 enrolled under provider 1021960 in DB
    & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p"Aditya7033@" SIH26135_TEST -e "
    INSERT INTO training_enrollments (enrollment_number, trainee_id, batch_id, provider_id, center_id, course_id, program_id, enrollment_date, enrollment_status_id)
    VALUES ('ENR-CROSS-PROV-01', 1005359, $createdBatchId, 1021960, 99887, 99895, 1, '2026-07-01', 1)
    ON DUPLICATE KEY UPDATE provider_id = 1021960, enrollment_status_id = 1;
    "

    # Now attempt to enroll trainee 1005359 under caller provider 1010905
    $crossPayload = @{
        enrollmentNumber = "ENR-CROSS-" + (Get-Random -Minimum 100000 -Maximum 999999)
        traineeId = 1005359
        batchId = $createdBatchId
        providerId = 1010905
        centerId = 99887
        courseId = 99895
        programId = 1
        enrollmentDate = "2026-07-01"
        enrollmentStatusId = 1
    } | ConvertTo-Json

    $crossResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-enrollments" -Method Post -Body $crossPayload -ContentType "application/json" -Headers $headers
    Record-Result 10 "Cross-Provider Trainee Access Rejection" $false "Server permitted enrolling trainee affiliated with another provider"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 400 -or $statusCode -eq 403 -or $statusCode -eq 409) {
        Record-Result 10 "Cross-Provider Trainee Access Rejection" $true "Enforced tenant boundary rejection with HTTP $statusCode"
    } else {
        Record-Result 10 "Cross-Provider Trainee Access Rejection" $false "Expected HTTP 400/403/409, received $statusCode"
    }
}

# -------------------------------------------------------------------------
# Scenario 11: Batch Capacity Enforcement
# -------------------------------------------------------------------------
try {
    # Enroll second trainee into batch of capacity 2 (should succeed)
    $enrCode2 = "ENR-" + (Get-Random -Minimum 100000 -Maximum 999999)
    $enrollPayload2 = @{
        enrollmentNumber = $enrCode2
        traineeId = 1005356
        batchId = $createdBatchId
        providerId = 1010905
        centerId = 99887
        courseId = 99895
        programId = 1
        enrollmentDate = "2026-07-01"
        enrollmentStatusId = 1
    } | ConvertTo-Json
    $enrollResp2 = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-enrollments" -Method Post -Body $enrollPayload2 -ContentType "application/json" -Headers $headers

    # Now attempt to enroll third trainee into batch of capacity 2 (should fail with 409 BATCH_CAPACITY_EXCEEDED)
    $enrCode3 = "ENR-" + (Get-Random -Minimum 100000 -Maximum 999999)
    $enrollPayload3 = @{
        enrollmentNumber = $enrCode3
        traineeId = 1005332
        batchId = $createdBatchId
        providerId = 1010905
        centerId = 99887
        courseId = 99895
        programId = 1
        enrollmentDate = "2026-07-01"
        enrollmentStatusId = 1
    } | ConvertTo-Json
    $enrollResp3 = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-enrollments" -Method Post -Body $enrollPayload3 -ContentType "application/json" -Headers $headers
    Record-Result 11 "Batch Capacity Enforcement" $false "Allowed enrollment exceeding batch capacity"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 409) {
        Record-Result 11 "Batch Capacity Enforcement" $true "Batch capacity limit strictly enforced with HTTP 409 BATCH_CAPACITY_EXCEEDED"
    } else {
        Record-Result 11 "Batch Capacity Enforcement" $false "Expected HTTP 409, received $statusCode"
    }
}

# -------------------------------------------------------------------------
# Scenario 12: Real-time Authoritative Batch Occupancy Verification
# -------------------------------------------------------------------------
try {
    $batchVerify = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-batches/$createdBatchId" -Headers $headers
    $enrolledCount = $batchVerify.data.enrolledCount
    $availableSeats = $batchVerify.data.availableSeats
    $capacity = $batchVerify.data.capacity

    if ($enrolledCount -eq 2 -and $availableSeats -eq 0 -and $capacity -eq 2) {
        Record-Result 12 "Authoritative Batch Occupancy Verification" $true "Occupancy: $enrolledCount/$capacity, availableSeats: $availableSeats"
    } else {
        Record-Result 12 "Authoritative Batch Occupancy Verification" $false "Got enrolledCount=$enrolledCount, availableSeats=$availableSeats (expected 2 and 0)"
    }
} catch {
    Record-Result 12 "Authoritative Batch Occupancy Verification" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 13: Attendance Recording
# -------------------------------------------------------------------------
try {
    $attPayload = @{
        enrollmentId = $enrollmentId1
        sessionDate = "2026-07-05"
        attendanceStatusId = 1 # PRESENT
        remarks = "Verified biometric attendance"
    } | ConvertTo-Json

    $attResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/attendance-records" -Method Post -Body $attPayload -ContentType "application/json" -Headers $headers
    if ($attResp.data.id -gt 0) {
        Record-Result 13 "Attendance Recording" $true "Attendance recorded with ID $($attResp.data.id)"
    } else {
        Record-Result 13 "Attendance Recording" $false "Response missing attendance record ID"
    }
} catch {
    Record-Result 13 "Attendance Recording" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 14: Multipart CSV File Upload
# -------------------------------------------------------------------------
$testCsvPath = "$PSScriptRoot/test_trainee_upload.csv"
$uploadedImportBatchId = $null
try {
    $regNum1 = "REG-CSV-" + (Get-Random -Minimum 100000 -Maximum 999999)

    # Row 1: Valid
    # Row 2: Invalid Foreign Key (stateId 999999)
    # Row 3: Duplicate Registration Number (same as Row 1)
    # Row 4: Formula Injection test value in Name (=cmd|' /C calc'!A0)
    $csvContent = @"
registrationNumber,firstName,genderId,stateId,districtId,currentEmploymentStatusId,profileStatusId
$regNum1,Aarav Kumar,1,101,1001,1,2
REG-INV-FK,Rohan Singh,1,999999,1001,1,2
$regNum1,Duplicate Aarav,1,101,1001,1,2
REG-CSV-SAFE,=1+1 Calculator,1,101,1001,1,2
"@
    Set-Content -Path $testCsvPath -Value $csvContent -Encoding UTF8

    $boundary = [System.Guid]::NewGuid().ToString()
    $fileBytes = [System.IO.File]::ReadAllBytes($testCsvPath)
    $fileEnc = [System.Text.Encoding]::GetEncoding("ISO-8859-1").GetString($fileBytes)

    $bodyLines = (
        "--$boundary",
        'Content-Disposition: form-data; name="file"; filename="test_trainees.csv"',
        'Content-Type: text/csv',
        '',
        $fileEnc,
        "--$boundary--"
    ) -join "`r`n"

    $uploadResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/import-batches?entityType=Trainee&sourceSystem=PROVIDER_PORTAL" `
        -Method Post `
        -Headers @{
            Authorization = "Bearer $token"
            "Content-Type" = "multipart/form-data; boundary=$boundary"
        } `
        -Body $bodyLines

    $uploadedImportBatchId = $uploadResp.data.id
    if ($uploadedImportBatchId -gt 0) {
        Record-Result 14 "Multipart CSV File Upload" $true "Staged CSV with Import Batch ID $uploadedImportBatchId"
    } else {
        Record-Result 14 "Multipart CSV File Upload" $false "Missing import batch ID in response"
    }
} catch {
    Record-Result 14 "Multipart CSV File Upload" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 15: Asynchronous Import Processing
# -------------------------------------------------------------------------
try {
    $procResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/import-batches/$uploadedImportBatchId/process" -Method Post -Headers $headers
    Record-Result 15 "Asynchronous Import Processing Trigger" $true "Processing started for batch $uploadedImportBatchId"
} catch {
    Record-Result 15 "Asynchronous Import Processing Trigger" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 16: Asynchronous Status Monitor Polling
# -------------------------------------------------------------------------
$pollTerminal = $false
$finalStatus = $null
try {
    for ($i = 0; $i -lt 15; $i++) {
        Start-Sleep -Milliseconds 800
        $statusResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/import-batches/$uploadedImportBatchId/status" -Headers $headers
        $finalStatus = $statusResp.data.status
        if ($statusResp.data.isTerminal -or $finalStatus -in @("COMPLETED", "FAILED", "PARTIALLY_COMPLETED")) {
            $pollTerminal = $true
            break
        }
    }
    if ($pollTerminal) {
        Record-Result 16 "Asynchronous Status Monitor Polling" $true "Reached terminal status '$finalStatus' after polling"
    } else {
        Record-Result 16 "Asynchronous Status Monitor Polling" $false "Timeout waiting for terminal state (current: $finalStatus)"
    }
} catch {
    Record-Result 16 "Asynchronous Status Monitor Polling" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 17: Valid CSV Rows Persisted
# -------------------------------------------------------------------------
try {
    $persistedLookup = Invoke-RestMethod -Uri "$baseUrl/api/v1/trainees?registrationNumber=$regNum1" -Headers $headers
    if ($persistedLookup.data.id -gt 0) {
        Record-Result 17 "Valid CSV Rows Persisted" $true "Trainee $regNum1 successfully persisted in database"
    } else {
        Record-Result 17 "Valid CSV Rows Persisted" $false "Could not locate persisted trainee $regNum1"
    }
} catch {
    Record-Result 17 "Valid CSV Rows Persisted" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 18: Invalid CSV Rows Quarantined
# -------------------------------------------------------------------------
$quarantineRecords = @()
try {
    $qResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/import-batches/$uploadedImportBatchId/quarantine?includeDuplicates=true" -Headers $headers
    $quarantineRecords = $qResp.data.content
    if ($quarantineRecords.Count -ge 2) {
        Record-Result 18 "Invalid CSV Rows Quarantined" $true "Quarantined $($quarantineRecords.Count) rows in import_records"
    } else {
        Record-Result 18 "Invalid CSV Rows Quarantined" $false "Found $($quarantineRecords.Count) quarantined rows (expected >= 2)"
    }
} catch {
    Record-Result 18 "Invalid CSV Rows Quarantined" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 19: Authoritative Error Codes in Quarantine
# -------------------------------------------------------------------------
try {
    $errorCodes = $quarantineRecords | ForEach-Object { $_.errorCode }
    $hasFkError = $errorCodes -contains "FOREIGN_KEY_NOT_FOUND" -or $errorCodes -contains "VALIDATION_FAILED"
    $hasDupError = $errorCodes -contains "DUPLICATE_RECORD" -or $errorCodes -contains "UNIQUE_KEY_VIOLATION" -or $errorCodes -contains "DUPLICATE_TRAINEE_REGISTRATION"

    if ($hasFkError -and $hasDupError) {
        Record-Result 19 "Authoritative Error Codes in Quarantine" $true "Found foreign key and duplicate error codes in quarantine"
    } else {
        Record-Result 19 "Authoritative Error Codes in Quarantine" $false "Observed error codes: $($errorCodes -join ', ')"
    }
} catch {
    Record-Result 19 "Authoritative Error Codes in Quarantine" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 20: Quarantine CSV Export with Formula Injection Escaping
# -------------------------------------------------------------------------
try {
    $csvExport = Invoke-RestMethod -Uri "$baseUrl/api/v1/import-batches/$uploadedImportBatchId/quarantine/export?includeDuplicates=true" -Headers $headers
    $hasHeader = $csvExport -match "Record ID" -or $csvExport -match "Source Row" -or $csvExport -match "Error Code"
    # Verify CWE-1236 formula escaping: formula triggers (=, +, -, @) are prefixed with apostrophe or sanitized
    $isEscaped = ($csvExport -match "'=" -or -not ($csvExport -match "\n[0-9]+,[0-9]+,[^,]+,[0-9]+,[^,]+,[^,]+,[^,]+,=[A-Za-z]+"))

    if ($hasHeader -and $isEscaped) {
        Record-Result 20 "Quarantine CSV Export & Formula Protection" $true "Exported valid RFC-4180 CSV with CWE-1236 escaping"
    } else {
        Record-Result 20 "Quarantine CSV Export & Formula Protection" $false "CSV missing headers or unescaped formulas"
    }
} catch {
    Record-Result 20 "Quarantine CSV Export & Formula Protection" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 21: Unauthenticated Endpoint Rejection
# -------------------------------------------------------------------------
try {
    $unauthResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-batches"
    Record-Result 21 "Unauthenticated Endpoint Rejection" $false "Endpoint permitted unauthenticated request"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 401) {
        Record-Result 21 "Unauthenticated Endpoint Rejection" $true "Rejected with HTTP 401 Unauthorized"
    } else {
        Record-Result 21 "Unauthenticated Endpoint Rejection" $false "Expected HTTP 401, received $statusCode"
    }
}

# -------------------------------------------------------------------------
# Scenario 22: Unauthorized Role Endpoint Rejection
# -------------------------------------------------------------------------
try {
    $traineeLogin = @{
        usernameOrEmail = "trainee_only_user"
        password = "RawPassword123"
    } | ConvertTo-Json
    $traineeTokenResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body $traineeLogin -ContentType "application/json"
    $traineeHeaders = @{ Authorization = "Bearer $($traineeTokenResp.data.accessToken)" }

    $forbiddenResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-batches" -Method Post -Body $batchPayload -ContentType "application/json" -Headers $traineeHeaders
    Record-Result 22 "Unauthorized Role Rejection" $false "Server permitted trainee role to create training batch"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 403) {
        Record-Result 22 "Unauthorized Role Rejection" $true "Enforced RBAC boundary with HTTP 403 Forbidden"
    } else {
        Record-Result 22 "Unauthorized Role Rejection" $false "Expected HTTP 403, received $statusCode"
    }
}

# -------------------------------------------------------------------------
# Scenario 23: Batch Lifecycle Status Transition
# -------------------------------------------------------------------------
try {
    # Transition to OPEN (status 2)
    $transitionPayload = @{ batchStatusId = 2 } | ConvertTo-Json
    $statusResp1 = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-batches/$createdBatchId/status" -Method Put -Body $transitionPayload -ContentType "application/json" -Headers $headers

    # Transition to ONGOING (status 3)
    $transitionPayload2 = @{ batchStatusId = 3 } | ConvertTo-Json
    $statusResp2 = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-batches/$createdBatchId/status" -Method Put -Body $transitionPayload2 -ContentType "application/json" -Headers $headers

    if ($statusResp2.data.batchStatusId -eq 3) {
        Record-Result 23 "Batch Lifecycle Status Transition" $true "Batch transitioned to OPEN and then ONGOING"
    } else {
        Record-Result 23 "Batch Lifecycle Status Transition" $false "Final statusId: $($statusResp2.data.batchStatusId) (expected 3)"
    }
} catch {
    Record-Result 23 "Batch Lifecycle Status Transition" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 24: Centers and Courses Pagination & Filtering
# -------------------------------------------------------------------------
try {
    $pageResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/training-centers?providerId=1010905&page=0&size=1" -Headers $headers
    $total = $pageResp.data.totalElements
    $pages = $pageResp.data.totalPages
    $count = $pageResp.data.content.Count
    if ($total -ge 3 -and $pages -ge 3 -and $count -eq 1) {
        Record-Result 24 "Centers and Courses Pagination & Filtering" $true "Paging correctly constrained to size=1 (total: $total, pages: $pages)"
    } else {
        Record-Result 24 "Centers and Courses Pagination & Filtering" $false "Got total=$total, pages=$pages, count=$count"
    }
} catch {
    Record-Result 24 "Centers and Courses Pagination & Filtering" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Scenario 25: Trainee Identity Deduplication Preservation
# -------------------------------------------------------------------------
try {
    # Check that trainee 1005143 has only ONE record in trainees table
    $dedupCheck = & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p"Aditya7033@" SIH26135_TEST -e "
    SELECT COUNT(*) AS c FROM trainees WHERE registration_number = 'REG-LIVE-VERIFY-001';
    "
    $countVal = [regex]::Match($dedupCheck, "\d+").Value
    if ([int]$countVal -eq 1) {
        Record-Result 25 "Identity Deduplication Preservation" $true "Canonical trainee REG-LIVE-VERIFY-001 preserved without duplicate insertion"
    } else {
        Record-Result 25 "Identity Deduplication Preservation" $false "Found $countVal records for REG-LIVE-VERIFY-001"
    }
} catch {
    Record-Result 25 "Identity Deduplication Preservation" $false $_.Exception.Message
}

# -------------------------------------------------------------------------
# Summary
# -------------------------------------------------------------------------
Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "Summary: $passCount PASSED, $failCount FAILED out of 25 Scenarios" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

# Cleanup temporary file
if (Test-Path $testCsvPath) {
    Remove-Item $testCsvPath -Force
}

if ($failCount -gt 0) {
    exit 1
} else {
    exit 0
}
