package com.amalitech.pulsecheck.dto;

import com.amalitech.pulsecheck.model.Monitor;
import com.amalitech.pulsecheck.model.MonitorStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body describing a monitor's current state.
 * Returned by register, heartbeat, pause, and get-status endpoints.
 */
public class MonitorResponse {

    private final String id;
    private final MonitorStatus status;
    private final int timeout;

    @JsonProperty("seconds_remaining")
    private final long secondsRemaining;

    private final String message;

    public MonitorResponse(Monitor monitor, String message) {
        this.id = monitor.getId();
        this.status = monitor.getStatus();
        this.timeout = monitor.getTimeoutSeconds();
        this.secondsRemaining = monitor.getStatus() == MonitorStatus.ACTIVE
                ? monitor.secondsRemaining()
                : 0;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public MonitorStatus getStatus() {
        return status;
    }

    public int getTimeout() {
        return timeout;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }

    public String getMessage() {
        return message;
    }
}
