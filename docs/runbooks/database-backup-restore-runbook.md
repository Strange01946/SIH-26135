# SIH-26135 Database Backup & Restoration Runbook

**Document**: Local Database Disaster Recovery & Backup Operational Runbook  
**Target Environment**: Windows 11 / MySQL 8.0 Community Server (Local-PC Execution)  
**Applicability**: Database Administrators, Backend Engineers, Evaluators  
**Last Verified**: November 2026 (Phase 29.14)

---

## 1. Executive Summary & Core Safety Mandates

The SIH-26135 backend relies on a local MySQL 8.0 instance running on `localhost:3306`. This runbook establishes safe, verified procedures for backing up and restoring database catalogs.

> [!CAUTION]
> **CRITICAL RESTORATION SAFETY MANDATE**:
> 1. **NEVER** restore a backup directly over the existing `SIH26135` application database during routine testing or validation.
> 2. Always restore into an **isolated scratch database** (e.g., `sih26135_scratch_verify`) first to verify table integrity and record counts.
> 3. Any destructive drop, overwrite, or truncate on the primary database `SIH26135` requires deliberate, explicit human confirmation.
> 4. **Zero Passwords on CLI**: Passwords must never appear as plaintext command-line arguments or in logged shell history. Always use `$env:MYSQL_PWD` or MySQL option files.

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

Execute the backup using PowerShell with secure environment-backed authentication:

```powershell
# 1. Define paths and timestamped filename
$backupDir = "E:\project\database\backups"
if (!(Test-Path $backupDir)) { New-Item -ItemType Directory -Path $backupDir -Force }

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$targetDb = "SIH26135"  # Or "SIH26135_TEST"
$backupFile = "$backupDir\${targetDb}_backup_${timestamp}.sql"

# 2. Set credentials securely in the environment (never passed on the CLI)
$env:MYSQL_PWD = Read-Host -Prompt "Enter MySQL root password" -AsSecureString
# For automated local scripting, convert secure string or set from session environment:
# $env:MYSQL_PWD = $env:DB_PASSWORD

# 3. Execute mysqldump with atomic consistency flags
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" `
    -u root `
    -h localhost `
    -P 3306 `
    --single-transaction `
    --quick `
    --routines `
    --triggers `
    --hex-blob `
    --default-character-set=utf8mb4 `
    --result-file="$backupFile" `
    $targetDb

# 4. Clear credentials from environment
$env:MYSQL_PWD = $null
```

#### Key Flag Technical Rationale:
- `--single-transaction`: Creates a consistent InnoDB snapshot without locking tables, allowing concurrent reads.
- `--quick`: Streams rows directly from the server rather than buffering the entire table in client RAM.
- `--routines`: Dumps stored procedures and functions.
- `--triggers`: Preserves database triggers.
- `--hex-blob`: Dumps binary columns (UUIDs, hashes) in hexadecimal format to avoid character-set corruption.
- `--result-file`: Writes directly to disk, avoiding PowerShell stdout encoding distortions.

### 3.3 Backup Verification Procedure

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

## 4. Safe Database Restoration Procedure

To prevent accidental data loss or corruption, restoration follows a strict **Scratch-First Validation Protocol**.

```
+-----------------------------------------------------------------------------------+
|                        SCRATCH-FIRST RESTORATION FLOW                             |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  1. SQL Dump File  --->  2. Restore into Isolated Scratch DB                      |
|     (backup.sql)            (sih26135_scratch_verify)                             |
|                                     |                                             |
|                                     v                                             |
|                          3. Automated Invariant Checks:                           |
|                             • 168 Tables Present                                  |
|                             • Trainee 1005143 Exists                              |
|                             • 5 DPDP Consents Active                              |
|                             • Root Admin (User 1) Exists                          |
|                                     |                                             |
|                                     v                                             |
|                          4. Checks PASSED?                                        |
|                             [YES] ---> Deliberate Human Confirmation Form         |
|                             [NO]  ---> ABORT: Corrupt Backup File                 |
|                                     |                                             |
|                                     v                                             |
|                          5. Restore to Primary DB (SIH26135)                      |
|                                     |                                             |
|                                     v                                             |
|                          6. Drop Scratch DB (sih26135_scratch_verify)             |
+-----------------------------------------------------------------------------------+
```

### 4.1 Step 1: Create Scratch Database

```powershell
$scratchDb = "sih26135_scratch_verify"
$env:MYSQL_PWD = "YourSecureLocalPassword"

& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -e "
    DROP DATABASE IF EXISTS $scratchDb;
    CREATE DATABASE $scratchDb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
"
```

### 4.2 Step 2: Restore Backup into Scratch Database

Execute the import using `cmd.exe /c` redirection to ensure maximum throughput and prevent PowerShell pipe character-encoding conversion:

```powershell
$mysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
cmd.exe /c "set ""MYSQL_PWD=$env:MYSQL_PWD"" && ""$mysqlExe"" -u root $scratchDb < ""$backupFile"""
```

### 4.3 Step 3: Verify Scratch Database Invariants

Execute automated SQL integrity queries against `sih26135_scratch_verify`:

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root $scratchDb -e "
    SELECT count(*) AS total_tables FROM information_schema.tables WHERE table_schema='$scratchDb';
    SELECT trainee_id, first_name, last_name, registration_number FROM trainees WHERE trainee_id=1005143;
    SELECT count(*) AS dpdp_consents_count FROM trainee_consents WHERE trainee_id=1005143;
    SELECT user_id, username, email FROM users WHERE user_id=1;
"
```

#### Expected Invariant Verification Outputs:
1. `total_tables`: Exactly **168** tables.
2. `trainee_id 1005143`: First Name `Authorized`, Last Name `Tester`, Reg Number `REG-LIVE-VERIFY-001`.
3. `dpdp_consents_count`: Exactly **5** active consent records.
4. `user_id 1`: Username `admin`, Email `admin@test.gov.in`.

If any of these assertions fail, **ABORT IMMEDIATELY**. The backup is corrupted or incomplete.

### 4.4 Step 4: Promote to Primary Database (Human Confirmation Required)

Only after the scratch database passes all invariant checks may the primary application database be updated:

```powershell
# Mandatory Human Confirmation
$confirm = Read-Host "Scratch verification passed. Type 'OVERWRITE_PRIMARY' to restore into primary database"
if ($confirm -ne "OVERWRITE_PRIMARY") {
    Write-Host "Restoration aborted by operator." -ForegroundColor Yellow
    & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -e "DROP DATABASE IF EXISTS $scratchDb;"
    exit
}

# Execute Primary Restoration
$primaryDb = "SIH26135"
cmd.exe /c "set ""MYSQL_PWD=$env:MYSQL_PWD"" && ""$mysqlExe"" -u root $primaryDb < ""$backupFile"""

# Clean up Scratch Database
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -e "DROP DATABASE IF EXISTS $scratchDb;"
$env:MYSQL_PWD = $null
Write-Host "Database restoration completed successfully." -ForegroundColor Green
```

---

## 5. Point-in-Time Recovery (PITR) & Binary Logging Analysis

### 5.1 Verified Local Binary Logging Status
An environment inspection of the local MySQL 8.0 instance confirms:
- `log_bin`: **ON**
- `binlog_format`: **ROW**
- `binlog_expire_logs_seconds`: `2592000` (30 days retention)

### 5.2 Local Workstation Capabilities & Limitations
1. **Capabilities**:
   - The MySQL 8.0 server writes binlogs (e.g., `binlog.000001`, `binlog.000002`) to the local data directory.
   - Developers can inspect individual binlogs via `mysqlbinlog.exe` to examine recent SQL transactions.
2. **Current Local Limitations**:
   - There is no automated continuous off-host shipping or remote object storage for binary logs.
   - Binary logs reside on the same physical storage volume as the database files. In the event of a disk drive failure, binary logs would be lost alongside the primary data files.
   - For local development and demonstration, **full logical dumps (`mysqldump`) remain the authoritative, supported backup mechanism**. Scheduled PITR replay is neither required nor configured for local workstations.
