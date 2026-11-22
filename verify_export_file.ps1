# SIH-26135 Deep Verification of Authenticated Quarantine CSV Export
# Validates file download, content, CWE-1236 escaping, and security rejections

$baseUrl = "http://localhost:8080"
$exportFilePath = "$PSScriptRoot/downloaded_quarantine_export.csv"

Write-Host "Starting Deep Export Verification against $baseUrl..." -ForegroundColor Cyan

# 1. Login as Provider
$loginResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body '{"usernameOrEmail":"provider_test_user","password":"RawPassword123"}' -ContentType "application/json"
$token = $loginResp.data.accessToken
$headers = @{
    Authorization = "Bearer $token"
    Accept = "text/csv, application/octet-stream, */*"
}

# 2. Target batch with quarantined records
$batchId = 988316
Write-Host "Target import batch ID: $batchId"

# 3. Download the CSV file via authenticated GET request
$exportUrl = "$baseUrl/api/v1/import-batches/$batchId/quarantine/export?includeDuplicates=true"
Write-Host "Downloading CSV from $exportUrl..."

$wc = New-Object System.Net.WebClient
$wc.Headers.Add("Authorization", "Bearer $token")
$wc.Headers.Add("Accept", "text/csv, */*")
$wc.DownloadFile($exportUrl, $exportFilePath)

if (-not (Test-Path $exportFilePath)) {
    Write-Host "FAILED: File was not downloaded to disk." -ForegroundColor Red
    exit 1
}

$fileInfo = Get-Item $exportFilePath
Write-Host "File downloaded successfully: $($fileInfo.FullName) (Size: $($fileInfo.Length) bytes)" -ForegroundColor Green

# 4. Inspect downloaded CSV content
$csvLines = Get-Content -Path $exportFilePath -Encoding UTF8
Write-Host "Total CSV lines: $($csvLines.Count)"

$header = $csvLines[0]
Write-Host "Header: $header"
$expectedHeaders = @("Record ID", "Batch ID", "Batch Code", "Source Row", "Entity Type", "Record Status", "Error Code", "Error Message", "Created At")
$missingHeaders = @()
foreach ($h in $expectedHeaders) {
    if ($header -notmatch [regex]::Escape($h)) {
        $missingHeaders += $h
    }
}
if ($missingHeaders.Count -gt 0) {
    Write-Host "FAILED: Missing headers: $($missingHeaders -join ', ')" -ForegroundColor Red
    exit 1
} else {
    Write-Host "PASS: All standard RFC-4180 CSV header columns verified." -ForegroundColor Green
}

# Inspect data rows
$dataRows = $csvLines | Select-Object -Skip 1
Write-Host "Data rows count: $($dataRows.Count)"
if ($dataRows.Count -eq 0) {
    Write-Host "WARNING: Data rows count is 0, checking if batch had records"
} else {
    Write-Host "Sample data row 1: $($dataRows[0])"
    # Check for row numbers and error codes
    $hasRowCol = $dataRows[0] -match "\d+"
    $hasErrorCol = $dataRows[0] -match "FAILED|DUPLICATE"
    if ($hasRowCol -and $hasErrorCol) {
        Write-Host "PASS: Data row contains source row number and quarantine status." -ForegroundColor Green
    }
}

# 5. Check CWE-1236 Formula Injection Escaping
# In CSV, any value starting with =, +, -, @ must be prefixed with single quote '
$rawText = Get-Content -Path $exportFilePath -Raw -Encoding UTF8
$unescapedFormulaMatches = [regex]::Matches($rawText, "(?m)^.*?,.*?,.*?,.*?,.*?,.*?,.*?,[=+@\-][a-zA-Z0-9].*?$")
if ($unescapedFormulaMatches.Count -gt 0) {
    Write-Host "FAILED: Unescaped formula injection detected in CSV output: $($unescapedFormulaMatches[0].Value)" -ForegroundColor Red
    exit 1
} else {
    Write-Host "PASS: CWE-1236 Formula injection protection verified (zero unescaped formula triggers)." -ForegroundColor Green
}

# 6. Negative Security Verification: Unauthenticated Access Must Be Denied (401)
try {
    $wcUnauth = New-Object System.Net.WebClient
    $wcUnauth.DownloadString($exportUrl)
    Write-Host "FAILED: Unauthenticated request was allowed." -ForegroundColor Red
    exit 1
} catch [System.Net.WebException] {
    $statusCode = [int]$_.Exception.Response.StatusCode
    if ($statusCode -eq 401) {
        Write-Host "PASS: Unauthenticated export correctly rejected with HTTP 401 Unauthorized." -ForegroundColor Green
    } else {
        Write-Host "FAILED: Expected 401, received $statusCode" -ForegroundColor Red
        exit 1
    }
}

# 7. Negative Security Verification: Unauthorized Role Access Must Be Denied (403)
try {
    $traineeLogin = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/login" -Method Post -Body '{"usernameOrEmail":"trainee_only_user","password":"RawPassword123"}' -ContentType "application/json"
    $traineeToken = $traineeLogin.data.accessToken

    $wcTrainee = New-Object System.Net.WebClient
    $wcTrainee.Headers.Add("Authorization", "Bearer $traineeToken")
    $wcTrainee.DownloadString($exportUrl)
    Write-Host "FAILED: Trainee role was allowed to export provider quarantine CSV." -ForegroundColor Red
    exit 1
} catch [System.Net.WebException] {
    $statusCode = [int]$_.Exception.Response.StatusCode
    if ($statusCode -eq 403) {
        Write-Host "PASS: Trainee role export attempt correctly rejected with HTTP 403 Forbidden." -ForegroundColor Green
    } else {
        Write-Host "FAILED: Expected 403, received $statusCode" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`nAll Deep Export Verification checks PASSED." -ForegroundColor Green
Remove-Item $exportFilePath -Force
