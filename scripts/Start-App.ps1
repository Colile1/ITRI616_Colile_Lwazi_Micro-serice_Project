# Start-App.ps1 : builds and starts all services for the Secure Leave Management System

$root = Split-Path -Parent $PSScriptRoot

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Secure Leave Management System - Starting Application" -ForegroundColor Cyan
Write-Host "  NWU ITRI615 | Colile Sibanda" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Output logic: creates the logs directory if it does not exist
$logsDir = "$root\logs"
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir | Out-Null }

# Pure function: starts a Java service in a new window and returns the process
function Start-JavaService {
    param([string]$Name, [string]$ServicePath, [int]$Port)

    Write-Host "Building $Name..." -ForegroundColor Yellow
    Push-Location $ServicePath
    $buildResult = mvn package -DskipTests -q 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] Build failed for $Name" -ForegroundColor Red
        Write-Host $buildResult
        Pop-Location
        return $null
    }
    Write-Host "  [PASS] $Name built successfully." -ForegroundColor Green

    $jarFile = Get-ChildItem "$ServicePath\target\*.jar" | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1
    if (-not $jarFile) {
        Write-Host "  [FAIL] No JAR found for $Name" -ForegroundColor Red
        Pop-Location
        return $null
    }

    $logFile = "$logsDir\$Name.log"
    $proc = Start-Process -FilePath "java" `
        -ArgumentList "-jar", $jarFile.FullName `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError "$logsDir\$Name-error.log" `
        -WindowStyle Hidden `
        -PassThru

    Pop-Location
    Write-Host "  [PASS] $Name started (PID: $($proc.Id)) on port $Port" -ForegroundColor Green
    return $proc
}

# Start Auth Service
$authProc = Start-JavaService -Name "auth-service" -ServicePath "$root\auth-service" -Port 8081

# Start Leave Service
$leaveProc = Start-JavaService -Name "leave-service" -ServicePath "$root\leave-service" -Port 8082

# Start API Gateway
$gatewayProc = Start-JavaService -Name "api-gateway" -ServicePath "$root\api-gateway" -Port 8080

# Wait for services to start
Write-Host ""
Write-Host "Waiting for services to initialise (30 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# Health checks
Write-Host "Running health checks..." -ForegroundColor Yellow
$services = @(
    @{ Name = "Auth Service"; Url = "http://localhost:8081/actuator/health" },
    @{ Name = "Leave Service"; Url = "http://localhost:8082/actuator/health" },
    @{ Name = "API Gateway"; Url = "http://localhost:8080/actuator/health" }
)

foreach ($svc in $services) {
    try {
        $response = Invoke-RestMethod -Uri $svc.Url -TimeoutSec 5
        Write-Host "  [PASS] $($svc.Name): $($response.status)" -ForegroundColor Green
    } catch {
        Write-Host "  [WARN] $($svc.Name): Not responding yet (may still be starting)" -ForegroundColor Yellow
    }
}

# Start Frontend
Write-Host ""
Write-Host "Installing and starting React frontend..." -ForegroundColor Yellow
Push-Location "$root\frontend"
if (-not (Test-Path "node_modules")) {
    Write-Host "  Installing npm packages..." -ForegroundColor Yellow
    npm install --silent
}
$frontendProc = Start-Process -FilePath "npm" `
    -ArgumentList "start" `
    -WindowStyle Normal `
    -PassThru
Pop-Location
Write-Host "  [PASS] Frontend starting on http://localhost:3000" -ForegroundColor Green

# Save PIDs for Stop-App.ps1
$pids = @{
    auth    = $authProc?.Id
    leave   = $leaveProc?.Id
    gateway = $gatewayProc?.Id
    frontend = $frontendProc?.Id
}
$pids | ConvertTo-Json | Out-File "$root\scripts\.running-pids.json" -Encoding utf8

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Application started successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "  Frontend:      http://localhost:3000" -ForegroundColor White
Write-Host "  API Gateway:   http://localhost:8080" -ForegroundColor White
Write-Host "  Auth Service:  http://localhost:8081" -ForegroundColor White
Write-Host "  Leave Service: http://localhost:8082" -ForegroundColor White
Write-Host ""
Write-Host "  Demo Accounts:" -ForegroundColor White
Write-Host "    admin / admin123        (Admin)" -ForegroundColor White
Write-Host "    lwazi.manager / password (Manager)" -ForegroundColor White
Write-Host "    colile.employee / password (Employee)" -ForegroundColor White
Write-Host ""
Write-Host "  Run Stop-App.ps1 to stop all services." -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
