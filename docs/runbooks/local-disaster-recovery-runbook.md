# SIH-26135 Local Disaster Recovery Runbook

**Document**: Local Disaster Recovery Operational Runbook  
**Target Environment**: Windows 11 / Host Workstation (Local-PC Execution)  
**Applicability**: Backend Engineers, Evaluators, Technical Leads  
**Last Verified**: November 2026 (Phase 29.14 — Targeted Corrections)

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
|  [Step 1] Stop Application  --->  [Step 2] Preserve Evidence  --->  [Step 3] Identify Failure     |
|      (Stop-Process)                     (Logs & Crash Dumps)              (DB, JVM, Files, Env)   |
|                                                                                   |               |
|  [Step 6] Restore Config   <---   [Step 5] Restart MySQL     <---   [Step 4] Verify Git Source    |
|      (Protected Options File)           (Restart-Service)                 (git status / diff)     |
|         |                                                                                         |
|         v                                                                                         |
|  [Step 7] Scratch DB Restore  ->  [Step 8] Scratch Invariant Check -> [ROUTINE DRILL STOPS HERE]  |
|      (Unique name, non-destr.)          (168 tables, trainee 1005143)    (SIH26135 Untouched)     |
|                                                                                   |               |
|  .................................................................................|.............. |
|  : ONLY IN EMERGENCY (Data Loss in SIH26135):                                     v               :
|  : [Step 9] Exceptional Primary Recovery  --->  [Step 10] Rebuild & Restart  --->  [Step 11]     :
|  :   (Snapshot, Auth Token, Overwrite)            (clean compile / run)             Audit Health  :
|  :................................................................................................:
+---------------------------------------------------------------------------------------------------+
```

---

### Step 1: Safely Terminate Lingering Application Processes

If the application is hung, looping, or unresponsive, cleanly terminate any residual Java processes:

```powershell
# Find PIDs listening on backend port 8080
$pids = (Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue).OwningProcess | Select-Object -Unique

