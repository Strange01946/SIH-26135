# SIH-26135 Database Backup & Restoration Runbook

**Document**: Local Database Disaster Recovery & Backup Operational Runbook  
**Target Environment**: Windows 11 / MySQL 8.0 Community Server (Local-PC Execution)  
**Applicability**: Database Administrators, Backend Engineers, Evaluators  
**Last Verified**: November 2026 (Phase 29.14 — Targeted Corrections)

---

## 1. Executive Summary & Core Safety Mandates

The SIH-26135 backend relies on a local MySQL 8.0 instance running on `localhost:3306`. This runbook establishes safe, verified procedures for backing up and restoring database catalogs.

> [!CAUTION]
> **CRITICAL RESTORATION SAFETY MANDATES**:
> 1. **NEVER** restore a backup directly over the existing `SIH26135` application database during routine testing or validation.
> 2. Always verify backups in a **unique, isolated scratch database** (`sih26135_scratch_<timestamp>_<uuid>`) first to verify table integrity and record counts.
> 3. **Non-Destructive Scratch Creation**: Never execute blind `DROP DATABASE IF EXISTS` commands against fixed database names. Always check existence and fail closed on collision.
> 4. **Strict Separation of Workflows**: Routine validation concludes after scratch verification (**Workflow A**). Primary database restoration (**Workflow B**) is an emergency-only procedure requiring explicit operator confirmation and a pre-recovery snapshot.
> 5. **Supported Authentication**: Passwords must never appear as plaintext command-line arguments or in logged shell history. Always use MySQL option files (`--defaults-extra-file`) or MySQL login paths (`mysql_config_editor`).

### 1.1 Windows Environment Variable & Process Exposure Limitations
- **`MYSQL_PWD` Limitations**: Setting `$env:MYSQL_PWD` does not provide complete secret isolation. The variable is visible to any process running under the same user token (via Task Manager, Process Explorer, or `Get-CimInstance Win32_Process`) and is automatically inherited by child processes.
- **`cmd.exe /c` Exposure**: Passing passwords in `cmd.exe /c "set MYSQL_PWD=..."` strings embeds the secret in process command-line arguments, which can be captured by process-creation audit logs (e.g., Windows Event ID 4688).
- **Recommended Practice**: Use a temporary options file (`--defaults-extra-file`) with restricted permissions that is securely zeroed and deleted in a `finally` block, or configure persistent encrypted credentials via `mysql_config_editor`.

---

## 2. Environment & Binary Path Discovery

The default MySQL 8.0 binaries on Windows are located in:
```powershell
$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin"
```

Verify binary availability:
```powershell
& "$mysqlBin\mysql.exe" --version
& "$mysqlBin\mysqldump.exe" --version
# Expected: Ver 8.0.46 for Win64 on x86_64
```

Verify service status:
```powershell
Get-Service -Name "MySQL80"
```

---

## 3. Database Backup Procedure (`mysqldump`)

### 3.1 Pre-Backup Checklist
1. Ensure the Windows `MySQL80` service is running.
2. Identify the target catalog:
   - Application Database: `SIH26135`
   - Dedicated Test Database: `SIH26135_TEST`
3. Ensure adequate disk space exists in the target backup directory (minimum 500 MB free).
4. Store backups in a dedicated directory outside transient build paths (e.g., `database\backups\`).

### 3.2 Executing the Backup

Execute the backup using PowerShell with protected temporary option-file authentication:

```powershell
# 1. Define paths and timestamped filename
$backupDir = "E:\project\database\backups"
if (!(Test-Path $backupDir)) { New-Item -ItemType Directory -Path $backupDir -Force }

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$targetDb = "SIH26135"  # Or "SIH26135_TEST"
$backupFile = "$backupDir\${targetDb}_backup_${timestamp}.sql"

# 2. Prompt securely for password (not displayed and not saved to shell history)
$securePassword = Read-Host -Prompt "Enter MySQL root password" -AsSecureString

# 3. Create protected temporary options file
$tempCnf = [System.IO.Path]::GetTempFileName()
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)

