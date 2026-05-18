# starter_guide.md – How to Start and Test the Application

## Prerequisites

Run the prerequisite check:
```powershell
.\scripts\Check-Prerequisites.ps1
```

Required tools:
- Java 17+ (https://adoptium.net/)
- Apache Maven 3.8+ (https://maven.apache.org/)
- Node.js 16+ and npm (https://nodejs.org/)

---

## Starting the Application

### Option 1: Use the Start-App Script (Recommended)
```powershell
.\scripts\Start-App.ps1
```

This script:
1. Builds all three Java services with Maven
2. Starts Auth Service (port 8081)
3. Starts Leave Service (port 8082)
4. Starts API Gateway (port 8080)
5. Installs npm dependencies and starts the React frontend (port 3000)
6. Runs health checks on all services

### Option 2: Start Services Manually

**Terminal 1 – Auth Service:**
```powershell
cd auth-service
mvn spring-boot:run
```

**Terminal 2 – Leave Service:**
```powershell
cd leave-service
mvn spring-boot:run
```

**Terminal 3 – API Gateway:**
```powershell
cd api-gateway
mvn spring-boot:run
```

**Terminal 4 – Frontend:**
```powershell
cd frontend
npm install
npm start
```

---

## Stopping the Application

```powershell
.\scripts\Stop-App.ps1
```

---

## Access URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| Auth Service | http://localhost:8081 |
| Leave Service | http://localhost:8082 |
| Auth H2 Console | http://localhost:8081/h2-console |
| Leave H2 Console | http://localhost:8082/h2-console |
| Auth Prometheus | http://localhost:8081/actuator/prometheus |
| Gateway Prometheus | http://localhost:8080/actuator/prometheus |

---

## Demo Accounts

| Username | Password | Role |
|----------|----------|------|
| admin | Admin@1234 | Admin |
| lwazi.manager | Manager@1234 | Manager |
| colile.employee | Employee@1234 | Employee |
| thabo.employee | Employee@1234 | Employee |

---

## Testing Components Individually

### Test Auth Service (direct – port 8081)

```powershell
# Login
Invoke-RestMethod -Uri "http://localhost:8081/auth/login" -Method POST `
  -Body '{"username":"admin","password":"Admin@1234"}' -ContentType "application/json"

# Register new user
Invoke-RestMethod -Uri "http://localhost:8081/auth/register" -Method POST `
  -Body '{"username":"test.user","email":"test@example.com","password":"Test@1234","role":"EMPLOYEE"}' `
  -ContentType "application/json"
```

### Test via API Gateway (port 8080)

```powershell
# Login and save token
$token = (Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method POST `
  -Body '{"username":"colile.employee","password":"Employee@1234"}' -ContentType "application/json").token

# Get leave requests
Invoke-RestMethod -Uri "http://localhost:8080/leave" -Headers @{Authorization="Bearer $token"}

# Create leave request (adjust dates)
$tomorrow = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
$nextWeek = (Get-Date).AddDays(7).ToString("yyyy-MM-dd")
Invoke-RestMethod -Uri "http://localhost:8080/leave" -Method POST `
  -Headers @{Authorization="Bearer $token"} -ContentType "application/json" `
  -Body "{`"leaveType`":`"ANNUAL`",`"startDate`":`"$tomorrow`",`"endDate`":`"$nextWeek`",`"reason`":`"Annual leave for family holiday in Johannesburg`"}"
```

### Test Rate Limiting

```powershell
# Send 6 rapid login attempts — 6th should return HTTP 429
1..7 | ForEach-Object {
    try {
        Invoke-RestMethod -Uri "http://localhost:8081/auth/login" -Method POST `
          -Body '{"username":"wrong","password":"wrong"}' -ContentType "application/json"
    } catch {
        Write-Host "Attempt $_: HTTP $($_.Exception.Response.StatusCode.value__)"
    }
}
```

### Test RBAC

```powershell
# Employee token
$empToken = (Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method POST `
  -Body '{"username":"colile.employee","password":"Employee@1234"}' -ContentType "application/json").token

# Employee tries to approve — should return HTTP 403
try {
    Invoke-RestMethod -Uri "http://localhost:8080/leave/1/approve" -Method PUT `
      -Headers @{Authorization="Bearer $empToken"} -Body "{}" -ContentType "application/json"
} catch {
    Write-Host "HTTP $($_.Exception.Response.StatusCode.value__) — RBAC working correctly"
}
```

### Test Prometheus Metrics

```powershell
Invoke-WebRequest -Uri "http://localhost:8081/actuator/prometheus" | Select-Object -ExpandProperty Content | Select-Object -First 30
```

---

## Testing the Full System

Run the automated integration test:
```powershell
.\scripts\Check-Prerequisites.ps1
```

Or manually follow the test sequence:
1. Open http://localhost:3000 in a browser
2. Log in as `colile.employee / Employee@1234`
3. Submit a leave request
4. Log out
5. Log in as `lwazi.manager / Manager@1234`
6. Approve or reject the leave request
7. Log out
8. Log in as `colile.employee` again and verify the status updated
