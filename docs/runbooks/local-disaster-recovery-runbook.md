# SIH-26135 Local Disaster Recovery Runbook

**Document**: Local Disaster Recovery Operational Runbook  
**Target Environment**: Windows 11 / Host Workstation (Local-PC Execution)  
**Applicability**: Backend Engineers, Evaluators, Technical Leads  
**Last Verified**: November 2026 (Phase 29.14 — Final Corrections)

---

## 1. Scope & Incident Categorization

This runbook establishes a clear, step-by-step recovery procedure for realistic local developer workstation disruptions. 

### Local Failure Scenarios Covered:
1. **Workstation Crash or Power Loss**: Sudden machine reboot during database writes or file imports.
2. **Database State Corruption or Accidental Truncation**: Dropped tables, bad migrations, or test fixture pollution.
3. **Accidental Deletion of Source or Build Artifacts**: Deleted files, corrupted classes, or missing dependencies.
4. **Corrupted Local Maven Repository Cache**: Partial `.m2` jar downloads causing compilation crashes.
5. **Configuration Drift / Broken Environment Variables**: Missing or corrupted shell environment settings.
6. **Failed or Orphaned File Ingestion Staging**: Half-written multipart files locking disk space.

> [!IMPORTANT]
> **Out of Scope**: Multi-region cloud failover, Kubernetes node evacuations, and cross-data-center replication are intentionally excluded. Local disaster recovery focuses strictly on bringing the single host workstation back to a healthy, verified operational baseline.

---

## 2. Master Local Disaster Recovery Sequence

```
+---------------------------------------------------------------------------------------------------+
|                            MASTER LOCAL RECOVERY WORKFLOW                                         |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|  [Step 1] Stop Backend Process  ->  [Step 2] Preserve Evidence  ->  [Step 3] Identify Failure     |
|      (Port 8080 specific)                 (Logs & Crash Dumps)            (DB, JVM, Files, Env)   |
|                                                                                   |               |
|  [Step 6] Restore Config        <-  [Step 5] Restart MySQL      <-  [Step 4] Verify Git Source    |
|      (Protected Options File)             (Restart-Service)               (git status / diff)     |
|         |                                                                                         |
|         v                                                                                         |
|  [Step 7 & 8] Unified Scratch Restore & Invariant Assertion  ->  [ROUTINE DRILL STOPS HERE]       |
|      (Explicit backup path, non-destructive create, real checks)     (SIH26135 Untouched)         |
|                                                                                   |               |
|  .................................................................................|.............. |
|  : ONLY IN TRUE EMERGENCY (Fatal Corruption in SIH26135):                         v               :
|  : [Step 9] Exceptional Primary Recovery  --->  [Step 10] Rebuild & Restart  --->  [Step 11]     :
|  :   (Snapshot, Auth Token, Overwrite)            (clean compile / run)             Audit Health  :
|  :................................................................................................:
+---------------------------------------------------------------------------------------------------+
```

---

### Step 1: Safely Terminate Lingering Application Processes

Terminate the specific backend process holding port 8080 without indiscriminately killing unrelated Java processes (such as IDEs or build daemons):

```powershell
# Locate the specific process listening on backend port 8080
$backendConn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($backendConn) {
    $backendPid = $backendConn.OwningProcess
    Write-Host "Stopping backend process listening on port 8080 (PID: $backendPid)..."
    Stop-Process -Id $backendPid -Force
} else {
    Write-Host "No process currently occupying port 8080." -ForegroundColor Gray
}
```

---

### Step 2: Preserve Diagnostic Evidence

Before wiping temporary state, preserve existing logs and crash dumps for root-cause analysis:

```powershell
$recoveryDir = "E:\project\recovery_artifacts_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
New-Item -ItemType Directory -Path $recoveryDir -Force

# Copy application logs
if (Test-Path "backend\logs") {
    Copy-Item -Path "backend\logs\*" -Destination $recoveryDir -Recurse -Force
}

# Copy surefire failure reports if applicable
if (Test-Path "backend\target\surefire-reports") {
    Copy-Item -Path "backend\target\surefire-reports\*.txt" -Destination $recoveryDir -Force
}
Write-Host "Diagnostic evidence preserved in: $recoveryDir"
```

---

### Step 3: Identify the Failure Domain

