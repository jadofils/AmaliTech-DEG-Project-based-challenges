# Watchdog Sentinel API

> A Production-Ready Dead Man's Switch for Critical Infrastructure Monitoring

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Development](#development)
- [Deployment](#deployment)
- [The Developer's Choice Feature](#the-developers-choice-feature)
- [Monitoring and Observability](#monitoring-and-observability)
- [Troubleshooting](#troubleshooting)

---

## Overview

**Watchdog Sentinel** is a dead man's switch API designed for remote devices in low-connectivity environments such as solar farms, weather stations, and IoT sensors. It automatically detects when devices stop reporting and triggers alerts with no human intervention required.

### Core Capabilities

| Feature | Description |
|---|---|
| **Dead Man's Switch** | Automatic alerting when devices fail to send heartbeats |
| **Configurable Timeouts** | Per-device timeout from 10 seconds to 24 hours |
| **Heartbeat Reset** | REST endpoints to reset timers on demand |
| **Maintenance Mode** | Pause monitoring during repairs without false alarms |
| **Real-time Status** | Check device health at any moment |
| **Persistent Storage** | No data loss on server restart via PostgreSQL |

---

## System Architecture

### High-Level Architecture

```mermaid
flowchart TB
    subgraph "External Systems"
        Device[Remote Device\nSolar Farm / Weather Station]
        Admin[Support Engineer\nAdministrator]
        Webhook[Alert Webhook\nSlack / PagerDuty]
    end

    subgraph "Watchdog Sentinel API"
        direction TB

        subgraph "API Layer - Port 8080"
            Controller[Monitor Controller\nREST Endpoints]
            Validation[Request Validation\nJakarta Validation]
            Swagger[Swagger UI\nAPI Documentation]
        end

        subgraph "Service Layer - Business Logic"
            MonitorService[Monitor Service\nCRUD Operations]
            TimerService[Timer Service\nCountdown Management]
            AlertService[Alert Service\nNotification Dispatcher]
        end

        subgraph "Data Layer - Persistence"
            Repository[Monitor Repository\nSpring Data JPA]
            DB[(PostgreSQL\nPersistent Storage)]
        end

        subgraph "Background Jobs"
            TimerScheduler[ScheduledExecutor\nTimer Management]
            CleanupScheduler[Cleanup Scheduler\nDaily at 2 AM]
        end

        subgraph "Cross-Cutting Concerns"
            ExceptionHandler[Global Exception Handler\nError Responses]
        end
    end

    Device --> Controller
    Admin --> Swagger
    Admin --> Controller
    Controller --> Validation
    Validation --> MonitorService
    MonitorService --> TimerService
    MonitorService --> Repository
    Repository --> DB
    TimerService --> TimerScheduler
    TimerService --> AlertService
    AlertService --> Webhook
    CleanupScheduler --> DB
    Controller --> ExceptionHandler

    style Device fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    style Admin fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    style Webhook fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style AlertService fill:#ffebee,stroke:#c62828,stroke-width:2px
    style DB fill:#f3e5f5,stroke:#6a1b9a,stroke-width:2px
    style Controller fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    style MonitorService fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    style TimerService fill:#fff9c4,stroke:#f57f17,stroke-width:2px
```

### Sequence Diagram: Heartbeat and Alert Flow

```mermaid
sequenceDiagram
    autonumber
    participant D as Device
    participant C as MonitorController
    participant S as MonitorService
    participant T as TimerService
    participant R as MonitorRepository
    participant A as AlertService
    participant L as LogFile

    Note over D,L: Normal Heartbeat Flow

    D->>C: POST /api/monitors/{id}/heartbeat
    activate C
    C->>S: sendHeartbeat(id)
    activate S
    S->>R: findById(id)
    R-->>S: Monitor Entity

    alt Monitor Status: ACTIVE
        S->>R: updateLastHeartbeat(id, now())
        S->>T: resetTimer(id, timeout)
        activate T
        T->>T: cancel existing timer
        T->>T: schedule new timer
        T-->>S: timer scheduled
        deactivate T
        S-->>C: HeartbeatResponse(timeRemaining)

    else Monitor Status: PAUSED
        S->>R: updateStatus(id, ACTIVE)
        S->>R: updateLastHeartbeat(id, now())
        S->>T: startTimer(id, timeout)
        S-->>C: HeartbeatResponse(resumed=true)

    else Monitor Status: DOWN
        S-->>C: MonitorExpiredException
        C-->>D: 410 Gone

    else Monitor Not Found
        S-->>C: MonitorNotFoundException
        C-->>D: 404 Not Found
    end

    deactivate S
    C-->>D: 200 OK
    deactivate C

    Note over D,L: Timer Expiration and Alert Flow

    T->>T: timer expires
    activate T
    T->>S: processExpiredMonitor(id)
    activate S
    S->>R: findById(id)
    R-->>S: Monitor Entity

    alt Status still ACTIVE
        S->>R: updateStatus(id, DOWN)
        S->>R: incrementAlertCount(id)
        S->>A: sendAlert(monitor)
        activate A
        A->>L: log.error("ALERT: Device down")
        A->>A: buildAlertPayload()
        A->>L: log.info("Alert dispatched")
        deactivate A
        S-->>T: alert sent
    else Status already changed
        S-->>T: already alerted
    end

    deactivate S
    deactivate T
```

### State Flow Diagram

```mermaid
stateDiagram-v2
    [*] --> Creation: POST /monitors

    Creation --> ACTIVE: Monitor created\nTimer started

    ACTIVE --> ACTIVE: Heartbeat received\nTimer reset
    ACTIVE --> DOWN: Timer expires\nNo heartbeat
    ACTIVE --> PAUSED: POST /pause\nManual intervention

    PAUSED --> ACTIVE: Heartbeat received\nAuto-resume
    PAUSED --> [*]: DELETE /{id}\nManual deletion

    DOWN --> [*]: Auto-cleanup\nAfter 24 hours
    DOWN --> [*]: DELETE /{id}\nManual deletion

    note right of ACTIVE
        Timer running
        Alerts active
        Status: Monitoring
    end note

    note right of PAUSED
        Timer stopped
        No alerts
        Status: Maintenance
    end note

    note right of DOWN
        Alert sent
        Needs attention
        Status: Failed
    end note
```

### Database Schema

```mermaid
erDiagram
    MONITOR {
        string id PK "Device ID (unique)"
        int timeout "Timeout in seconds"
        string alert_email "Notification email"
        string alert_webhook "Webhook URL (optional)"
        enum status "ACTIVE / PAUSED / DOWN"
        datetime last_heartbeat "Last ping timestamp"
        datetime alert_triggered_at "Last alert time"
        int alert_count "Alert counter"
        datetime created_at "Record creation"
        datetime updated_at "Record update"
    }

    AUDIT_LOG {
        int id PK
        string monitor_id FK
        string action "CREATE / HEARTBEAT / ALERT"
        datetime timestamp
        string details "JSON metadata"
    }

    MONITOR ||--o{ AUDIT_LOG : generates
```

### Deployment Architecture

```mermaid
flowchart LR
    subgraph "Docker Host"
        direction TB
        subgraph "watchdog-sentinel container"
            App[Spring Boot App\nPort 8080]
        end
    end

    subgraph "External Services"
        DB[(Neon PostgreSQL\nCloud DB)]
        Webhook[Alert Webhook\nSlack / Email]
    end

    subgraph "Client"
        Device[Remote Device]
        Admin[Administrator]
    end

    Device --> App
    Admin --> App
    App --> DB
    App --> Webhook

    style App fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    style DB fill:#f3e5f5,stroke:#6a1b9a,stroke-width:2px
    style Webhook fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style Device fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    style Admin fill:#e1f5fe,stroke:#01579b,stroke-width:2px
```

---

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL (or use the provided Neon DB connection)

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/jadofils/AmaliTech-DEG-Project-based-challenges.git
cd AmaliTech-DEG-Project-based-challenges/backend

# 2. Set environment variables
export DB_PASSWORD=your_db_password
export PORT=8080

# 3. Build the application
./mvnw clean package

# 4. Run the application
./mvnw spring-boot:run

# 5. Verify it is working
curl http://localhost:8080/api/monitors/health
```

**Expected Output:**

```json
{
  "status": "healthy",
  "timestamp": "2024-01-15T10:30:00",
  "service": "watchdog-sentinel"
}
```

---

## API Documentation

**Base URL:** `http://localhost:8080/api`

**Interactive Docs:** `http://localhost:8080/swagger-ui/index.html`

---

### 1. Create Monitor

Registers a new device for heartbeat monitoring. Once created, the countdown timer starts immediately. If no heartbeat is received before the timeout expires, an alert is fired and the monitor status changes to `DOWN`. The device ID must be unique — attempting to register the same ID twice returns a `409 Conflict`.

```
POST /monitors
Content-Type: application/json
```

**Request Body:**

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | string | yes | Unique device identifier (3-100 chars, letters/numbers/hyphens/underscores) |
| `timeout` | integer | yes | Seconds before alert fires (10 - 86400) |
| `alert_email` | string | yes | Email address to notify on alert |
| `alert_webhook` | string | no | HTTP endpoint to POST alert payload to |

**Test examples:**

```json
{
  "id": "solar-panel-001",
  "timeout": 60,
  "alert_email": "admin@critmon.com",
  "alert_webhook": ""
}
```

```json
{
  "id": "weather-station-042",
  "timeout": 120,
  "alert_email": "ops@critmon.com",
  "alert_webhook": ""
}
```

```json
{
  "id": "wind-turbine-007",
  "timeout": 30,
  "alert_email": "engineer@critmon.com",
  "alert_webhook": ""
}
```

```json
{
  "id": "iot-sensor-lab-3",
  "timeout": 300,
  "alert_email": "support@critmon.com",
  "alert_webhook": ""
}
```

**Response `201 Created`:**

```json
{
  "id": "solar-panel-001",
  "timeout": 60,
  "status": "ACTIVE",
  "alertEmail": "admin@critmon.com",
  "lastHeartbeat": "2024-01-15T10:30:00",
  "createdAt": "2024-01-15T10:30:00",
  "message": "Monitor created successfully"
}
```

**Error `400 Bad Request`** — validation failed:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "validation_errors": {
    "timeout": "Timeout must be at least 10 seconds"
  }
}
```

**Error `409 Conflict`** — device already registered:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Monitor already exists for device: solar-panel-001"
}
```

---

### 2. Send Heartbeat

Proves the device is still alive by resetting the countdown timer back to the full timeout value. Call this endpoint from your device on a regular interval shorter than the timeout. If the monitor is `PAUSED`, sending a heartbeat automatically resumes it. If the monitor is `DOWN`, you must delete and re-register it.

```
POST /monitors/{id}/heartbeat
```

**Test examples** — use the IDs registered above:

```
POST /monitors/solar-panel-001/heartbeat
POST /monitors/weather-station-042/heartbeat
POST /monitors/wind-turbine-007/heartbeat
POST /monitors/iot-sensor-lab-3/heartbeat
```

**Response `200 OK`:**

```json
{
  "deviceId": "solar-panel-001",
  "message": "Heartbeat received - timer reset",
  "timeRemaining": 60,
  "resumed": false,
  "timestamp": "2024-01-15T10:31:00"
}
```

**Error `404 Not Found`** — device not registered:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Monitor not found for device: solar-panel-001"
}
```

**Error `410 Gone`** — device is DOWN, must re-register:

```json
{
  "status": 410,
  "error": "Gone",
  "message": "Monitor is expired. Create a new monitor to resume tracking."
}
```

---

### 3. Get Monitor Status

Returns the full current state of a monitor. Use this to check whether a device is alive, how much time is left before an alert fires, and how many alerts have been triggered historically.

```
GET /monitors/{id}/status
```

**Test examples:**

```
GET /monitors/solar-panel-001/status
GET /monitors/weather-station-042/status
```

**Response `200 OK`:**

```json
{
  "id": "solar-panel-001",
  "status": "ACTIVE",
  "timeout": 60,
  "timeRemaining": 45,
  "lastHeartbeat": "2024-01-15T10:31:00",
  "createdAt": "2024-01-15T10:30:00",
  "alertCount": 0,
  "alertEmail": "admin@critmon.com"
}
```

**Status Values:**

| Status | Meaning |
|---|---|
| `ACTIVE` | Timer is running, device is being monitored |
| `PAUSED` | Timer stopped, no alerts will fire |
| `DOWN` | Timer expired, alert was triggered, device needs attention |

---

### 4. Pause Monitoring

Stops the countdown timer without triggering an alert. Use this when you are doing planned maintenance on a device and do not want false alarms. The monitor stays `PAUSED` indefinitely until a heartbeat is received, which auto-resumes it.

```
POST /monitors/{id}/pause
```

**Test examples:**

```
POST /monitors/wind-turbine-007/pause
POST /monitors/iot-sensor-lab-3/pause
```

**Response `200 OK`:**

```json
{
  "id": "wind-turbine-007",
  "status": "PAUSED",
  "message": "Monitor paused successfully. Send heartbeat to resume."
}
```

> To resume: simply send a heartbeat to the paused device. It auto-transitions back to `ACTIVE`.

---

### 5. Resume Monitoring

Manually resumes a `PAUSED` monitor and restarts the countdown timer. Sending a heartbeat also resumes automatically — this endpoint is for cases where you want to resume without counting as a heartbeat.

```
POST /monitors/{id}/resume
```

**Test example:**

```
POST /monitors/wind-turbine-007/resume
```

**Response `200 OK`:**

```json
{
  "id": "wind-turbine-007",
  "status": "ACTIVE",
  "message": "Monitor resumed successfully"
}
```

---

### 6. Delete Monitor

Permanently removes a monitor and cancels its timer. Use this when a device is decommissioned. This action cannot be undone — you must re-register the device to monitor it again.

```
DELETE /monitors/{id}
```

**Test example:**

```
DELETE /monitors/solar-panel-001
```

**Response `200 OK`:**

```json
{
  "message": "Monitor deleted successfully"
}
```

---

### 7. List All Monitors

Returns a paginated list of all monitors. Filter by status to see only active, paused, or down devices. Useful for building dashboards or bulk status checks.

```
GET /monitors?status=ACTIVE&page=0&size=20&sortBy=createdAt&direction=desc
```

| Parameter | Default | Description |
|---|---|---|
| `status` | all | Filter by `ACTIVE`, `PAUSED`, or `DOWN` |
| `page` | `0` | Page number (0-indexed) |
| `size` | `20` | Items per page |
| `sortBy` | `createdAt` | Field to sort by |
| `direction` | `desc` | `asc` or `desc` |

**Test examples:**

```
GET /monitors
GET /monitors?status=ACTIVE
GET /monitors?status=DOWN&page=0&size=10
```

**Response `200 OK`:**

```json
{
  "content": [
    {
      "id": "solar-panel-001",
      "timeout": 60,
      "status": "ACTIVE",
      "alertEmail": "admin@critmon.com",
      "lastHeartbeat": "2024-01-15T10:31:00",
      "createdAt": "2024-01-15T10:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 4,
  "totalPages": 1
}
```

---

### 8. Health Check

Returns the current health of the API. Use this to verify the server is running, the database is connected, and to see how many timers are currently active in memory.

```
GET /monitors/health
```

**Response `200 OK`:**

```json
{
  "status": "healthy",
  "timestamp": "2024-01-15T10:30:00",
  "service": "watchdog-sentinel",
  "activeTimers": 4,
  "database": "connected"
}
```

---

## Project Structure

```
backend/
src/main/java/com/watchdog/
├── WatchdogApplication.java
├── config/
│   ├── AsyncConfig.java
│   ├── TimerConfig.java
│   └── OpenAPIConfig.java
├── controller/
│   └── MonitorController.java
├── service/
│   ├── MonitorService.java
│   ├── TimerService.java
│   ├── AlertService.java
│   └── impl/
│       └── MonitorServiceImpl.java
├── repository/
│   └── MonitorRepository.java
├── model/
│   ├── entity/
│   │   └── Monitor.java
│   └── enums/
│       └── MonitorStatus.java
├── dto/
│   ├── request/
│   │   ├── CreateMonitorRequest.java
│   │   └── HeartbeatRequest.java
│   └── response/
│       ├── MonitorResponse.java
│       ├── HeartbeatResponse.java
│       └── StatusResponse.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── MonitorNotFoundException.java
│   ├── MonitorExpiredException.java
│   └── InvalidHeartbeatException.java
├── validation/
│   └── ValidTimeout.java
└── scheduler/
    └── CleanupScheduler.java
```

---

## Development

### Local Setup

```bash
# Use development profile
export SPRING_PROFILES_ACTIVE=dev

# Run with hot reload
./mvnw spring-boot:run

# Build JAR
./mvnw clean package
java -jar target/watchdog-sentinel-1.0.0.jar
```

### Running Tests

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify
```

---

## Deployment

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `PORT` | Server port | `8080` |
| `DB_PASSWORD` | PostgreSQL password | required |
| `SPRING_DATASOURCE_URL` | Full JDBC database URL | set in yaml |
| `ALERT_WEBHOOK_ENABLED` | Enable webhook alerts | `false` |
| `ALERT_WEBHOOK_URL` | Default webhook URL | none |
| `LOG_LEVEL` | Logging level | `INFO` |

### Docker Deployment

**1. Build the image:**

```bash
docker build -t watchdog-sentinel:latest .
```

**2. Run with Docker:**

```bash
docker run -d \
  --name watchdog-sentinel \
  -p 8080:8080 \
  -e DB_PASSWORD=your_db_password \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host/neondb?sslmode=require \
  -e PORT=8080 \
  watchdog-sentinel:latest
```

**3. Run with Docker Compose:**

Create a `docker-compose.yml` at the project root:

```yaml
version: '3.8'

services:
  app:
    build: .
    container_name: watchdog-sentinel
    ports:
      - "8080:8080"
    environment:
      - DB_PASSWORD=${DB_PASSWORD}
      - SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}
      - PORT=8080
      - LOG_LEVEL=INFO
    env_file:
      - .env
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/monitors/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

**4. Start with Docker Compose:**

```bash
# Start
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

**Dockerfile:**

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## The Developer's Choice Feature

### Intelligent Alert Deduplication with Circuit Breaker

**Why this was added:**
In environments with unstable connectivity — solar farms, remote weather stations, IoT sensors — a device may go offline and recover repeatedly within a short period. A standard dead man's switch fires an alert on every single failure. This floods the on-call engineer with hundreds of notifications per hour, a well-known problem called **alert fatigue**. Engineers start ignoring alerts, which defeats the entire purpose of the monitoring system.

**This is not AI.** It is a rule-based circuit breaker algorithm — a proven pattern from distributed systems engineering. No machine learning is involved. It makes decisions purely based on alert count and time elapsed since the last alert.

**It does not need its own endpoint.** The logic runs internally inside `AlertService` every time a timer expires. It is completely transparent to the device. The device just stops sending heartbeats — the system decides intelligently whether to fire an alert or suppress it.

**How it works:**

```java
// First failure: alert immediately
if (alertCount == 0) {
    sendImmediateAlert();
}
// Cooldown passed (>60 min since last alert): reset and alert again
else if (minutesSinceLastAlert > 60) {
    sendAlert();
}
// Within the hour but under the cap: alert with backoff
else if (alertCount < 3) {
    sendAlertWithBackoff();
}
// Circuit breaker open: suppress alert, log warning
else {
    log.warn("Circuit breaker ENGAGED - alert suppressed");
}
```

**Impact in real scenarios:**

| Scenario | Without Feature | With Feature |
|---|---|---|
| Flaky connection | 100+ alerts per hour | 3 alerts maximum |
| Extended outage | Alert every 60 seconds | Alert once, then quiet |
| Maintenance window | False alarms | Paused, no alerts |
| After 1 hour cooldown | Still flooding | Circuit resets, alerts resume |
| After recovery | Manual cleanup needed | Auto-resume on heartbeat |

**What the alert payload includes:**

```json
{
  "ALERT": "Device solar-panel-001 is down!",
  "time": "2024-01-15T10:32:00",
  "device_id": "solar-panel-001",
  "alert_email": "admin@critmon.com",
  "type": "timeout",
  "timeout_seconds": 60,
  "last_heartbeat": "2024-01-15T10:31:00",
  "alert_count": 2,
  "circuit_breaker_status": "CLOSED"
}
```

When the circuit breaker engages, the log shows:

```
WARN - Circuit breaker ENGAGED for device: solar-panel-001 - too many alerts (3 in last hour)
```

When the cooldown passes and the circuit resets:

```
INFO - Circuit breaker RESET for device: solar-panel-001 - cooldown period passed
```

---

## Monitoring and Observability

### Prometheus Metrics

Add to `application.yaml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

Metrics endpoint: `http://localhost:8080/actuator/prometheus`

```
watchdog_monitors_total{status="active"} 150
watchdog_heartbeats_total 12500
watchdog_alerts_total 24
```

### Log Output Example

```
2024-01-15 10:30:00 INFO  - Created monitor: solar-panel-001 (timeout: 60s)
2024-01-15 10:31:00 INFO  - Heartbeat received: solar-panel-001, timer reset
2024-01-15 10:32:00 ERROR - ALERT: {"ALERT":"Device solar-panel-001 is down!", "time":"2024-01-15T10:32:00"}
2024-01-15 10:32:00 INFO  - Email simulated: To: admin@critmon.com
```

---

## Troubleshooting

| Issue | Symptom | Solution |
|---|---|---|
| Port already in use | `BindException` | Change port: `--server.port=8081` |
| DB connection failed | `PSQLException` | Verify `DB_PASSWORD` env variable is set |
| Timer not firing | No alerts in logs | Check `@EnableScheduling` is on main class |
| Memory leak | `OutOfMemoryError` | Increase heap: `-Xmx512m` |

### Debug Mode

```bash
export LOG_LEVEL=DEBUG
java -jar watchdog-sentinel.jar

# Attach remote debugger on port 5005
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar watchdog-sentinel.jar
```

---

**Version:** 1.0.0 | **Java:** 17 | **Spring Boot:** 3.3.0 | **Database:** PostgreSQL (Neon)
