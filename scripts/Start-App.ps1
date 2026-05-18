# Start-App.ps1 : builds and starts all services via Docker Compose

$root = Split-Path -Parent $PSScriptRoot

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Secure Leave Management System - Starting Application" -ForegroundColor Cyan
Write-Host "  NWU ITRI615 | Colile Sibanda" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Check Docker is running
try {
    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw }
} catch {
    Write-Host "  [FAIL] Docker is not running. Start Docker Desktop and try again." -ForegroundColor Red
    exit 1
}

# Build Java JARs (required before docker-compose build)
$services = @("auth-service", "leave-service", "api-gateway")
foreach ($svc in $services) {
    Write-Host "Building $svc JAR..." -ForegroundColor Yellow
    Push-Location "$root\$svc"
    mvn package -DskipTests -q 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] Maven build failed for $svc. Check Java/Maven installation." -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Write-Host "  [PASS] $svc JAR built." -ForegroundColor Green
    Pop-Location
}

# Build Docker images
Write-Host ""
Write-Host "Building Docker images..." -ForegroundColor Yellow
Set-Location $root
docker-compose build 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] docker-compose build failed." -ForegroundColor Red
    exit 1
}
Write-Host "  [PASS] All images built." -ForegroundColor Green

# Start containers
Write-Host ""
Write-Host "Starting containers..." -ForegroundColor Yellow
docker-compose up -d 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] docker-compose up failed." -ForegroundColor Red
    exit 1
}

# Wait for health checks
Write-Host "Waiting for services to become healthy (up to 60 seconds)..." -ForegroundColor Yellow
$timeout = 60
$elapsed = 0
$allHealthy = $false
while ($elapsed -lt $timeout) {
    Start-Sleep -Seconds 5
    $elapsed += 5
    $statuses = docker ps --format "{{.Names}}|{{.Status}}" 2>&1
    $unhealthy = $statuses | Where-Object { $_ -match "starting|unhealthy" }
    $exited    = $statuses | Where-Object { $_ -match "Exited" }
    if ($exited) {
        Write-Host "  [FAIL] One or more containers exited. Run 'docker-compose logs' for details." -ForegroundColor Red
        exit 1
    }
    if (-not $unhealthy) { $allHealthy = $true; break }
    Write-Host "  ... still starting ($elapsed s)" -ForegroundColor Gray
}

if (-not $allHealthy) {
    Write-Host "  [WARN] Services did not all report healthy within $timeout seconds." -ForegroundColor Yellow
    Write-Host "         Run 'docker ps' to check status manually." -ForegroundColor Yellow
} else {
    Write-Host "  [PASS] All services healthy." -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Application started successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "  Frontend:      http://localhost:3000" -ForegroundColor White
Write-Host "  API Gateway:   http://localhost:8080" -ForegroundColor White
Write-Host "  Auth Service:  http://localhost:8081/actuator/health" -ForegroundColor White
Write-Host "  Leave Service: http://localhost:8082/actuator/health" -ForegroundColor White
Write-Host ""
Write-Host "  Demo Accounts:" -ForegroundColor White
Write-Host "    admin / Admin@1234            (Admin)" -ForegroundColor White
Write-Host "    lwazi.manager / Manager@1234  (Manager)" -ForegroundColor White
Write-Host "    colile.employee / Employee@1234 (Employee)" -ForegroundColor White
Write-Host ""
Write-Host "  Run .\scripts\Stop-App.ps1 to stop all services." -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