Determine which subsystem failed:
- **Database Layer**: Can `mysqladmin ping` reach MySQL? Is `MySQL80` running?
- **Build / Source Layer**: Are files missing or uncommitted? Did Maven compilation fail?
- **Configuration Layer**: Are required environment variables (`JWT_SECRET`, database options) available?
- **Staging / Storage Layer**: Is disk space exhausted?

---

### Step 4: Verify Project Source Integrity

Check the Git repository state to ensure source code is intact:

```powershell
# Verify working tree status
git status

# Verify zero whitespace defects or conflict markers
git diff --check
```

---

### Step 5: Verify & Restart MySQL Database Service

If the MySQL server was interrupted by a machine reboot or power loss, restart the service:

```powershell
# Restart MySQL service
Restart-Service -Name "MySQL80"

# Verify service is running
Get-Service -Name "MySQL80"

# Verify responsiveness
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe" -u root -h localhost ping
if ($LASTEXITCODE -ne 0) {
    throw "MySQL service is unresponsive (exit code $LASTEXITCODE)."
}
```

---

### Step 6: Set Up Protected Authentication & Configuration

Prompt securely for database credentials without echoing to shell history or hardcoding passwords. Create a temporary MySQL options file with proper escaping:

```powershell
$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin"

# Prompt securely for password (never hardcode in scripts)
$securePassword = Read-Host -Prompt "Enter MySQL root password" -AsSecureString

# Create protected temporary options file in %TEMP%
$tempCnf = [System.IO.Path]::GetTempFileName()
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

# Escape backslashes and double quotes for MySQL option-file syntax
$escapedPassword = $plainPassword.Replace('\', '\\').Replace('"', '\"')
$plainPassword = $null

Set-Content -Path $tempCnf -Value @"
[client]
user=root
password="$escapedPassword"
host=localhost
port=3306
default-character-set=utf8mb4
"@
$escapedPassword = $null

# Set application environment variables
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "SIH26135"
$env:DB_USERNAME = "root"

# Configure JWT Secret (Must be >= 32 bytes / 256 bits). Prompt interactively:
$env:JWT_SECRET = Read-Host -Prompt "Enter JWT Secret (minimum 32 characters / 256 bits)"
$env:COMMUNICATION_PROVIDER_MODE = "MOCK"
```

> [!NOTE]
> **Physical Erasure Reality**: Overwriting and deleting a temporary file in user space zero-fills sectors at the filesystem layer. On wear-leveled SSDs or journaled volumes, physical flash blocks may retain residual data until trimmed or garbage-collected. Where stricter host isolation is required, prefer MySQL's persistent encrypted login path (`mysql_config_editor`), which stores AES-encrypted login keys in `%APPDATA%\MySQL\.mylogin.cnf`.

---

### Steps 7 & 8: Unified Scratch Database Restore & Real Invariant Assertion

> [!CAUTION]
> **NEVER RESTORE DIRECTLY OVER `SIH26135` DURING ROUTINE DRILLS.**
> To prevent leaving orphaned databases if an error occurs, scratch creation, dump import, invariant assertions, and cleanup are managed within a **single unified `try / finally` block**.

