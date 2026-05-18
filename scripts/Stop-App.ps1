# Stop-App.ps1 : gracefully stops all services via Docker Compose

$root = Split-Path -Parent $PSScriptRoot

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Secure Leave Management System - Stopping Application" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

Set-Location $root

# Check Docker is running
try {
    docker info 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw }
} catch {
    Write-Host "  [WARN] Docker does not appear to be running." -ForegroundColor Yellow
    exit 0
}

docker-compose down 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [PASS] All containers stopped and removed." -ForegroundColor Green
} else {
    Write-Host "  [WARN] docker-compose down reported an error. Containers may already be stopped." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "  All services stopped." -ForegroundColor Green
Write-Host "  To also remove volumes: docker-compose down -v" -ForegroundColor Gray
Write-Host "============================================================" -ForegroundColor Cyan
