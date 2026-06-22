package com.amalitech.pulsecheck.exception;

public class MonitorAlreadyExistsException extends RuntimeException {

    public MonitorAlreadyExistsException(String monitorId) {
        super("Monitor already exists: " + monitorId);
    }
}
