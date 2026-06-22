# Pulse-Check API — "Watchdog" Sentinel

A Dead Man's Switch monitoring service built for **CritMon Servers Inc.**, a critical
infrastructure monitoring company. Remote devices (solar farms, weather stations) send
periodic heartbeats to prove they're alive. If a device goes silent for longer than its
configured timeout, the system automatically flags it as down and fires an alert —
no human watching logs required.

Built with **Java 17** and **Spring Boot 3.3**.

---

## 1. Architecture Diagram

### State flow for a single monitor

```
                 register
                    │
                    ▼
            ┌───────────────┐
   ┌───────▶│    ACTIVE     │◀───────┐
   │        └───────┬───────┘        │
   │   heartbeat     │  timeout      │ heartbeat
   │  (resets timer) │  elapses      │ (auto un-pauses)
   │                 ▼               │
   │        ┌───────────────┐        │
   └────────│     DOWN      │        │
            └───────────────┘        │
                                      │
            ┌───────────────┐        │
            │    PAUSED     │────────┘
            └───────────────┘
                 ▲
                 │ pause
                 │ (from ACTIVE)
```

### Sequence diagram — heartbeat vs. expiry race

```
Device                    API                      Watchdog Sweep (every 1s)
  │                        │                                │
  │  POST /monitors        │                                │
  ├───────────────────────▶│ store Monitor{ACTIVE, t=60s}   │
  │  201 Created           │                                │
  │◀───────────────────────┤                                │
  │                        │                                │
  │   ... 45s pass ...     │                                │──▶ checks: 45s < 60s, OK
  │                        │                                │
  │ POST /heartbeat        │                                │
  ├───────────────────────▶│ lastHeartbeat = now()          │
  │  200 OK                │                                │
  │◀───────────────────────┤                                │
  │                        │                                │──▶ checks: 0s < 60s, OK
  │   ... 65s silence ...  │                                │
  │                        │                                │──▶ checks: 65s ≥ 60s
  │                        │                                │    → status = DOWN
  │                        │                                │    → log ALERT
  │ GET /monitors/{id}     │                                │
  ├───────────────────────▶│ status: DOWN                   │
  │◀───────────────────────┤                                │
```

**Design note:** instead of spawning one timer thread per device, a single scheduled
sweep (`@Scheduled(fixedRate = 1000)`) checks every active monitor once per second.
This keeps the implementation simple and safe — no per-device thread to leak or cancel —
at the cost of up to ~1 second of detection latency, which is acceptable for this use case.

---

## 2. Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.8+ (or use the included wrapper if you generate one)

### Run locally

```bash
git clone <your-fork-url>
cd pulse-check-api
mvn spring-boot:run
```

The API starts on **http://localhost:8080**.

### Run tests

```bash
mvn test
```

### Build a jar

```bash
mvn clean package
java -jar target/pulse-check-api-1.0.0.jar
```

---

## 3. API Documentation

All responses are JSON. Timestamps are ISO-8601.

### Register a monitor

```
POST /monitors
Content-Type: application/json

{
  "id": "device-123",
  "timeout": 60,
  "alert_email": "admin@critmon.com"
}
```

**201 Created**
```json
{
  "id": "device-123",
  "status": "ACTIVE",
  "timeout": 60,
  "seconds_remaining": 60,
  "message": "Monitor registered successfully."
}
```

**409 Conflict** — if `id` is already registered.

---

### Send a heartbeat

```
POST /monitors/{id}/heartbeat
```

**200 OK**
```json
{
  "id": "device-123",
  "status": "ACTIVE",
  "timeout": 60,
  "seconds_remaining": 60,
  "message": "Heartbeat received. Timer reset."
}
```

**404 Not Found** — if `id` does not exist.

> Calling heartbeat on a **paused** monitor automatically un-pauses it and restarts
> the countdown, as specified in the bonus user story.

---

### Pause a monitor

```
POST /monitors/{id}/pause
```

**200 OK**
```json
{
  "id": "device-123",
  "status": "PAUSED",
  "timeout": 60,
  "seconds_remaining": 0,
  "message": "Monitor paused. No alerts will fire until next heartbeat."
}
```

**404 Not Found** — if `id` does not exist.

---

### Get monitor status *(Developer's Choice — see below)*

```
GET /monitors/{id}
```

**200 OK**
```json
{
  "id": "device-123",
  "status": "DOWN",
  "timeout": 60,
  "seconds_remaining": 0,
  "message": "Current monitor status."
}
```

**404 Not Found** — if `id` does not exist.

---

### Error format

```json
{
  "error": "Monitor not found: device-999",
  "timestamp": "2026-06-22T20:30:00Z"
}
```

---

## 4. Design Decisions

- **Storage — `ConcurrentHashMap<String, Monitor>`:** simplest store that's safe
  under concurrent register/heartbeat/pause calls from many devices, without pulling
  in an external database for what is fundamentally in-memory state.
- **Per-monitor locking, not a global lock:** each `Monitor` is mutated under
  `synchronized (monitor)`. This means heartbeats from *different* devices never
  block each other — only operations on the *same* device id serialize, which is
  the only contention that actually matters here.
- **Polling sweep over per-device timers:** a single `@Scheduled` task checks all
  active monitors once a second rather than scheduling a `Timer`/`Future` per
  device. Far simpler to reason about, with no leaked or hard-to-cancel timer
  threads, at the cost of ~1s worst-case detection delay.
- **`volatile` fields on `Monitor`:** `lastHeartbeat` and `status` are read by both
  the HTTP-handling threads and the scheduled sweep thread; `volatile` guarantees
  visibility of the latest value across threads without requiring a lock for reads.

---

## 5. The Developer's Choice — On-Demand Status Lookup

**Feature added:** `GET /monitors/{id}` — lets anyone check a device's current
status, timeout, and seconds remaining at any time.

**Why:** the original spec only surfaces a device going down via a server-side log
line (`console.log`/`ALERT`). In CritMon's real use case, an operator or a dashboard
needs to *ask* "is device-123 healthy right now?" without grepping server logs. This
read-only endpoint makes the monitor's state queryable on demand, which is the
minimum building block needed for any future status dashboard, polling-based alert
integration, or health-check page — without changing any of the existing write
behavior.

---

## 6. Project Structure

```
src/main/java/com/amalitech/pulsecheck/
├── PulseCheckApiApplication.java   # entry point, @EnableScheduling
├── controller/MonitorController   # HTTP endpoints
├── service/MonitorService         # core watchdog logic + scheduled sweep
├── model/Monitor                  # domain entity
├── model/MonitorStatus            # ACTIVE / PAUSED / DOWN
├── dto/                           # request/response shapes
└── exception/                     # 404 / 409 / validation handling
```
