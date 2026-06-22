package com.amalitech.pulsecheck.model;

import java.time.Instant;

/**
 * Represents a single device "monitor" — the dead man's switch state for one device.
 *
 * <p>A monitor tracks when the last heartbeat was received and how long the device
 * is allowed to go silent (timeoutSeconds) before it is considered DOWN.
 *
 * <p>This class is mutable and is only ever accessed through {@code MonitorService},
 * which synchronizes on each individual Monitor instance to keep heartbeat/pause/expiry
 * checks thread-safe without locking the entire monitor map.
 */
public class Monitor {

    private final String id;
    private final int timeoutSeconds;
    private final String alertEmail;
    private volatile Instant lastHeartbeat;
    private volatile MonitorStatus status;

    public Monitor(String id, int timeoutSeconds, String alertEmail) {
        this.id = id;
        this.timeoutSeconds = timeoutSeconds;
        this.alertEmail = alertEmail;
        this.lastHeartbeat = Instant.now();
        this.status = MonitorStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getAlertEmail() {
        return alertEmail;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public MonitorStatus getStatus() {
        return status;
    }

    public void setStatus(MonitorStatus status) {
        this.status = status;
    }

    /**
     * Seconds remaining before this monitor is considered DOWN.
     * Only meaningful when status == ACTIVE.
     */
    public long secondsRemaining() {
        long elapsed = Instant.now().getEpochSecond() - lastHeartbeat.getEpochSecond();
        return Math.max(0, timeoutSeconds - elapsed);
    }

    /**
     * Whether this monitor has gone silent for longer than its allowed timeout.
     */
    public boolean isExpired() {
        long elapsed = Instant.now().getEpochSecond() - lastHeartbeat.getEpochSecond();
        return elapsed >= timeoutSeconds;
    }
}