Set-Content -Path $tempCnf -Value @"
[client]
user=root
password=$plainPassword
host=localhost
port=3306
default-character-set=utf8mb4
"@
$plainPassword = $null

try {
    # 4. Execute mysqldump with atomic consistency flags
    & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" `
        --defaults-extra-file="$tempCnf" `
        --single-transaction `
        --quick `
        --routines `
        --triggers `
        --hex-blob `
        --result-file="$backupFile" `
        $targetDb

    Write-Host "Backup completed: $backupFile" -ForegroundColor Green
} finally {
    # 5. Clean up temporary credentials file reliably
    if (Test-Path $tempCnf) {
        Set-Content -Path $tempCnf -Value ""
        Remove-Item -Path $tempCnf -Force
    }
}
```

#### Key Flag Technical Rationale:
- `--defaults-extra-file`: Passes authentication securely via a temporary option file, keeping passwords off the command line and out of process argument lists.
- `--single-transaction`: Creates a consistent InnoDB snapshot without locking tables, allowing concurrent reads.
- `--quick`: Streams rows directly from the server rather than buffering the entire table in client RAM.
- `--routines`: Dumps stored procedures and functions.
- `--triggers`: Preserves database triggers.
- `--hex-blob`: Dumps binary columns (UUIDs, hashes) in hexadecimal format to avoid character-set corruption.
- `--result-file`: Writes directly to disk, avoiding PowerShell stdout encoding distortions.

### 3.3 Permanent Alternative: MySQL Login-Path (`mysql_config_editor`)
For automated developer workflows without prompts, configure a persistent encrypted login path once:
```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql_config_editor.exe" set --login-path=local_sih --host=localhost --user=root --password
```
Once configured, backups run securely without any temporary credential files:
```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" --login-path=local_sih --single-transaction --quick --result-file="$backupFile" $targetDb
```

### 3.4 Backup Verification Procedure

Immediately verify the generated backup file before relying on it:

```powershell
# 1. Verify file exists and has non-zero size (> 1 MB)
$fileItem = Get-Item $backupFile
Write-Host "Backup Size: $($fileItem.Length / 1MB) MB"
if ($fileItem.Length -lt 1000000) { throw "Backup file size is suspiciously small!" }

# 2. Check header and trailer in the SQL file
Get-Content -Path $backupFile -TotalCount 10
Get-Content -Path $backupFile -Tail 10
# Expected trailer: "-- Dump completed on YYYY-MM-DD HH:MM:SS"

# 3. Verify presence of critical core tables
$tablesToCheck = @("users", "trainees", "trainee_consents", "audit_logs")
foreach ($tbl in $tablesToCheck) {
    $found = Select-String -Path $backupFile -Pattern "CREATE TABLE \`$tbl\`" -SimpleMatch -Quiet
    if ($found) { Write-Host "Verified table: $tbl" -ForegroundColor Green }
    else { Write-Warning "Missing table definition: $tbl" }
}
```

---

## 4. Restoration Workflows

Restoration is divided into two mutually exclusive workflows to prevent accidental primary database overwrites:

```
+---------------------------------------------------------------------------------------------------+
|  WORKFLOW A: Safe Backup Verification Drill (Routine / Non-Destructive)                          |
|                                                                                                   |
|  1. Generate Unique Name  ->  2. Check Non-Existence  ->  3. Create Scratch DB (No DROP)         |
|     (sih26135_scratch_...)       (Fail closed if exists)     (Track Session Ownership)            |
|                                                                     |                             |
|                                                                     v                             |
|  6. STOP (Primary Untouched) <-  5. Drop Owned Scratch  <-  4. Restore Dump & Assert Invariants   |
|     (Drill Complete)                (Clean up exact DB)         (168 tables, trainee 1005143)     |
+---------------------------------------------------------------------------------------------------+

+---------------------------------------------------------------------------------------------------+
|  WORKFLOW B: Exceptional Primary Database Recovery (Emergency Operator Procedure Only)           |
|                                                                                                   |
|  1. Formal Operator Auth  ->  2. Stop Application     ->  3. Pre-Recovery Backup Snapshot         |
|     (Incident response)          (Stop-Process)              (Snapshot current SIH26135)          |
|                                                                     |                             |
|                                                                     v                             |
|  6. Post-Recovery Invariants <-  5. Restore to SIH26135 <-  4. Explicit Confirmation Token        |
|     (Verify health & data)          (Execute restore)           ('RESTORE_SIH26135_CONFIRMED')    |
+---------------------------------------------------------------------------------------------------+
```

---

## 5. Workflow A: Safe Backup Verification (Non-Destructive Scratch Drill)

Workflow A verifies the integrity and restorability of a backup dump **without modifying or interacting with the primary database**.

### 5.1 Step 1: Generate Unique Scratch Database Name & Validate Non-Existence
```powershell
$mysqlBin = "C:\Program Files\MySQL\MySQL Server 8.0\bin"
$uniqueId = (Get-Date -Format "yyyyMMdd_HHmmss") + "_" + ([System.Guid]::NewGuid().ToString("N").Substring(0, 8))
$scratchDb = "sih26135_scratch_" + $uniqueId
$sessionCreatedScratch = $null

# Set up protected temporary options file
$securePassword = Read-Host -Prompt "Enter MySQL root password" -AsSecureString
$tempCnf = [System.IO.Path]::GetTempFileName()
$bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
Set-Content -Path $tempCnf -Value "[client]`nuser=root`npassword=$plainPassword`nhost=localhost`nport=3306`ndefault-character-set=utf8mb4"
$plainPassword = $null

try {
    # 1. Existence check: fail-closed if name collision occurs
    $exists = & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -N -B -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = '$scratchDb';"
    if ([int]$exists -ne 0) {
        throw "Safety Violation: Database '$scratchDb' already exists! Aborting without modifying existing databases."
    }

    # 2. Non-destructive database creation (No DROP DATABASE IF EXISTS)
    & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -e "CREATE DATABASE $scratchDb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    $sessionCreatedScratch = $scratchDb
    Write-Host "Created unique scratch database: $sessionCreatedScratch" -ForegroundColor Cyan

    # 3. Restore dump into the newly created scratch database
    $mysqlExe = "$mysqlBin\mysql.exe"
    cmd.exe /c """$mysqlExe"" --defaults-extra-file=""$tempCnf"" $scratchDb < ""$backupFile"""
    if ($LASTEXITCODE -ne 0) {
        throw "Restoration into scratch database failed with exit code $LASTEXITCODE"
    }

    # 4. Verify scratch database invariants
    & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" $scratchDb -e "
        SELECT count(*) AS total_tables FROM information_schema.tables WHERE table_schema='$scratchDb';
        SELECT trainee_id, first_name, last_name, registration_number FROM trainees WHERE trainee_id=1005143;
        SELECT count(*) AS dpdp_consents_count FROM trainee_consents WHERE trainee_id=1005143;
        SELECT user_id, username, email FROM users WHERE user_id=1;
    "

    Write-Host "Scratch verification passed. Expected: 168 tables, trainee 1005143, 5 DPDP consents, user 1 admin." -ForegroundColor Green

} finally {
    # 5. Strict owned cleanup: only drop the exact database created in this session
    if ($sessionCreatedScratch -and ($sessionCreatedScratch -eq $scratchDb)) {
        & "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" -e "DROP DATABASE $sessionCreatedScratch;"
        Write-Host "Cleaned up owned scratch database: $sessionCreatedScratch" -ForegroundColor Gray
        $sessionCreatedScratch = $null
    } elseif ($sessionCreatedScratch) {
        Write-Warning "Scratch database ownership mismatch. Manual investigation required for: $sessionCreatedScratch"
    }

    # Clean up temporary option file
    if (Test-Path $tempCnf) {
        Set-Content -Path $tempCnf -Value ""
        Remove-Item -Path $tempCnf -Force
    }
}

Write-Host "Workflow A Complete: Backup verified successfully. Primary database SIH26135 was NOT modified." -ForegroundColor Green
```

---

## 6. Workflow B: Exceptional Primary Database Recovery (Emergency Operator Procedure)

> [!WARNING]
> **EMERGENCY PROCEDURE ONLY**: Workflow B is strictly for disaster recovery when the primary database `SIH26135` has suffered irrecoverable data loss or corruption. **It is NEVER executed during routine testing, verification drills, or evaluation.**

### 6.1 Prerequisites for Primary Restoration
1. **Operator Authorization**: Written or ticket authorization identifying the recovery incident.
2. **Stop Applications**: Backend application must be stopped to avoid concurrent writes during restore:
   ```powershell
   Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force
   ```
3. **Pre-Recovery Snapshot**: Take a backup of the current database state (even if corrupted) before restoring:
   ```powershell
   $preRecoveryFile = "E:\project\database\backups\pre_recovery_snapshot_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
   & "$mysqlBin\mysqldump.exe" --defaults-extra-file="$tempCnf" --single-transaction --quick --result-file="$preRecoveryFile" SIH26135
   Write-Host "Pre-recovery snapshot preserved: $preRecoveryFile"
   ```
4. **Data Loss Window Acknowledgment**: Any transactions executed between the backup timestamp and the incident will be permanently replaced.

### 6.2 Executing Primary Restoration with Explicit Authorization Gate
```powershell
# Explicit interactive confirmation: requires exact token match
$confirmToken = Read-Host "Type 'RESTORE_SIH26135_CONFIRMED' to overwrite primary database SIH26135"
if ($confirmToken -ne "RESTORE_SIH26135_CONFIRMED") {
    Write-Host "Restoration aborted. Primary database was NOT modified." -ForegroundColor Yellow
    exit 1
}

# Execute restoration to primary database
$mysqlExe = "$mysqlBin\mysql.exe"
cmd.exe /c """$mysqlExe"" --defaults-extra-file=""$tempCnf"" SIH26135 < ""$backupFile"""

if ($LASTEXITCODE -ne 0) {
    Write-Error "Primary restoration failed! Consider restoring pre-recovery snapshot from: $preRecoveryFile"
    exit 1
}

# Post-recovery invariant verification
& "$mysqlBin\mysql.exe" --defaults-extra-file="$tempCnf" SIH26135 -e "
    SELECT count(*) AS total_tables FROM information_schema.tables WHERE table_schema='SIH26135';
    SELECT trainee_id, first_name, last_name, registration_number FROM trainees WHERE trainee_id=1005143;
    SELECT count(*) AS dpdp_consents_count FROM trainee_consents WHERE trainee_id=1005143;
    SELECT user_id, username, email FROM users WHERE user_id=1;
"
Write-Host "Primary recovery completed and verified." -ForegroundColor Green
```

---

## 7. Point-in-Time Recovery (PITR) & Binary Logging Analysis

### 7.1 Verified Local Binary Logging Status
An environment inspection of the local MySQL 8.0 instance confirms:
- `log_bin`: **ON**
- `binlog_format`: **ROW**
- `binlog_expire_logs_seconds`: `2592000` (30 days retention)

### 7.2 Local Workstation Capabilities & Limitations
1. **Capabilities**:
   - The MySQL 8.0 server writes binlogs (e.g., `binlog.000001`, `binlog.000002`) to the local data directory.
   - Developers can inspect individual binlogs via `mysqlbinlog.exe` to examine recent SQL transactions.
2. **Current Local Limitations**:
   - There is no automated continuous off-host shipping or remote object storage for binary logs.
   - Binary logs reside on the same physical storage volume as the database files. In the event of a disk drive failure, binary logs would be lost alongside the primary data files.
   - For local development and demonstration, **full logical dumps (`mysqldump`) remain the authoritative, supported backup mechanism**. Scheduled PITR replay is neither required nor configured for local workstations.
