<#
.SYNOPSIS
    Safe developer test runner for SIH-26135 Spring Boot backend.

.DESCRIPTION
    Runs backend Maven test suites with dedicated test-database credentials
    bound strictly to 'SIH26135_TEST'.

    Guarantees:
    1. Isolation Safety:
       - Strictly targets 'SIH26135_TEST'. Never targets or falls back to production 'SIH26135'.
       - Never derives catalog name from user input; catalog is fixed to 'SIH26135_TEST'.
       - Constructs JDBC URL preserving project configuration: useUnicode=true,
         characterEncoding=utf8, connectionTimeZone=Asia/Kolkata.
       - Validates hostname against URL delimiter / query fragment injection before URL construction.
       - Validates port within range 1..65535.
       - Validates target database and JDBC URL before Maven execution.
       - Never modifies, migrates, drops, or alters database schemas.
    2. Ephemeral Credential Containment:
       - Resolves password from $env:SIH_TEST_DB_PASSWORD or the Windows Credential Store
         helper for Username@HostName.
       - Never falls back to production DB_* credentials.
       - Sets SIH_TEST_DB_URL, SIH_TEST_DB_USERNAME, and SIH_TEST_DB_PASSWORD strictly in the
         ephemeral process environment.
       - Guarantees complete cleanup of all three variables in a finally block.
       - Never writes secrets to files, logs, or persistent settings; never prints credentials.
    3. Safe Optional Pre-Verification:
       - -PreVerify switch invokes database/provision-test-db.ps1 strictly with -VerifyOnly.
       - Never invokes normal/destructive provisioning mode under any circumstances.
       - Detects verification failures via PowerShell terminating exception handling without
         relying on stale $LASTEXITCODE values.
       - Aborts Maven execution immediately if verification fails.
    4. Maven Integration & Exit Code Integrity:
       - Executes 'mvn test' from the backend directory with optional -Dtest=<value> filter.
       - Handles argument quoting cleanly without splitting on commas.
       - Preserves and returns Maven's exact exit code.

.PARAMETER Test
    Optional Maven Surefire test filter (e.g. -Test "DatabaseIsolationGuardTest"
    or -Test "DatabaseIsolationGuardTest,MysqlConnectionTest"). Passed as -Dtest=<value>.

.PARAMETER Username
    Database user for test catalog. Defaults to $env:SIH_TEST_DB_USERNAME if set;
    otherwise defaults to 'root'. Rejects whitespace padding and URL delimiters.

.PARAMETER HostName
    Database host for test catalog. Defaults to 'localhost'. Validated to reject
    URL delimiters, path separators, port fragments, query parameters, or whitespace.

.PARAMETER Port
    Database port for test catalog. Defaults to 3306. Validated within range 1..65535.

.PARAMETER PreVerify
    Optional switch. Runs read-only schema verification via provision-test-db.ps1 -VerifyOnly
    prior to executing Maven tests.
#>

