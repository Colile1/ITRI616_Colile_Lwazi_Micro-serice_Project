# Stop-App.ps1 : gracefully stops all running services

$root = Split-Path -Parent $PSScriptRoot

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Secure Leave Management System - Stopping Application" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Output logic: stops a process by PID if it is still running
function Stop-ServiceProcess {
    param([int]$Pid, [string]$Name)
    try {
        $proc = Get-Process -Id $Pid -ErrorAction SilentlyContinue
        if ($proc) {
            $proc | Stop-Process -Force
            Write-Host "  [PASS] $Name stopped (PID: $Pid)." -ForegroundColor Green
        } else {
            Write-Host "  [INFO] $Name (PID: $Pid) was not running." -ForegroundColor Gray
        }
    } catch {
        Write-Host "  [WARN] Could not stop $Name (PID: $Pid): $_" -ForegroundColor Yellow
    }
}

# Load saved PIDs
$pidFile = "$root\scripts\.running-pids.json"
if (Test-Path $pidFile) {
    $pids = Get-Content $pidFile | ConvertFrom-Json
    Stop-ServiceProcess -Pid $pids.gateway -Name "API Gateway"
    Stop-ServiceProcess -Pid $pids.auth -Name "Auth Service"
    Stop-ServiceProcess -Pid $pids.leave -Name "Leave Service"
    Stop-ServiceProcess -Pid $pids.frontend -Name "Frontend"
    Remove-Item $pidFile -Force
} else {
    Write-Host "  No running PID file found. Attempting to kill by port..." -ForegroundColor Yellow
    @(8080, 8081, 8082) | ForEach-Object {
        $port = $_
        $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($conn) {
            $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
            if ($proc) {
                $proc | Stop-Process -Force
                Write-Host "  [PASS] Stopped process on port $port (PID: $($conn.OwningProcess))." -ForegroundColor Green
            }
        }
    }
    # Kill npm/node processes on port 3000
    $npmProcs = Get-Process -Name "node" -ErrorAction SilentlyContinue
    if ($npmProcs) {
        $npmProcs | Stop-Process -Force
        Write-Host "  [PASS] Stopped Node.js/frontend processes." -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "  All services stopped." -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
