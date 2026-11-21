# SIH-26135 Local Disaster Recovery Runbook

**Document**: Local Disaster Recovery Operational Runbook  
**Target Environment**: Windows 11 / Host Workstation (Local-PC Execution)  
**Applicability**: Backend Engineers, Evaluators, Technical Leads  
**Last Verified**: November 2026 (Phase 29.14)

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

## 2. Master 10-Step Local Disaster Recovery Sequence

```
+---------------------------------------------------------------------------------------------------+
|                            MASTER LOCAL RECOVERY WORKFLOW                                         |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|  [Step 1] Stop Application  --->  [Step 2] Preserve Evidence  --->  [Step 3] Identify Failure     |
|      (Stop-Process)                     (Logs & Crash Dumps)              (DB, JVM, Files, Env)   |
|                                                                                   |               |
|  [Step 6] Restore Config   <---   [Step 5] Restart MySQL     <---   [Step 4] Verify Git Source    |
|      (Env & Properties)                 (Restart-Service)                 (git status / diff)     |
|         |                                                                                         |
|         v                                                                                         |
|  [Step 7] Scratch DB Restore  ->  [Step 8] Invariant Checks   ->  [Step 9] Restart Backend        |
|      (sih26135_scratch_verify)          (168 tables, trainee 1005143)    (mvn / java -jar)        |
|                                                                                   |               |
|                                                                                   v               |
|                                                                      [Step 10] Health & API Audit |
|                                                                          (Liveness, Readiness, me)|
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
- **Configuration Layer**: Are required environment variables (`JWT_SECRET`, `DB_PASSWORD`) set?
- **Staging / Storage Layer**: Is disk space exhausted?

---

### Step 4: Verify Project Source Integrity

Check the Git repository state to ensure source code is intact:

```powershell
# Verify working tree status
git status

# Verify zero whitespace defects or conflict markers
git diff --check

# If untracked garbage files accumulated, clean them cautiously:
# git clean -nd  (Dry run to preview)
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

### Step 6: Restore Application Configuration & Environment

Re-establish necessary environment variables in your active PowerShell session:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "SIH26135"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "YourSecureLocalPassword"

$env:JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
$env:COMMUNICATION_PROVIDER_MODE = "MOCK"
```

---

### Step 7: Restore Database into Isolated Scratch Catalog First

> [!CAUTION]
> Never restore directly into the primary application database (`SIH26135`). Always import into a scratch database first.

```powershell
$scratchDb = "sih26135_scratch_verify"
$latestBackup = (Get-ChildItem -Path "database\backups\*.sql" | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName

if (!$latestBackup) {
    Write-Warning "No backup dump found in database\backups. Using schema initialization scripts in database/..."
    # Re-run schema initialization scripts if no backup dump is available
} else {
    Write-Host "Restoring from: $latestBackup into $scratchDb"
    $mysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    
    $env:MYSQL_PWD = $env:DB_PASSWORD
    & $mysqlExe -u root -e "DROP DATABASE IF EXISTS $scratchDb; CREATE DATABASE $scratchDb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    cmd.exe /c "set ""MYSQL_PWD=$env:MYSQL_PWD"" && ""$mysqlExe"" -u root $scratchDb < ""$latestBackup"""
}
```

---

### Step 8: Verify Database Invariants in Scratch Catalog

Execute invariant assertion queries against the scratch database:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root $scratchDb -e "
    SELECT count(*) AS total_tables FROM information_schema.tables WHERE table_schema='$scratchDb';
    SELECT trainee_id, first_name, last_name, registration_number FROM trainees WHERE trainee_id=1005143;
    SELECT count(*) AS dpdp_consents_count FROM trainee_consents WHERE trainee_id=1005143;
    SELECT user_id, username, email FROM users WHERE user_id=1;
"
```

Confirm:
- `total_tables`: **168**
- Authoritative trainee `1005143`: Present (`Authorized Tester`)
- DPDP consents: **5** granted records
- Root admin user `1`: Present (`admin`)

Once confirmed, apply the dump to the primary database `SIH26135` and drop the scratch database:

```powershell
# Promote to primary database
cmd.exe /c "set ""MYSQL_PWD=$env:MYSQL_PWD"" && ""$mysqlExe"" -u root SIH26135 < ""$latestBackup"""
& $mysqlExe -u root -e "DROP DATABASE IF EXISTS $scratchDb;"
```

---

### Step 9: Rebuild & Restart Spring Boot Backend

Perform a clean build to eliminate any corrupted class files or build artifacts:

```powershell
# Clean and recompile
mvn -f backend/pom.xml clean compile

# Launch with local demo profile
mvn -f backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=local-demo"
```

---

### Step 10: Operational Health & API Verification

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
```

System recovery is complete. All application and database invariants are restored.