foreach ($pid in $pids) {
    Write-Host "Stopping lingering backend process PID: $pid"
    Stop-Process -Id $pid -Force
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
# Expected: mysqld is alive
```

---

### Step 6: Set Up Protected Authentication & Configuration

Obtain credentials interactively and configure a protected temporary option file (or use `mysql_config_editor`):

```powershell
$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin"
$securePassword = Read-Host -Prompt "Enter MySQL root password" -AsSecureString

$tempCnf = [System.IO.Path]::GetTempFileName()
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

Set-Content -Path $tempCnf -Value "[client]`nuser=root`npassword=$plainPassword`nhost=localhost`nport=3306`ndefault-character-set=utf8mb4"
$plainPassword = $null

# Set application environment variables
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "SIH26135"
$env:DB_USERNAME = "root"
$env:JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
$env:COMMUNICATION_PROVIDER_MODE = "MOCK"
```

---

### Step 7: Restore Database into Unique Scratch Catalog First (Non-Destructive)

> [!CAUTION]
> **NEVER RESTORE DIRECTLY OVER `SIH26135` DURING ROUTINE DRILLS.**
> Always create a unique scratch database and verify dump integrity first. Do not use blind `DROP DATABASE IF EXISTS`.

```powershell
$uniqueId = (Get-Date -Format "yyyyMMdd_HHmmss") + "_" + ([System.Guid]::NewGuid().ToString("N").Substring(0, 8))
$scratchDb = "sih26135_scratch_" + $uniqueId
$sessionCreatedScratch = $null

$latestBackup = (Get-ChildItem -Path "database\backups\*.sql" | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName

if (!$latestBackup) {
    throw "No backup dump found in database\backups to restore!"
}

try {
    # 1. Existence check: fail-closed if collision occurs
    $exists = & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -N -B -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = '$scratchDb';"
    if ([int]$exists -ne 0) {
        throw "Safety Violation: Database '$scratchDb' already exists! Aborting."
    }

    # 2. Non-destructive create
    & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -e "CREATE DATABASE $scratchDb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    $sessionCreatedScratch = $scratchDb
    Write-Host "Created unique scratch database: $sessionCreatedScratch" -ForegroundColor Cyan

    # 3. Restore dump into scratch database
    $mysqlExe = "$mysqlBin\mysql.exe"
    cmd.exe /c """$mysqlExe"" --defaults-extra-file=""$tempCnf"" $scratchDb < ""$latestBackup"""
    if ($LASTEXITCODE -ne 0) {
        throw "Scratch database restoration failed with exit code $LASTEXITCODE"
    }

} catch {
    Write-Error "Scratch restore failed: $_"
    throw
}
```

---

### Step 8: Verify Invariants in Scratch Catalog & Clean Up

Assert expected baseline invariants in the scratch database:

```powershell
try {
    & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" $scratchDb -e "
        SELECT count(*) AS total_tables FROM information_schema.tables WHERE table_schema='$scratchDb';
        SELECT trainee_id, first_name, last_name, registration_number FROM trainees WHERE trainee_id=1005143;
        SELECT count(*) AS dpdp_consents_count FROM trainee_consents WHERE trainee_id=1005143;
        SELECT user_id, username, email FROM users WHERE user_id=1;
    "

    Write-Host "Scratch verification passed. 168 tables, trainee 1005143, 5 consents, admin user present." -ForegroundColor Green

} finally {
    # Safe owned cleanup
    if ($sessionCreatedScratch -and ($sessionCreatedScratch -eq $scratchDb)) {
        & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -e "DROP DATABASE $sessionCreatedScratch;"
        Write-Host "Cleaned up owned scratch database: $sessionCreatedScratch" -ForegroundColor Gray
        $sessionCreatedScratch = $null
    }
}

Write-Host "Routine verification drill COMPLETE. Primary database SIH26135 was NOT modified." -ForegroundColor Green
```

> [!IMPORTANT]
> **STOP HERE FOR ROUTINE DISASTER RECOVERY DRILLS.**
> The backup dump is verified. Step 9 below is reserved solely for real disaster scenarios where `SIH26135` has suffered catastrophic corruption.

---

### Step 9: Exceptional Primary Database Restoration (Workflow B — Emergency Operator Procedure Only)

Execute this step **ONLY** if the primary database `SIH26135` has experienced verified data corruption:

1. **Pre-Recovery Snapshot**:
   ```powershell
   $preRecoveryFile = "E:\project\database\backups\pre_recovery_snapshot_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
   & "$mysqlBin\mysqldump.exe" --defaults-extra-file="$tempCnf" --single-transaction --quick --result-file="$preRecoveryFile" SIH26135
   Write-Host "Pre-recovery snapshot preserved at: $preRecoveryFile"
   ```
2. **Explicit Operator Authorization Gate**:
   ```powershell
   $confirmToken = Read-Host "DANGER: You are about to overwrite primary database SIH26135. Type 'RESTORE_SIH26135_CONFIRMED' to proceed"
   if ($confirmToken -ne "RESTORE_SIH26135_CONFIRMED") {
       Write-Host "Primary restoration aborted by operator. SIH26135 was NOT modified." -ForegroundColor Yellow
       exit 1
   }

   # Execute primary database overwrite
   $mysqlExe = "$mysqlBin\mysql.exe"
   cmd.exe /c """$mysqlExe"" --defaults-extra-file=""$tempCnf"" SIH26135 < ""$latestBackup"""
   if ($LASTEXITCODE -ne 0) {
       Write-Error "Primary restoration failed! Investigate or restore pre-recovery snapshot: $preRecoveryFile"
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

# 3. Test Authentication
$loginBody = '{"username":"admin","password":"password123"}'
$authResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $authResponse.data.accessToken

# 4. Verify Authoritative Trainee Endpoint
$trainee = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/trainees/1005143" -Method Get -Headers @{Authorization="Bearer $token"}
Write-Host "Verified Trainee: $($trainee.data.firstName) $($trainee.data.lastName)" -ForegroundColor Green

# 5. Clean up temporary option file
if (Test-Path $tempCnf) {
    Set-Content -Path $tempCnf -Value ""
    Remove-Item -Path $tempCnf -Force
}
```

System disaster recovery verification is complete.
