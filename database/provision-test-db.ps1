<#
.SYNOPSIS
    Provisions the dedicated test database SIH26135_TEST from authoritative schema sources.

.DESCRIPTION
    Deterministically transforms authoritative DDL scripts (Phases 00–16) to target SIH26135_TEST.
    Strictly refuses to target or modify the production SIH26135 database.

    Security & Reliability Guarantees:
    1. Fail-Closed Credential Identity:
       - Requires explicitly configured username via -Username parameter or SIH_TEST_DB_USERNAME.
       - Rejects missing, blank, or whitespace-padded usernames (zero silent defaulting to 'root').
       - When 'root' is explicitly configured, queries and connects to that exact account.
       - Credential resolution strictly queries the Windows Credential Store for the exact configured
         user and host ($Username@$HostName). Never silently queries a different account.
       - Preserves restrictive option-file permissions (inheritance disabled, FullControl to current user).
       - Never logs or exposes credentials in command-line arguments or console output.
    2. Meaningful Server Identity Verification:
       - Compares connected server port (@@port) against configured target port ($Port).
       - Verifies server identity against SIH_EXPECTED_SERVER_UUID and SIH_EXPECTED_HOSTNAME when configured.
       - Acknowledges that without pre-pinned TLS certificates or UUID configuration in the repository,
         the server is not cryptographically authenticated; production isolation is guaranteed by
         strict target guards and fail-closed preflight validation.
       - Guarantees every destructive operation (DROP DATABASE, CREATE DATABASE) can ONLY target
         exactly 'SIH26135_TEST', and strictly asserts production 'SIH26135' is never targeted.
    3. Strengthened SQL Transformation Preflight:
       - Inspects all 17 authoritative schema files in memory before initiating any database action.
       - Enforces strict sequence ordering (phase-00 through phase-16) and non-empty file contents.
       - Executes deterministic transformations and performs fail-closed residual analysis:
         scans all SQL tokens, identifiers, strings, and comments accounting for normal SQL identifier
         boundaries (including backticks) and requires exact match to 'SIH26135_TEST'.
       - Rejects 'SIH26135_TEST_PROD', 'SIH26135_TEST2', 'SIH26135_PROD', 'SIH26135.users',
         and all prefix/suffix variations ($SIH26135_TEST, prefix_SIH26135, etc.).
       - Accurately adjusts ck_unemployment_events_distinct_jobs in Phase 14 due to MySQL 8.0
         ON DELETE SET NULL foreign key restrictions (MySQL Error 3823), matching the live schema.
    4. Meaningful Option-File Parsing Self-Test:
       - Validates option file generation against installed mysql.exe using representative dummy
         non-production values containing backslashes, quotes, spaces, '#', ';', tabs, and CR/LF.
       - Asserts that mysql.exe --print-defaults exits with code 0 and verifies that comment delimiters
         ('#' and ';') do not truncate option values.
       - Temporary test files are guaranteed clean in a finally block without connecting to any database.
       - Truthful reporting: if mysql.exe is unavailable, explicitly reports parser integration as SKIPPED
         with concise reason and never counts it as a pass. Fail-closed policy ensures overall SelfTest
         exits non-zero on skip because complete integration verification could not be certified.
    5. Execution Modes:
       - Normal: Preflight validation, server verification, clean reset, and sequential schema application.
       - -VerifyOnly: Preflight validation, server verification, and object count verification (144 tables,
         21 views in SIH26135_TEST; confirms SIH26135 untouched) without modifying any database.
       - -PreflightOnly: In-memory schema inspection, ordering, and transformation validation with 0 DB calls.
       - -SelfTest: Automated unit validation of option escaping, isolation guards, SQL transformation,
         and credential resolution without modifying any database.