```powershell
# 1. Explicit Operator Backup Selection (Never select arbitrary newest files)
$backupFile = "E:\project\database\backups\SIH26135_backup_verified.sql"
# Or prompt: $backupFile = Read-Host -Prompt "Enter full path to verified backup file"

if (!(Test-Path $backupFile)) {
    throw "Backup file not found at: $backupFile"
}
$fileInfo = Get-Item $backupFile
if ($fileInfo.Length -lt 1000000) {
    throw "Backup file size is suspiciously small ($($fileInfo.Length) bytes). Verification aborted."
}

# Inspect backup provenance metadata
$headerLines = Get-Content -Path $backupFile -TotalCount 25
$provenanceMatch = $headerLines | Select-String -Pattern "Host:.*Database:\s*([a-zA-Z0-9_]+)"
if ($provenanceMatch) {
    Write-Host "Backup Provenance Confirmed: Target Catalog = $($provenanceMatch.Matches[0].Groups[1].Value)" -ForegroundColor Cyan
}

# 2. Generate Unique Scratch Database Name
$uniqueId = (Get-Date -Format "yyyyMMdd_HHmmss") + "_" + ([System.Guid]::NewGuid().ToString("N").Substring(0, 8))
$scratchDb = "sih26135_scratch_" + $uniqueId
$sessionCreatedScratch = $null

try {
    # 3. Fail-Closed Existence Check
    $exists = & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -N -B -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = '$scratchDb';"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to query information_schema.schemata (exit code $LASTEXITCODE)."
    }
    if ([int]$exists -ne 0) {
        throw "Safety Violation: Database '$scratchDb' already exists! Aborting."
    }

    # 4. Non-Destructive Creation (Never execute DROP DATABASE IF EXISTS)
    & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -e "CREATE DATABASE $scratchDb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to create scratch database '$scratchDb' (exit code $LASTEXITCODE)."
    }
    # Record ownership strictly after verified successful creation
    $sessionCreatedScratch = $scratchDb
    Write-Host "Created unique scratch database: $sessionCreatedScratch" -ForegroundColor Cyan

    # 5. Restore dump into scratch database
    $mysqlExe = "$mysqlBin\mysql.exe"
    cmd.exe /c """$mysqlExe"" --defaults-extra-file=""$tempCnf"" $scratchDb < ""$backupFile"""
    if ($LASTEXITCODE -ne 0) {
        throw "Scratch database restoration failed with exit code $LASTEXITCODE"
    }

    # 6. Real Invariant Assertions (Asserting actual query values, not just execution status)
    # Universal Restore Requirement: Expected schema table count
    $totalTables = [int](& "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -N -B -e "SELECT count(*) FROM information_schema.tables WHERE table_schema='$scratchDb';")
    if ($LASTEXITCODE -ne 0 -or $totalTables -lt 160) {
        throw "Invariant Assertion Failed: Expected at least 160 tables, found $totalTables"
    }

    # Expected Demonstration Fixture: Authoritative Trainee
    $traineeCount = [int](& "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -N -B -e "SELECT count(*) FROM ${scratchDb}.trainees WHERE trainee_id=1005143 AND registration_number='REG-LIVE-VERIFY-001';")
    if ($LASTEXITCODE -ne 0 -or $traineeCount -ne 1) {
        throw "Invariant Assertion Failed: Authoritative trainee 1005143 ('REG-LIVE-VERIFY-001') not found."
    }

    # Expected Demonstration Fixture: DPDP Consents
    $consentCount = [int](& "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -N -B -e "SELECT count(*) FROM ${scratchDb}.trainee_consents WHERE trainee_id=1005143;")
    if ($LASTEXITCODE -ne 0 -or $consentCount -ne 5) {
        throw "Invariant Assertion Failed: Expected 5 DPDP consents for trainee 1005143, found $consentCount."
    }

    # Expected Demonstration Fixture: Root Administrator
    $adminCount = [int](& "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -N -B -e "SELECT count(*) FROM ${scratchDb}.users WHERE user_id=1 AND username='admin';")
    if ($LASTEXITCODE -ne 0 -or $adminCount -ne 1) {
        throw "Invariant Assertion Failed: Root admin user 1 ('admin') not found."
    }

    Write-Host "Scratch verification passed. Invariants verified: $totalTables tables, trainee 1005143, 5 consents, admin user present." -ForegroundColor Green

} finally {
    # 7. Guaranteed Owned Cleanup: Only drop the exact database created by this session
    if ($sessionCreatedScratch -and ($sessionCreatedScratch -eq $scratchDb)) {
        & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -e "DROP DATABASE $sessionCreatedScratch;"
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Cleaned up owned scratch database: $sessionCreatedScratch" -ForegroundColor Gray
            $sessionCreatedScratch = $null
        } else {
            Write-Warning "CRITICAL: Cleanup failed for scratch database '$sessionCreatedScratch'. Manual DROP required."
        }
    }
}

Write-Host "Routine verification drill COMPLETE. Primary database SIH26135 was NOT modified." -ForegroundColor Green
```

> [!IMPORTANT]
> **STOP HERE FOR ROUTINE DISASTER RECOVERY DRILLS.**
> The backup dump is verified. Step 9 below is reserved solely for catastrophic production incidents where `SIH26135` has suffered verified data loss.

---

### Step 9: Exceptional Primary Database Restoration (Workflow B — Emergency Operator Procedure Only)

Execute this step **ONLY** if the primary database `SIH26135` has experienced verified data corruption:

