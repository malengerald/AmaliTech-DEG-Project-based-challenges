package com.amalitech.pulsecheck.exception;

public class MonitorNotFoundException extends RuntimeException {

    public MonitorNotFoundException(String monitorId) {
        super("Monitor not found: " + monitorId);
    }
}