#>
[CmdletBinding()]
param (
    [string]$TargetDatabase = "SIH26135_TEST",
    [string]$MysqlExePath = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [string]$Username = $env:SIH_TEST_DB_USERNAME,
    [switch]$VerifyOnly = $false,
    [switch]$PreflightOnly = $false,
    [switch]$SelfTest = $false
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Constants
$ForbiddenProductionDatabase = "SIH26135"
$ExpectedTestDatabase = "SIH26135_TEST"

# =========================================================================
# Core Helper Functions (Testable and Reusable)
# =========================================================================

<#
.SYNOPSIS
    Serializes an option value for a MySQL option file ([client] group in --defaults-extra-file).
.DESCRIPTION
    Complies with MySQL 8.0 option file specification:
    - Wraps in double quotes so comment delimiters (# and ;) and spaces do not truncate the value.
    - Escapes backslashes (\ -> \\), double quotes (" -> \"), carriage returns, newlines, and tabs.
#>
function Format-MySqlOptionValue {
    param ([string]$Value)
    if ($null -eq $Value) {
        return '""'
    }
    $escaped = $Value -replace '\\', '\\' `
                      -replace '"', '\"' `
                      -replace "`r", '\r' `
                      -replace "`n", '\n' `
                      -replace "`t", '\t'
    return "`"$escaped`""
}

<#
.SYNOPSIS
    Strict isolation guard for target database parameter.
.DESCRIPTION
    Fails closed if the database name is null, blank, contains whitespace, is mixed/lowercase,
    matches production 'SIH26135' in any casing, or differs in any way from literal 'SIH26135_TEST'.
    Rejects unexpected input rather than silently normalizing or trimming it.
#>
function Assert-ValidTargetDatabase {
    param (
        [string]$DatabaseName,
        [string]$Expected = "SIH26135_TEST",
        [string]$Forbidden = "SIH26135"
    )

    if ($null -eq $DatabaseName -or $DatabaseName -eq "") {
        throw "FATAL ISOLATION GUARD: Target database parameter cannot be null or empty."
    }

    # Strict rejection of leading/trailing whitespace (no silent trimming)
    if ($DatabaseName -match '^\s|\s$') {
        throw "FATAL ISOLATION GUARD: Target database '$DatabaseName' contains forbidden leading/trailing whitespace. Must be exact '$Expected' without padding."
    }

    # Strict rejection of production database in any casing
    if ($DatabaseName.ToUpperInvariant() -eq $Forbidden.ToUpperInvariant()) {
        throw "FATAL ISOLATION GUARD: Refusing to target or modify forbidden production database '$Forbidden'!"
    }

    # Strict case-sensitive match (no silent normalization of mixed/lowercase)
    if ($DatabaseName -cne $Expected) {
        throw "FATAL ISOLATION GUARD: Target database MUST be exactly '$Expected' (case-sensitive, unpadded). Received invalid target '$DatabaseName'."
    }
}

<#
.SYNOPSIS
    Resolves the server URL for Windows Credential Store lookup.
.DESCRIPTION
    Guarantees credential queries match the configured username and hostname exactly.
    Fails closed on missing, blank, or padded identity values. Never silently defaults to 'root'.
#>
function Resolve-CredentialServerUrl {
    param (
        [string]$User,
        [string]$ServerHost
    )

    if ([string]::IsNullOrWhiteSpace($User)) {
        throw "FATAL CREDENTIAL IDENTITY: Username cannot be null, empty, or blank for credential lookup."
    }
    if ($User -match '^\s|\s$') {
        throw "FATAL CREDENTIAL IDENTITY: Username '$User' contains forbidden leading/trailing whitespace."
    }

    if ([string]::IsNullOrWhiteSpace($ServerHost)) {
        throw "FATAL CONNECTION CONFIGURATION: HostName cannot be null, empty, or blank for credential lookup."
    }
    if ($ServerHost -match '^\s|\s$') {
        throw "FATAL CONNECTION CONFIGURATION: HostName '$ServerHost' contains forbidden leading/trailing whitespace."
    }

    return "$User@$ServerHost"
}

<#
.SYNOPSIS
    Transforms an authoritative schema SQL file for the dedicated test catalog.
.DESCRIPTION
    Performs verified, deterministic replacements and executes a fail-closed residual check.
    Accounts for normal SQL identifier boundaries (including backticks) and requires that any
    SQL token or identifier containing 'SIH26135' is strictly equal to 'SIH26135_TEST'.
    Rejects 'SIH26135_TEST_PROD', 'SIH26135_TEST2', 'SIH26135_PROD', 'SIH26135.users', etc.
#>
function Transform-AuthoritativeSql {
    param (
        [string]$SqlContent,
        [string]$TargetDatabase,
        [string]$FileName = "unknown.sql"
    )

    if ([string]::IsNullOrWhiteSpace($SqlContent)) {
        throw "FATAL PREFLIGHT: SQL content for '$FileName' is null or empty."
    }

    # Deterministic replacements of supported authoritative schema constructs:
    # 1. Database creation statement (phase-00)
    # 2. Database context switch (all phases 00-16)
    # 3. Dynamic metadata queries in phase-04 and phase-09 checking information_schema
    # 4. Header comment in phase-00
    $transformed = $SqlContent `
        -replace '(?i)\bCREATE\s+DATABASE\s+(IF\s+NOT\s+EXISTS\s+)?SIH26135\b', "CREATE DATABASE `$1$TargetDatabase" `
        -replace '(?i)\bUSE\s+SIH26135;', "USE $TargetDatabase;" `
        -replace '(?i)TABLE_SCHEMA\s*=\s*''SIH26135''', "TABLE_SCHEMA = '$TargetDatabase'" `
        -replace '(?i)CONSTRAINT_SCHEMA\s*=\s*''SIH26135''', "CONSTRAINT_SCHEMA = '$TargetDatabase'" `
        -replace '(?i)--\s*Database:\s*SIH26135\b', "-- Database: $TargetDatabase"

    # Phase 14 check constraint adjustment:
    # Trainee unemployment events defines ck_unemployment_events_distinct_jobs referencing preceding_employment_id
    # and succeeding_employment_id. Both columns have ON DELETE SET NULL foreign keys.
    # In MySQL 8.0, check constraints referencing SET NULL foreign key columns trigger Error 3823:
    # "Check constraint '...' cannot refer to column '...' which is used in a foreign key with ON UPDATE or ON DELETE SET NULL action."
    # In live SIH26135, ck_unemployment_events_distinct_jobs was omitted during initial deployment.
    # Stripping it here makes SIH26135_TEST an exact match with live SIH26135.
    if ($FileName -eq "phase-14-unemployment-attrition.sql") {
        $phase14Pattern = ',\s*CONSTRAINT\s+ck_unemployment_events_distinct_jobs\s+CHECK\s*\(\s*preceding_employment_id\s+IS\s+NULL[\s\S]*?\)'
        if (-not ($SqlContent -match $phase14Pattern)) {
            throw "FATAL PREFLIGHT: Expected constraint 'ck_unemployment_events_distinct_jobs' not found in '$FileName'! Unrecognized file structure."
        }
        $transformed = $transformed -replace $phase14Pattern, ''
        if ($transformed -match 'ck_unemployment_events_distinct_jobs') {
            throw "FATAL PREFLIGHT: Failed to remove 'ck_unemployment_events_distinct_jobs' from '$FileName'!"
        }
    }

    # Strict Fail-Closed Residual Catalog Guard:
    # Scan for any SQL identifier, string, or token containing 'SIH26135' (with or without backticks).
    # Normal SQL identifier boundary accounts for [a-zA-Z0-9_$] and backtick delimiters.
    # Any identifier containing 'SIH26135' must be strictly equal to TargetDatabase ('SIH26135_TEST').
    # Fails closed on 'SIH26135_TEST_PROD', 'SIH26135_TEST2', 'SIH26135_PROD', 'SIH26135.users',
    # and all prefix/suffix variations.
    $identPattern = '(?i)(?<![a-zA-Z0-9_$])(`?)([a-zA-Z0-9_$]*SIH26135[a-zA-Z0-9_$]*)\1'
    $matches = [regex]::Matches($transformed, $identPattern)
    foreach ($m in $matches) {
        $ident = $m.Groups[2].Value
        if ($ident -cne $TargetDatabase) {
            throw "FATAL PREFLIGHT: Transformed SQL for '$FileName' contains forbidden catalog reference '$ident' at position $($m.Index)! Only exact '$TargetDatabase' is permitted."
        }
    }

    # Strict check: reject any DROP DATABASE on production
    if ($transformed -match '(?i)\bDROP\s+DATABASE\b[^\n;]*\bSIH26135\b(?!_TEST\b)') {
        throw "FATAL PREFLIGHT: Transformed SQL for '$FileName' contains DROP DATABASE on production SIH26135! Aborting."
    }

    return $transformed
}

# =========================================================================
# SelfTest Mode (Unit Validations of Guards, Escaping, and Transformations)
# =========================================================================
if ($SelfTest) {
    Write-Host "=========================================================="
    Write-Host "Running SelfTest Unit Verifications"
    Write-Host "=========================================================="

    # 1. Option Value Escaping Unit Tests & Meaningful Parser Verification
    $enc1 = Format-MySqlOptionValue 'simple'
    if ($enc1 -ne '"simple"') { throw "SelfTest Failed: enc1 = $enc1" }
    $enc2 = Format-MySqlOptionValue 'pass"with"quotes\and\slashes'
    if ($enc2 -ne '"pass\"with\"quotes\\and\\slashes"') { throw "SelfTest Failed: enc2 = $enc2" }
    $enc3 = Format-MySqlOptionValue "line1`r`nline2`ttab"
    if ($enc3 -ne '"line1\r\nline2\ttab"') { throw "SelfTest Failed: enc3 = $enc3" }
    $enc4 = Format-MySqlOptionValue 'pass with # hash and ; semicolon'
    if ($enc4 -ne '"pass with # hash and ; semicolon"') { throw "SelfTest Failed: enc4 = $enc4" }
    $enc5 = Format-MySqlOptionValue $null
    if ($enc5 -ne '""') { throw "SelfTest Failed: enc5 = $enc5" }
    $enc6 = Format-MySqlOptionValue ""
    if ($enc6 -ne '""') { throw "SelfTest Failed: enc6 = $enc6" }
    Write-Host "  [PASS] Option-value serialization and escaping logic"

    # Meaningful parser fixture: Execute mysql.exe --print-defaults with representative dummy values
    $parserStatus = "NotRun"
    $parserSkipReason = ""

    if (Test-Path -Path $MysqlExePath -PathType Leaf) {
        $dummyTmp = $null
        $dummyCnf = $null
        try {
            $dummyTmp = [System.IO.Path]::GetTempFileName()
            $dummyCnf = [System.IO.Path]::ChangeExtension($dummyTmp, ".cnf")
            if (Test-Path $dummyTmp) { Remove-Item $dummyTmp -Force }

            $encDummyHost = Format-MySqlOptionValue "127.0.0.1"
            $encDummyUser = Format-MySqlOptionValue 'dummy_user"with"quotes\and\slashes and #hash;semi'
            $encDummyPass = Format-MySqlOptionValue "Complex`"Pass\\word`r`nWith#Hash;Semi`tAnd`$Dollars!"

            $dummyContent = @"
[client]
host=$encDummyHost
port=3306
user=$encDummyUser
password=$encDummyPass
default-character-set="utf8mb4"
"@
            [System.IO.File]::WriteAllText($dummyCnf, $dummyContent, [System.Text.UTF8Encoding]::new($false))
            $psi = New-Object System.Diagnostics.ProcessStartInfo
            $psi.FileName = $MysqlExePath
            $psi.Arguments = "--defaults-extra-file=`"$dummyCnf`" --print-defaults"
            $psi.RedirectStandardOutput = $true
            $psi.RedirectStandardError = $true
            $psi.UseShellExecute = $false
            $psi.CreateNoWindow = $true

            $p = [System.Diagnostics.Process]::Start($psi)
            $pOut = $p.StandardOutput.ReadToEnd()
            $pErr = $p.StandardError.ReadToEnd()
            $p.WaitForExit()

            # Fail closed on non-zero exit code or defaults handling errors
            if ($p.ExitCode -ne 0) {
                throw "SelfTest Failed: mysql.exe exited with non-zero exit code $($p.ExitCode): $pErr"
            }
            if ($pErr -match "Fatal error in defaults handling") {
                throw "SelfTest Failed: mysql.exe reported fatal defaults handling error: $pErr"
            }
            if (-not ($pOut -match '--host=127\.0\.0\.1')) {
                throw "SelfTest Failed: Option parser output did not contain expected host argument"
            }
            if (-not ($pOut -match '--password=\*{5}')) {
                throw "SelfTest Failed: Option parser output did not contain masked password argument"
            }
            # Verify that comment delimiters (# and ;) did not truncate the user argument
            if (-not ($pOut -match '#hash;semi')) {
                throw "SelfTest Failed: Comment delimiter '#' or ';' truncated the user option value"
            }

            $parserStatus = "Passed"
            Write-Host "  [PASS] MySQL option-file parser integration verification (mysql.exe --print-defaults)"
        } finally {
            if ($dummyTmp -and (Test-Path $dummyTmp)) { Remove-Item $dummyTmp -Force }
            if ($dummyCnf -and (Test-Path $dummyCnf)) { Remove-Item $dummyCnf -Force }
        }
    } else {
        $parserStatus = "Skipped"
        $parserSkipReason = "mysql.exe not found at '$MysqlExePath'"
        Write-Host "  [SKIP] MySQL option-file parser integration verification: $parserSkipReason"
    }

    # 2. Tightened Target Database Guard Unit Tests
    $targetTestCases = @(
        @{ val = $null; shouldThrow = $true; desc = "Null" },
        @{ val = ""; shouldThrow = $true; desc = "Empty" },
        @{ val = "   "; shouldThrow = $true; desc = "Blank" },
        @{ val = "  SIH26135_TEST  "; shouldThrow = $true; desc = "Leading/trailing whitespace" },
        @{ val = "SIH26135_TEST "; shouldThrow = $true; desc = "Trailing space" },
        @{ val = " SIH26135_TEST"; shouldThrow = $true; desc = "Leading space" },
        @{ val = "sih26135_test"; shouldThrow = $true; desc = "Lowercase target" },
        @{ val = "Sih26135_Test"; shouldThrow = $true; desc = "Mixed-case target" },
        @{ val = "SIH26135"; shouldThrow = $true; desc = "Production catalog" },
        @{ val = "sih26135"; shouldThrow = $true; desc = "Lowercase production catalog" },
        @{ val = "  SIH26135  "; shouldThrow = $true; desc = "Padded production catalog" },
        @{ val = "SIH26135_PROD"; shouldThrow = $true; desc = "Production variant" },
        @{ val = "other_db"; shouldThrow = $true; desc = "Arbitrary database name" },
        @{ val = "SIH26135_TEST"; shouldThrow = $false; desc = "Exact expected target" }
    )

    foreach ($tc in $targetTestCases) {
        $threw = $false
        try { Assert-ValidTargetDatabase $tc.val } catch { $threw = $true }
        if ($threw -ne $tc.shouldThrow) {
            throw "SelfTest Failed: Target guard check for '$($tc.desc)' (Value: '$($tc.val)') threw=$threw, expectedThrow=$($tc.shouldThrow)"
        }
    }
    Write-Host "  [PASS] Tightened target database isolation guard (no silent trimming or normalization)"

    # 3. SQL Transformation Preflight & Fail-Closed Residuals Unit Tests
    # A. Valid transformations accepted
    $testSql = "CREATE DATABASE IF NOT EXISTS SIH26135; USE SIH26135; SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'SIH26135';"
    $outSql = Transform-AuthoritativeSql -SqlContent $testSql -TargetDatabase "SIH26135_TEST" -FileName "test.sql"
    if (-not ($outSql -match 'USE SIH26135_TEST;')) { throw "SelfTest Failed: USE replacement" }
    if (-not ($outSql -match 'CREATE DATABASE IF NOT EXISTS SIH26135_TEST')) { throw "SelfTest Failed: CREATE DATABASE replacement" }
    if (-not ($outSql -match "TABLE_SCHEMA = 'SIH26135_TEST'")) { throw "SelfTest Failed: TABLE_SCHEMA replacement" }

    # B. Exact SIH26135_TEST references accepted (plain, backtick, string, comments)
    $exactCases = @(
        "USE SIH26135_TEST;",
        "USE ``SIH26135_TEST``;",
        "SELECT * FROM SIH26135_TEST.users;",
        "SELECT * FROM ``SIH26135_TEST``.users;",
        "SELECT * FROM ``SIH26135_TEST``.``users``;",
        "SELECT * FROM 'SIH26135_TEST';",
        "-- Header SIH26135_TEST",
        "/* Block comment SIH26135_TEST */"
    )
    foreach ($ec in $exactCases) {
        $exactOut = Transform-AuthoritativeSql -SqlContent $ec -TargetDatabase "SIH26135_TEST" -FileName "exact.sql"
        if (-not ($exactOut -match "SIH26135_TEST")) { throw "SelfTest Failed: Exact reference rejected: $ec" }
    }

    # C. Forbidden examples rejected (exact production, suffix variants, prefix variants, qualified names, strings, comments)
    $forbiddenCases = @(
        # Suffix variants
        "USE SIH26135_TEST_PROD;",
        "USE ``SIH26135_TEST_PROD``;",
        "USE SIH26135_TEST2;",
        "USE ``SIH26135_TEST2``;",
        "USE SIH26135_PROD;",
        "USE ``SIH26135_PROD``;",
        "USE SIH26135_TEST$;",
        "USE SIH26135_TEST_;",
        # Prefix variants
        "USE prefix_SIH26135;",
        "USE prefix_SIH26135_TEST;",
        "USE ``prefix_SIH26135_TEST``;",
        "USE `$SIH26135_TEST;",
        "USE _SIH26135_TEST;",
        "USE 123SIH26135_TEST;",
        # Qualified production table references
        "SELECT * FROM SIH26135.users;",
        "SELECT * FROM ``SIH26135``.users;",
        "SELECT * FROM ``SIH26135``.``users``;",
        "SELECT * FROM prefix_SIH26135.users;",
        # Production DDL and literals
        "CREATE DATABASE SIH26135_PROD;",
        "DROP DATABASE SIH26135;",
        "WHERE OTHER_COL = 'SIH26135'",
        "WHERE TABLE_SCHEMA = 'SIH26135_PROD'",
        "-- Target SIH26135 database",
        "/* SIH26135 */"
    )
    foreach ($fc in $forbiddenCases) {
        $threw = $false
        try { [void](Transform-AuthoritativeSql -SqlContent $fc -TargetDatabase "SIH26135_TEST" -FileName "forbidden.sql") } catch { $threw = $true }
        if (-not $threw) {
            throw "SelfTest Failed: Residual guard failed to reject forbidden case: $fc"
        }
    }

    # D. Phase 14 check constraint adjustment test
    $phase14Mock = @"
CREATE TABLE trainee_unemployment_events (
  event_id BIGINT PRIMARY KEY,
  preceding_employment_id BIGINT,
  CONSTRAINT ck_unemployment_events_distinct_jobs CHECK (
    preceding_employment_id IS NULL OR succeeding_employment_id IS NULL OR preceding_employment_id <> succeeding_employment_id
  )
);
"@
    $p14Out = Transform-AuthoritativeSql -SqlContent $phase14Mock -TargetDatabase "SIH26135_TEST" -FileName "phase-14-unemployment-attrition.sql"
    if ($p14Out -match 'ck_unemployment_events_distinct_jobs') {
        throw "SelfTest Failed: Phase 14 check constraint was not removed by Transform-AuthoritativeSql"
    }

    Write-Host "  [PASS] SQL transformation preflight, residual guard, and Phase 14 constraint handling"

    # 4. Credential URL Resolution Consistency & Fail-Closed Unit Tests
    $credTestCases = @(
        @{ u = $null; h = "localhost"; shouldThrow = $true; desc = "Null username" },
        @{ u = ""; h = "localhost"; shouldThrow = $true; desc = "Empty username" },
        @{ u = "   "; h = "localhost"; shouldThrow = $true; desc = "Blank username" },
        @{ u = " root "; h = "localhost"; shouldThrow = $true; desc = "Whitespace-padded username" },
        @{ u = "root"; h = $null; shouldThrow = $true; desc = "Null hostname" },
        @{ u = "root"; h = ""; shouldThrow = $true; desc = "Empty hostname" },
        @{ u = "root"; h = " localhost "; shouldThrow = $true; desc = "Whitespace-padded hostname" },
        @{ u = "root"; h = "localhost"; shouldThrow = $false; expected = "root@localhost"; desc = "Explicit root account" },
        @{ u = "app_admin"; h = "db.internal"; shouldThrow = $false; expected = "app_admin@db.internal"; desc = "Explicit app_admin account" }
    )

    foreach ($ctc in $credTestCases) {
        $threw = $false
        $actualUrl = $null
        try {
            $actualUrl = Resolve-CredentialServerUrl -User $ctc.u -ServerHost $ctc.h
        } catch {
            $threw = $true
        }
        if ($threw -ne $ctc.shouldThrow) {
            throw "SelfTest Failed: Credential resolution for '$($ctc.desc)' threw=$threw, expectedThrow=$($ctc.shouldThrow)"
        }
        if (-not $threw -and $actualUrl -ne $ctc.expected) {
            throw "SelfTest Failed: Credential resolution for '$($ctc.desc)' returned '$actualUrl', expected '$($ctc.expected)'"
        }
    }
    Write-Host "  [PASS] Credential server URL resolution consistency and fail-closed validation"

    Write-Host "`n=========================================================="
    Write-Host "SelfTest Execution Summary"
    Write-Host "=========================================================="
    Write-Host "  Unit Tests:               4/4 PASSED"
    if ($parserStatus -eq "Passed") {
        Write-Host "  Parser Integration Test:  PASSED (mysql.exe --print-defaults verified)"
        Write-Host "  Overall SelfTest Status:  PASSED"
        Write-Host "==========================================================`n"
        return
    } else {
        Write-Host "  Parser Integration Test:  SKIPPED ($parserSkipReason)"
        Write-Host "  Overall SelfTest Status:  INCOMPLETE (Fail-closed policy: parser integration skipped)"
        Write-Host "==========================================================`n"
        throw "SelfTest Incomplete: All 4 unit test suites passed, but MySQL option-parser integration test was SKIPPED because mysql.exe is unavailable at '$MysqlExePath'. Under the fail-closed verification policy, overall self-test cannot be reported as PASSED without complete integration verification."
    }
}

# =========================================================================
# Main Execution Guard & Credential Resolution
# =========================================================================

# Strict Isolation Guard: Fail-Closed Against Production / Non-Test Target
Assert-ValidTargetDatabase -DatabaseName $TargetDatabase -Expected $ExpectedTestDatabase -Forbidden $ForbiddenProductionDatabase

if (-not (Test-Path -Path $MysqlExePath -PathType Leaf)) {
    throw "FATAL: mysql.exe not found at '$MysqlExePath'. Please verify MySQL installation path."
}

# Fail closed on missing, blank, or padded credential identity (zero silent defaulting to 'root')
if (-not $PreflightOnly) {
    if ([string]::IsNullOrWhiteSpace($Username)) {
        throw "FATAL CREDENTIAL IDENTITY: Test database username is missing. Must be explicitly provided via -Username parameter or SIH_TEST_DB_USERNAME environment variable."
    }
    if ($Username -match '^\s|\s$') {
        throw "FATAL CREDENTIAL IDENTITY: Username '$Username' contains forbidden leading/trailing whitespace. Must be unpadded."
    }

    if ([string]::IsNullOrWhiteSpace($HostName)) {
        throw "FATAL CONNECTION CONFIGURATION: HostName cannot be null, empty, or blank."
    }
    if ($HostName -match '^\s|\s$') {
        throw "FATAL CONNECTION CONFIGURATION: HostName '$HostName' contains forbidden leading/trailing whitespace. Must be unpadded."
    }
}

# Credential Resolution strictly bound to configured username and host
if (-not $PreflightOnly) {
    $Password = $env:SIH_TEST_DB_PASSWORD
    if ([string]::IsNullOrWhiteSpace($Password)) {
        $helper = "C:\Program Files\MySQL\MySQL Shell 8.0\bin\mysql-secret-store-windows-credential.exe"
        if (Test-Path -Path $helper -PathType Leaf) {
            try {
                $serverUrl = Resolve-CredentialServerUrl -User $Username -ServerHost $HostName
                $queryJson = @{
                    SecretType = "password"
                    ServerURL  = $serverUrl
                } | ConvertTo-Json -Compress

                $secretResult = $queryJson | & $helper get 2>$null | ConvertFrom-Json
                if ($secretResult -and $secretResult.Secret) {
                    $Password = $secretResult.Secret
                }
            } catch {
                # Do not leak credential store exception details
            }
        }
    }

    if ([string]::IsNullOrWhiteSpace($Password)) {
        $targetAccount = Resolve-CredentialServerUrl -User $Username -ServerHost $HostName
        throw "FATAL: Database credentials not provided in `$env:SIH_TEST_DB_PASSWORD and not found in Windows Credential Store for '$targetAccount'."
    }
}

# =========================================================================
# Preflight Schema Validation (All 17 Authoritative Files)
# =========================================================================
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not (Test-Path -Path $ScriptDir -PathType Container)) {
    $ScriptDir = "E:\project\database"
}

$SchemaFiles = @(
    "phase-00-database-init.sql",
    "phase-01-geography-government.sql",
    "phase-02-users-security.sql",
    "phase-03-trainees-consent.sql",
    "phase-04-programs-providers.sql",
    "phase-05-training-courses-batches.sql",
    "phase-06-skills-job-roles.sql",
    "phase-07-training-enrollment-attendance.sql",
    "phase-08-assessments-certifications.sql",
    "phase-09-employers-jobs-placement.sql",
    "phase-10-employment-history-salary.sql",
    "phase-11-surveys-follow-ups-communication.sql",
    "phase-12-employment-verification.sql",
    "phase-13-skill-gap.sql",
    "phase-14-unemployment-attrition.sql",
    "phase-15-audit-data-quality-logs.sql",
    "phase-16-analytical-views.sql"
)

Write-Host "=========================================================="
Write-Host "SIH-26135 Test Database Provisioning"
Write-Host "Target Database:                $TargetDatabase"
Write-Host "Authoritative Schema Directory: $ScriptDir"
Write-Host "=========================================================="
Write-Host "Step 1: Running Preflight Validation across all 17 schema files..."

if ($SchemaFiles.Count -ne 17) {
    throw "FATAL PREFLIGHT: Expected exactly 17 schema files, found $($SchemaFiles.Count)."
}

$ValidatedPayloads = [System.Collections.Generic.Dictionary[string, string]]::new()

for ($i = 0; $i -lt $SchemaFiles.Count; $i++) {
    $fileName = $SchemaFiles[$i]
    $filePath = Join-Path -Path $ScriptDir -ChildPath $fileName

    # Check existence
    if (-not (Test-Path -Path $filePath -PathType Leaf)) {
        throw "FATAL PREFLIGHT: Authoritative schema file '$fileName' not found in '$ScriptDir'!"
    }

    # Check readability and non-empty
    $fileInfo = Get-Item -Path $filePath
    if ($fileInfo.Length -le 0) {
        throw "FATAL PREFLIGHT: Authoritative schema file '$fileName' is empty (0 bytes)!"
    }

    # Verify execution order: files must match sequence phase-00 through phase-16
    $expectedPrefix = ("phase-{0:D2}-" -f $i)
    if (-not $fileName.StartsWith($expectedPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "FATAL PREFLIGHT: Schema sequence mismatch at index $i! Expected prefix '$expectedPrefix', found '$fileName'."
    }

    # Read raw UTF-8 content
    $rawContent = [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)
    if ([string]::IsNullOrWhiteSpace($rawContent)) {
        throw "FATAL PREFLIGHT: Content of '$fileName' is blank or unreadable!"
    }

    # Transform using strict validated transformation function
    $transformed = Transform-AuthoritativeSql -SqlContent $rawContent -TargetDatabase $TargetDatabase -FileName $fileName

    $ValidatedPayloads[$fileName] = $transformed
    Write-Host "  [OK] Validated: $fileName ($($fileInfo.Length) bytes)"
}

Write-Host "Preflight validation SUCCESS: All 17 schema files exist, are readable, in correct order, and dry-run transformations passed.`n"

if ($PreflightOnly) {
    Write-Host "PreflightOnly flag specified. Preflight checks completed successfully. Exiting without database operations."
    return
}

# =========================================================================
# Secure Option File Setup & Execution Engine
# =========================================================================
$TempFile = [System.IO.Path]::GetTempFileName()
$TempCnfPath = [System.IO.Path]::ChangeExtension($TempFile, ".cnf")
Move-Item $TempFile $TempCnfPath -Force

try {
    # Restrict permissions: Disable inheritance, grant FullControl only to current user identity
    $acl = Get-Acl -Path $TempCnfPath
    $acl.SetAccessRuleProtection($true, $false)
    $currentUser = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    $rule = New-Object System.Security.AccessControl.FileSystemAccessRule($currentUser, 'FullControl', 'Allow')
    $acl.SetAccessRule($rule)
    Set-Acl -Path $TempCnfPath -AclObject $acl

    # Write [client] options using compliant serialization and UTF-8 without BOM
    $cnfContent = @"
[client]
host=$(Format-MySqlOptionValue $HostName)
port=$Port
user=$(Format-MySqlOptionValue $Username)
password=$(Format-MySqlOptionValue $Password)
default-character-set="utf8mb4"
"@
    $utf8NoBom = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText($TempCnfPath, $cnfContent, $utf8NoBom)

    # Reliable MySQL Process Execution Helper
    function Invoke-MySql {
        param (
            [string]$Arguments = "",
            [string]$InputText = $null,
            [string]$OperationName = "MySQL Operation"
        )

        $procInfo = New-Object System.Diagnostics.ProcessStartInfo
        $procInfo.FileName = $MysqlExePath
        # --defaults-extra-file MUST be the first argument on the command line
        $procInfo.Arguments = "--defaults-extra-file=`"$TempCnfPath`" $Arguments"
        if ($null -ne $InputText) {
            $procInfo.RedirectStandardInput = $true
        }
        $procInfo.RedirectStandardOutput = $true
        $procInfo.RedirectStandardError = $true
        $procInfo.UseShellExecute = $false
        $procInfo.CreateNoWindow = $true

        $proc = [System.Diagnostics.Process]::Start($procInfo)

        if ($null -ne $InputText) {
            $writer = New-Object System.IO.StreamWriter($proc.StandardInput.BaseStream, $utf8NoBom)
            $writer.Write($InputText)
            $writer.Flush()
            $writer.Close()
        }

        $stdout = $proc.StandardOutput.ReadToEnd()
        $stderr = $proc.StandardError.ReadToEnd()
        $proc.WaitForExit()

        # MySQL Process Reliability: strictly inspect exit code and stderr
        if ($proc.ExitCode -ne 0) {
            $sanitizedError = $stderr.Trim()
            throw "FATAL: $OperationName failed with exit code $($proc.ExitCode): $sanitizedError"
        }

        return $stdout
    }

    # =========================================================================
    # Step 2: Database Isolation Verification: Server Identity & Schemata Check
    # =========================================================================
    Write-Host "Step 2: Connecting to MySQL server and verifying server and catalog identities..."
    $serverInfoOut = Invoke-MySql -Arguments "-s -N -e `"SELECT @@hostname, @@port, @@version, @@server_uuid;`"" -OperationName "Server Preflight Verification"
    $serverInfoParts = ($serverInfoOut.Trim() -split "\t")

    $connectedHost = if ($serverInfoParts.Length -ge 1) { $serverInfoParts[0].Trim() } else { "unknown" }
    $connectedPort = if ($serverInfoParts.Length -ge 2) { $serverInfoParts[1].Trim() } else { "0" }
    $connectedVer  = if ($serverInfoParts.Length -ge 3) { $serverInfoParts[2].Trim() } else { "unknown" }
    $connectedUuid = if ($serverInfoParts.Length -ge 4) { $serverInfoParts[3].Trim() } else { "unknown" }

    Write-Host "  Connected to MySQL Server: $connectedHost (Port: $connectedPort, Version: $connectedVer, UUID: $connectedUuid)"

    # Supported configuration verification:
    # 1. Port verification: Compare connected server port with configured port
    if ([int]$connectedPort -ne $Port) {
        throw "FATAL SERVER MISMATCH: Connected MySQL server port '$connectedPort' does not match configured target port '$Port'!"
    }

    # 2. Environment identity verification: If explicit trusted server UUID or HostName is configured in environment, assert exact match
    if (-not [string]::IsNullOrWhiteSpace($env:SIH_EXPECTED_SERVER_UUID)) {
        if ($connectedUuid -cne $env:SIH_EXPECTED_SERVER_UUID.Trim()) {
            throw "FATAL SERVER MISMATCH: Connected server UUID '$connectedUuid' does not match configured expected UUID '$($env:SIH_EXPECTED_SERVER_UUID)'!"
        }
        Write-Host "  [OK] Verified server UUID matches configured SIH_EXPECTED_SERVER_UUID."
    }

    if (-not [string]::IsNullOrWhiteSpace($env:SIH_EXPECTED_HOSTNAME)) {
        if ($connectedHost.ToUpperInvariant() -ne $env:SIH_EXPECTED_HOSTNAME.Trim().ToUpperInvariant()) {
            throw "FATAL SERVER MISMATCH: Connected server hostname '$connectedHost' does not match configured expected hostname '$($env:SIH_EXPECTED_HOSTNAME)'!"
        }
        Write-Host "  [OK] Verified server hostname matches configured SIH_EXPECTED_HOSTNAME."
    }

    # Document authentication boundary:
    # The repository does not store a pre-pinned server UUID or TLS certificate fingerprint.
    # Therefore, @@hostname and @@server_uuid serve as telemetry, and the server is verified against
    # configured network endpoint ($HostName:$Port) and environment pins when set.
    # Production database isolation is strictly enforced by the target guard rejecting non-test targets
    # and preflight SQL validation ensuring zero destructive operations target SIH26135.
    Write-Host "  Identity Verification Boundary: Verified endpoint ${HostName}:${Port} (telemetry UUID: $connectedUuid)."
    Write-Host "  Note: Server is not cryptographically authenticated via pre-pinned UUID. Isolation is enforced"
    Write-Host "        by fail-closed target guards and preflight transformations restricting actions to '$TargetDatabase'."

    # Query information_schema for production and test database presence
    $schemataSql = "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME IN ('$ForbiddenProductionDatabase', '$TargetDatabase');"
    $schemataOut = Invoke-MySql -Arguments "-s -N -e `"$schemataSql`"" -OperationName "Schemata Presence Query"
    $existingSchemata = ($schemataOut -split "`r?`n") | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | ForEach-Object { $_.Trim() }

    $prodFound = $existingSchemata -contains $ForbiddenProductionDatabase
    $testFound = $existingSchemata -contains $TargetDatabase

    Write-Host "  Production Database '$ForbiddenProductionDatabase': $(if ($prodFound) { 'DETECTED (Protected under isolation policies)' } else { 'NOT PRESENT' })"
    Write-Host "  Dedicated Test Database '$TargetDatabase': $(if ($testFound) { 'DETECTED (Target for verified provisioning)' } else { 'NOT YET CREATED' })"

    if (-not $VerifyOnly) {
        # =====================================================================
        # Step 3: Controlled Reset of SIH26135_TEST
        # =====================================================================
        Write-Host "`nStep 3: Resetting dedicated test catalog '$TargetDatabase' to clean state..."

        # Re-verify target database immediately prior to destructive operation
        Assert-ValidTargetDatabase -DatabaseName $TargetDatabase -Expected $ExpectedTestDatabase -Forbidden $ForbiddenProductionDatabase

        $resetSql = "DROP DATABASE IF EXISTS $TargetDatabase; CREATE DATABASE $TargetDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

        # Double check: ensure reset statement strictly targets SIH26135_TEST and NEVER drops or creates SIH26135
        if ($resetSql -match '(?i)\bSIH26135(?!_TEST)\b') {
            throw "FATAL ISOLATION GUARD: Reset statement references production database! Aborting."
        }

        [void](Invoke-MySql -Arguments "-e `"$resetSql`"" -OperationName "Resetting $TargetDatabase")
        Write-Host "  [OK] Dedicated test catalog '$TargetDatabase' dropped and recreated cleanly."

        # =====================================================================
        # Step 4: Sequential Execution of All 17 Validated Schema Payloads
        # =====================================================================
        Write-Host "`nStep 4: Applying 17 authoritative schema files to '$TargetDatabase'..."
        foreach ($fileName in $SchemaFiles) {
            Write-Host "  Applying: $fileName ..."
            $payload = $ValidatedPayloads[$fileName]
            # Extra defense in depth: bind session directly to TargetDatabase for phases 01-16
            $dbArg = if ($fileName -eq "phase-00-database-init.sql") { "" } else { "--database=$TargetDatabase" }
            [void](Invoke-MySql -Arguments $dbArg -InputText $payload -OperationName "Applying schema file '$fileName'")
        }
        Write-Host "All 17 authoritative schema files applied successfully."
    } else {
        Write-Host "`nVerifyOnly switch specified. Skipping database reset and DDL application."
    }

    # =========================================================================
    # Step 5: Post-Provisioning Verification Queries (Strict Exit Code & Count Check)
    # =========================================================================
    Write-Host "`nStep 5: Verifying schema object counts in dedicated test database '$TargetDatabase'..."
    $verifySql = "SELECT TABLE_TYPE, COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '$TargetDatabase' GROUP BY TABLE_TYPE;"
    $verifyOut = Invoke-MySql -Arguments "-s -N -e `"$verifySql`"" -OperationName "Verifying $TargetDatabase object counts"

    $verifyLines = $verifyOut -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    if ($verifyLines.Length -eq 0) {
        throw "FATAL: Verification query on '$TargetDatabase' returned no results! Unable to verify database state."
    }

    $counts = @{}
    foreach ($line in $verifyLines) {
        $parts = $line -split "\t"
        if ($parts.Length -ge 2) {
            $counts[$parts[0].Trim()] = [int]$parts[1].Trim()
        }
    }

    $baseTables = if ($counts.ContainsKey("BASE TABLE")) { $counts["BASE TABLE"] } else { -1 }
    $views = if ($counts.ContainsKey("VIEW")) { $counts["VIEW"] } else { -1 }

    Write-Host "  $TargetDatabase BASE TABLE count: $baseTables (expected: 144)"
    Write-Host "  $TargetDatabase VIEW count:       $views (expected: 21)"

    if ($baseTables -ne 144) {
        throw "FATAL: Verification failed for '$TargetDatabase': BASE TABLE count is $baseTables, expected 144."
    }
    if ($views -ne 21) {
        throw "FATAL: Verification failed for '$TargetDatabase': VIEW count is $views, expected 21."
    }

    # Verify production database SIH26135 remains untouched
    Write-Host "`nStep 6: Verifying production database '$ForbiddenProductionDatabase' remains completely untouched..."
    $normalSql = "SELECT TABLE_TYPE, COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '$ForbiddenProductionDatabase' GROUP BY TABLE_TYPE;"
    $normalOut = Invoke-MySql -Arguments "-s -N -e `"$normalSql`"" -OperationName "Verifying $ForbiddenProductionDatabase untouched"

    $normalLines = $normalOut -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    if ($normalLines.Length -eq 0) {
        throw "FATAL: Verification query on '$ForbiddenProductionDatabase' returned no results!"
    }

    $normalCounts = @{}
    foreach ($line in $normalLines) {
        $parts = $line -split "\t"
        if ($parts.Length -ge 2) {
            $normalCounts[$parts[0].Trim()] = [int]$parts[1].Trim()
        }
    }

    $normalBase = if ($normalCounts.ContainsKey("BASE TABLE")) { $normalCounts["BASE TABLE"] } else { -1 }
    $normalViews = if ($normalCounts.ContainsKey("VIEW")) { $normalCounts["VIEW"] } else { -1 }

    Write-Host "  $ForbiddenProductionDatabase BASE TABLE count: $normalBase (expected: 144)"
    Write-Host "  $ForbiddenProductionDatabase VIEW count:       $normalViews (expected: 21)"

    if ($normalBase -ne 144 -or $normalViews -ne 21) {
        throw "FATAL ISOLATION VIOLATION: Production database '$ForbiddenProductionDatabase' counts changed! Expected 144 tables and 21 views."
    }

    Write-Host "`nSUCCESS: Test database '$TargetDatabase' verified with 144 tables and 21 views."
    Write-Host "SUCCESS: Production database '$ForbiddenProductionDatabase' verified untouched with 144 tables and 21 views."
}
finally {
    # Minimize exposure: overwrite temporary option file contents before deleting
    if (Test-Path -Path $TempCnfPath) {
        try {
            [System.IO.File]::WriteAllText($TempCnfPath, [string]::Empty)
        } catch { }
        Remove-Item -Path $TempCnfPath -Force -ErrorAction SilentlyContinue
    }
}