1. **Verify Backend Application Stoppage**:
   ```powershell
   $activeConn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
   if ($activeConn) {
       throw "SAFETY VIOLATION: Backend application is still running on port 8080 (PID $($activeConn.OwningProcess))! Stop it before restore."
   }
   ```

2. **Mandatory Pre-Recovery Snapshot**:
   Take a snapshot of the current primary database state (even if corrupted) before touching it:
   ```powershell
   $preRecoveryFile = "E:\project\database\backups\pre_recovery_snapshot_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
   & "$mysqlBin\mysqldump.exe" --defaults-extra-file="$tempCnf" --single-transaction --quick --result-file="$preRecoveryFile" SIH26135
   if ($LASTEXITCODE -ne 0 -or !(Test-Path $preRecoveryFile) -or (Get-Item $preRecoveryFile).Length -lt 10000) {
       throw "CRITICAL: Pre-recovery snapshot failed! Aborting primary restoration to prevent unrecoverable data loss."
   }
   Write-Host "Pre-recovery snapshot preserved at: $preRecoveryFile" -ForegroundColor Green
   ```

3. **In-Place Schema Warning**:
   > [!WARNING]
   > Importing a SQL dump into an existing database does not remove extraneous tables, columns, or triggers created after the backup was taken. For a clean restoration, verify that the dump includes `DROP TABLE IF EXISTS` (standard in `mysqldump`), or confirm dropping and recreating the empty database catalog before restore.

4. **Explicit Operator Confirmation Gate**:
   ```powershell
   $confirmTarget = Read-Host "Enter the exact name of the target database to restore"
   if ($confirmTarget -ne "SIH26135") {
       throw "Target database mismatch ('$confirmTarget' != 'SIH26135'). Restoration aborted."
   }

   $confirmToken = Read-Host "DANGER: You are about to overwrite primary database SIH26135. Type 'RESTORE_SIH26135_CONFIRMED' to proceed"
   if ($confirmToken -ne "RESTORE_SIH26135_CONFIRMED") {
       Write-Host "Primary restoration aborted by operator. SIH26135 was NOT modified." -ForegroundColor Yellow
       exit 1
   }

   # Execute primary database overwrite
   $mysqlExe = "$mysqlBin\mysql.exe"
   cmd.exe /c """$mysqlExe"" --defaults-extra-file=""$tempCnf"" SIH26135 < ""$backupFile"""
   if ($LASTEXITCODE -ne 0) {
       Write-Error "Primary restoration failed! Immediately halt and restore pre-recovery snapshot from: $preRecoveryFile"
       exit 1
   }
   Write-Host "Primary database SIH26135 restored successfully." -ForegroundColor Green
   ```

---

### Step 10: Rebuild & Restart Spring Boot Backend

Perform a clean build to eliminate any corrupted class files or build artifacts:

```powershell
# Clean and recompile
mvn -f backend/pom.xml clean compile

# Launch with local demo profile
mvn -f backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=local-demo"
```

---

### Step 11: Operational Health & API Verification

Perform final post-recovery checks:

```powershell
# 1. Check Liveness Probe
curl -i http://localhost:8080/actuator/health/liveness
# Expected: HTTP 200 {"status":"UP"}

# 2. Check Readiness Probe
curl -i http://localhost:8080/actuator/health/readiness
# Expected: HTTP 200 {"status":"UP"}

# 3. Test Authentication (Using verified LoginRequest contract: usernameOrEmail + password)
$adminPassword = Read-Host -Prompt "Enter local demo administrator password"
$loginPayload = @{
    usernameOrEmail = "admin"
    password        = $adminPassword
} | ConvertTo-Json

$authResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body $loginPayload -ContentType "application/json"
$token = $authResponse.data.accessToken

if ($token) {
    # 4. Verify Authoritative Trainee Endpoint
    $trainee = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/trainees/1005143" -Method Get -Headers @{Authorization="Bearer $token"}
    Write-Host "Verified Trainee: $($trainee.data.firstName) $($trainee.data.lastName)" -ForegroundColor Green
} else {
    Write-Warning "Authentication failed or token was null. Verify administrator credentials."
}

# 5. Clean up temporary option file
if (Test-Path $tempCnf) {
    Set-Content -Path $tempCnf -Value ""
    Remove-Item -Path $tempCnf -Force
}
```

System disaster recovery verification is complete.