[CmdletBinding()]
param (
    [Parameter(Mandatory = $false)]
    [string]$Test,

    [Parameter(Mandatory = $false)]
    [string]$Username,

    [Parameter(Mandatory = $false)]
    [string]$HostName = "localhost",

    [Parameter(Mandatory = $false)]
    [int]$Port = 3306,

    [Parameter(Mandatory = $false)]
    [switch]$PreVerify = $false
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Target catalog constants (immutable; never derived from user input)
$TargetDatabase = "SIH26135_TEST"
$ForbiddenProductionDatabase = "SIH26135"

# Resolve default username if not explicitly provided
if ([string]::IsNullOrWhiteSpace($Username)) {
    if (-not [string]::IsNullOrWhiteSpace($env:SIH_TEST_DB_USERNAME)) {
        $Username = $env:SIH_TEST_DB_USERNAME
    } else {
        $Username = "root"
    }
}

# Parameter validation: Username
if ([string]::IsNullOrWhiteSpace($Username)) {
    throw "FATAL CREDENTIAL IDENTITY: Username cannot be null, empty, or blank."
}
if ($Username -match '^\s|\s$') {
    throw "FATAL CREDENTIAL IDENTITY: Username '$Username' contains forbidden leading or trailing whitespace."
}
if ($Username -match '[/?#@:;&=\\\[\]]|\s') {
    throw "FATAL CREDENTIAL IDENTITY: Username '$Username' contains forbidden URL delimiters, query characters, or whitespace."
}

# Parameter validation: HostName
# Reject malformed host values before constructing JDBC URL; do not permit URL delimiters or JDBC query fragments.
if ([string]::IsNullOrWhiteSpace($HostName)) {
    throw "FATAL CONNECTION CONFIGURATION: HostName cannot be null, empty, or blank."
}
if ($HostName -match '^\s|\s$') {
    throw "FATAL CONNECTION CONFIGURATION: HostName '$HostName' contains forbidden leading or trailing whitespace."
}
if ($HostName -match '[/?#@:;&=\\\[\]]|[\x00-\x1F\x7F]|\s') {
    throw "FATAL CONNECTION CONFIGURATION: HostName '$HostName' contains forbidden characters, URL delimiters, path separators, or query fragments. Must be a valid hostname or IP address without URL syntax."
}
if ($HostName -notmatch '^[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)*$') {
    throw "FATAL CONNECTION CONFIGURATION: HostName '$HostName' is not a valid hostname or IP address format."
}

# Parameter validation: Port
if ($Port -lt 1 -or $Port -gt 65535) {
    throw "FATAL CONNECTION CONFIGURATION: Port '$Port' is invalid. Port must be within range 1..65535."
}

# Strict Database Isolation Guard: catalog must be exactly SIH26135_TEST
if ($TargetDatabase.ToUpperInvariant() -eq $ForbiddenProductionDatabase.ToUpperInvariant()) {
    throw "FATAL ISOLATION GUARD: Refusing to target forbidden production database '$ForbiddenProductionDatabase'!"
}
if ($TargetDatabase -cne "SIH26135_TEST") {
    throw "FATAL ISOLATION GUARD: Target database MUST be exactly 'SIH26135_TEST'."
}

# Construct and validate JDBC URL aligned with project test configuration:
# Preserves useUnicode=true, characterEncoding=utf8, and connectionTimeZone=Asia/Kolkata
# (matching spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Kolkata).
$jdbcUrl = "jdbc:mysql://${HostName}:${Port}/${TargetDatabase}?useUnicode=true&characterEncoding=utf8&connectionTimeZone=Asia/Kolkata"

# Preflight URL validation
if (-not ($jdbcUrl -match '^jdbc:mysql://[a-zA-Z0-9\.\-]+:\d+/SIH26135_TEST\?useUnicode=true&characterEncoding=utf8&connectionTimeZone=Asia/Kolkata$')) {
    throw "FATAL ISOLATION GUARD: JDBC URL format does not match expected pattern targeting 'SIH26135_TEST'."
}
if ($jdbcUrl -match '(?i)\bSIH26135\b(?!_TEST\b)') {
    throw "FATAL ISOLATION GUARD: JDBC URL contains forbidden production catalog reference '$ForbiddenProductionDatabase'!"
}

# Resolve backend directory location
$backendDir = if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) {
    $PSScriptRoot
} else {
    Split-Path -Parent $MyInvocation.MyCommand.Path
}

if (-not (Test-Path -Path (Join-Path -Path $backendDir -ChildPath "pom.xml") -PathType Leaf)) {
    throw "FATAL EXECUTION CONTEXT: 'pom.xml' not found in '$backendDir'. Must be run from or located within backend directory."
}

# Credential Resolution: fail-closed, no production DB_* fallback, zero console exposure
$resolvedPassword = $env:SIH_TEST_DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($resolvedPassword)) {
    $helperPath = "C:\Program Files\MySQL\MySQL Shell 8.0\bin\mysql-secret-store-windows-credential.exe"
    if (-not (Test-Path -Path $helperPath -PathType Leaf)) {
        $cmd = Get-Command "mysql-secret-store-windows-credential.exe" -ErrorAction SilentlyContinue
        if ($cmd) {
            $helperPath = $cmd.Source
        }
    }

    if (-not (Test-Path -Path $helperPath -PathType Leaf)) {
        throw "FATAL CREDENTIAL RESOLUTION: Password was not provided in `$env:SIH_TEST_DB_PASSWORD and Windows Credential Store helper was not found at '$helperPath'."
    }

    $serverUrl = "$Username@$HostName"
    try {
        $queryJson = @{
            SecretType = "password"
            ServerURL  = $serverUrl
        } | ConvertTo-Json -Compress

        # Suppress stderr to avoid leaking exception traces or credentials
        $secretRaw = $queryJson | & $helperPath get 2>$null
        if (-not [string]::IsNullOrWhiteSpace($secretRaw)) {
            $secretObj = $secretRaw | ConvertFrom-Json
            if ($secretObj -and $secretObj.Secret) {
                $resolvedPassword = [string]$secretObj.Secret
            }
        }
    } catch {
        # Fail closed without exposing credential details or secret-bearing exception traces
    }
}

