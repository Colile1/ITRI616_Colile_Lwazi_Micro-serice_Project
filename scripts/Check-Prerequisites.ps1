# Check-Prerequisites.ps1 : verifies all required tools are installed before starting the application

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Secure Leave Management System - Prerequisites Check" -ForegroundColor Cyan
Write-Host "  NWU ITRI615 | Colile Sibanda" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

$allPassed = $true

# Pure function: checks if a command exists and returns its version
function Test-Tool {
    param([string]$Name, [string]$Command, [string]$VersionArg, [string]$MinVersion)

    try {
        $output = & $Command $VersionArg 2>&1 | Select-Object -First 1
        $version = ($output -replace '[^0-9.]', '').Split('.')[0..1] -join '.'
        Write-Host "  [PASS] $Name found: $output" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "  [FAIL] $Name not found. Please install $Name." -ForegroundColor Red
        return $false
    }
}

# Check Java 17+
Write-Host "Checking Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    $versionNumber = [regex]::Match($javaVersion, '(\d+)').Value
    if ([int]$versionNumber -ge 17) {
        Write-Host "  [PASS] Java found: $javaVersion" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Java 17+ required. Found: $javaVersion" -ForegroundColor Red
        $allPassed = $false
    }
} catch {
    Write-Host "  [FAIL] Java not found. Install Java 17+ from https://adoptium.net/" -ForegroundColor Red
    $allPassed = $false
}

# Check Maven
Write-Host "Checking Maven..." -ForegroundColor Yellow
try {
    $mvnVersion = mvn --version 2>&1 | Select-Object -First 1
    Write-Host "  [PASS] Maven found: $mvnVersion" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Maven not found. Install Maven from https://maven.apache.org/" -ForegroundColor Red
    $allPassed = $false
}

# Check Node.js
Write-Host "Checking Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1
    $nodeNum = [regex]::Match($nodeVersion, '(\d+)').Value
    if ([int]$nodeNum -ge 16) {
        Write-Host "  [PASS] Node.js found: $nodeVersion" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] Node.js 16+ required. Found: $nodeVersion" -ForegroundColor Red
        $allPassed = $false
    }
} catch {
    Write-Host "  [FAIL] Node.js not found. Install from https://nodejs.org/" -ForegroundColor Red
    $allPassed = $false
}

# Check npm
Write-Host "Checking npm..." -ForegroundColor Yellow
try {
    $npmVersion = npm --version 2>&1
    Write-Host "  [PASS] npm found: v$npmVersion" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] npm not found (usually installed with Node.js)." -ForegroundColor Red
    $allPassed = $false
}

# Check port availability
Write-Host ""
Write-Host "Checking port availability..." -ForegroundColor Yellow
$ports = @(8080, 8081, 8082, 3000)
foreach ($port in $ports) {
    $inUse = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($inUse) {
        Write-Host "  [WARN] Port $port is in use. Stop the process before starting the app." -ForegroundColor Yellow
    } else {
        Write-Host "  [PASS] Port $port is available." -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host "  All prerequisites satisfied. Run Start-App.ps1 to start." -ForegroundColor Green
} else {
    Write-Host "  Some prerequisites are missing. Please install them first." -ForegroundColor Red
}
Write-Host "============================================================" -ForegroundColor Cyan
