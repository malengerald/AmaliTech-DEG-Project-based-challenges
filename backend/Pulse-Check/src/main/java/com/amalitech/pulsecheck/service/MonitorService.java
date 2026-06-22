package com.amalitech.pulsecheck.service;

import com.amalitech.pulsecheck.exception.MonitorAlreadyExistsException;
import com.amalitech.pulsecheck.exception.MonitorNotFoundException;
import com.amalitech.pulsecheck.model.Monitor;
import com.amalitech.pulsecheck.model.MonitorStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core "Watchdog" logic for the Pulse-Check API.
 *
 * <p>Storage choice: a {@link ConcurrentHashMap} keyed by device id. This is the
 * simplest store that is still safe under concurrent heartbeat/register/pause calls
 * from many devices at once, without needing an external database for this challenge.
 *
 * <p>Each individual {@link Monitor} is mutated under a lock on that specific monitor
 * instance, so two devices never block each other — only concurrent operations on the
 * *same* device id serialize, which is exactly the contention we actually care about.
 *
 * <p>Expiry detection: rather than spawning one timer/thread per device (which gets
 * expensive and fiddly to cancel correctly), a single {@code @Scheduled} sweep runs
 * every second and checks every ACTIVE monitor's elapsed time. This trades a small,
 * constant polling cost for much simpler and safer code — a reasonable tradeoff at
 * the scale this challenge implies.
 */
@Service
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);

    private final Map<String, Monitor> monitors = new ConcurrentHashMap<>();

    /**
     * User Story 1: Register a new monitor and start its countdown.
     */
    public Monitor register(String id, int timeoutSeconds, String alertEmail) {
        Monitor created = new Monitor(id, timeoutSeconds, alertEmail);
        Monitor existing = monitors.putIfAbsent(id, created);
        if (existing != null) {
            throw new MonitorAlreadyExistsException(id);
        }
        log.info("Monitor registered: id={}, timeout={}s", id, timeoutSeconds);
        return created;
    }

    /**
     * User Story 2: Heartbeat - reset the countdown for a device.
     * Also un-pauses a paused monitor (per the Bonus User Story spec).
     */
    public Monitor heartbeat(String id) {
        Monitor monitor = getOrThrow(id);
        synchronized (monitor) {
            monitor.setLastHeartbeat(Instant.now());
            monitor.setStatus(MonitorStatus.ACTIVE);
        }
        log.info("Heartbeat received: id={}", id);
        return monitor;
    }

    /**
     * Bonus User Story: Pause monitoring for a device (e.g. during maintenance).
     */
    public Monitor pause(String id) {
        Monitor monitor = getOrThrow(id);
        synchronized (monitor) {
            monitor.setStatus(MonitorStatus.PAUSED);
        }
        log.info("Monitor paused: id={}", id);
        return monitor;
    }

    /**
     * Developer's Choice feature: allow looking up a single monitor's current status
     * on demand, instead of only finding out it's down via the server log.
     * See README "Developer's Choice" section for rationale.
     */
    public Monitor getStatus(String id) {
        return getOrThrow(id);
    }

    private Monitor getOrThrow(String id) {
        Monitor monitor = monitors.get(id);
        if (monitor == null) {
            throw new MonitorNotFoundException(id);
        }
        return monitor;
    }

    /**
     * User Story 3: The Watchdog sweep.
     * Runs every second, checks every ACTIVE monitor, and fires an alert
     * for any monitor whose timeout has elapsed without a heartbeat.
     */
    @Scheduled(fixedRate = 1000)
    void sweepForExpiredMonitors() {
        for (Monitor monitor : monitors.values()) {
            if (monitor.getStatus() != MonitorStatus.ACTIVE) {
                continue;
            }
            synchronized (monitor) {
                if (monitor.getStatus() == MonitorStatus.ACTIVE && monitor.isExpired()) {
                    monitor.setStatus(MonitorStatus.DOWN);
                    fireAlert(monitor);
                }
            }
        }
    }

    /**
     * Simulates firing an alert. In production this would call a webhook or
     * send an email via monitor.getAlertEmail(); here we log it as specified
     * in the challenge brief.
     */
    private void fireAlert(Monitor monitor) {
        log.error("{{\"ALERT\": \"Device {} is down!\", \"time\": \"{}\", \"alert_email\": \"{}\"}}",
                monitor.getId(), Instant.now(), monitor.getAlertEmail());
    }
}