if ([string]::IsNullOrWhiteSpace($resolvedPassword)) {
    throw "FATAL CREDENTIAL RESOLUTION: Database password was not provided in `$env:SIH_TEST_DB_PASSWORD and could not be retrieved from Windows Credential Store for '$Username@$HostName'."
}

# Execution block with guaranteed environment cleanup
$mavenExitCode = 1
Push-Location -Path $backendDir
try {
    # Set ephemeral environment variables for current process
    $env:SIH_TEST_DB_URL = $jdbcUrl
    $env:SIH_TEST_DB_USERNAME = $Username
    $env:SIH_TEST_DB_PASSWORD = $resolvedPassword

    # Optional read-only pre-verification
    if ($PreVerify) {
        $provisionerPath = Join-Path -Path $backendDir -ChildPath "..\database\provision-test-db.ps1"
        $resolvedProvisioner = [System.IO.Path]::GetFullPath($provisionerPath)
        if (-not (Test-Path -Path $resolvedProvisioner -PathType Leaf)) {
            throw "FATAL PREVERIFY: Provisioner script not found at '$resolvedProvisioner'."
        }

        Write-Host "Executing read-only pre-verification via provision-test-db.ps1 -VerifyOnly..."

        # Reliable failure detection: provision-test-db.ps1 throws PowerShell terminating errors on failure.
        # Track completion with boolean flag; never rely on stale $LASTEXITCODE.
        $preVerifySucceeded = $false
        try {
            # STRICT GUARD: Invoke only with -VerifyOnly to guarantee read-only verification.
            # Never invoke without -VerifyOnly (normal mode triggers destructive database reset).
            & $resolvedProvisioner -VerifyOnly -TargetDatabase "SIH26135_TEST" -Username $Username -HostName $HostName -Port $Port
            $preVerifySucceeded = $true
        } catch {
            throw "FATAL PREVERIFY: Test database pre-verification failed: $($_.Exception.Message)"
        }

        if (-not $preVerifySucceeded) {
            throw "FATAL PREVERIFY: Test database pre-verification did not complete successfully. Aborting test execution."
        }

        Write-Host "Test database pre-verification passed.`n"
    }

    # Execute Maven tests with robust argument handling
    if (-not [string]::IsNullOrWhiteSpace($Test)) {
        Write-Host "Running Maven tests with filter: -Dtest=$Test"
        mvn test "-Dtest=$Test"
        $mavenExitCode = $LASTEXITCODE
    } else {
        Write-Host "Running full Maven test suite..."
        mvn test
        $mavenExitCode = $LASTEXITCODE
    }

    if ($mavenExitCode -eq 0) {
        Write-Host "`n[SUCCESS] Maven test execution completed successfully (Exit Code 0)."
    } else {
        Write-Warning "`n[FAILURE] Maven test execution failed with exit code $mavenExitCode."
    }
}
finally {
    # Guaranteed cleanup: Remove ephemeral test database variables from process environment
    Remove-Item -Path env:SIH_TEST_DB_URL -ErrorAction SilentlyContinue
    Remove-Item -Path env:SIH_TEST_DB_USERNAME -ErrorAction SilentlyContinue
    Remove-Item -Path env:SIH_TEST_DB_PASSWORD -ErrorAction SilentlyContinue
    [System.Environment]::SetEnvironmentVariable("SIH_TEST_DB_URL", $null, [System.EnvironmentVariableTarget]::Process)
    [System.Environment]::SetEnvironmentVariable("SIH_TEST_DB_USERNAME", $null, [System.EnvironmentVariableTarget]::Process)
    [System.Environment]::SetEnvironmentVariable("SIH_TEST_DB_PASSWORD", $null, [System.EnvironmentVariableTarget]::Process)

    Pop-Location -ErrorAction SilentlyContinue
}

exit $mavenExitCode
