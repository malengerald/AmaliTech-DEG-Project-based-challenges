package com.amalitech.pulsecheck.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /monitors
 * Example: {"id": "device-123", "timeout": 60, "alert_email": "admin@critmon.com"}
 */
public class RegisterMonitorRequest {

    @NotBlank(message = "id is required")
    private String id;

    @Min(value = 1, message = "timeout must be a positive number of seconds")
    private int timeout;

    @Email(message = "alert_email must be a valid email address")
    @NotBlank(message = "alert_email is required")
    @JsonProperty("alert_email")
    private String alertEmail;

    public RegisterMonitorRequest() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getAlertEmail() {
        return alertEmail;
    }

    public void setAlertEmail(String alertEmail) {
        this.alertEmail = alertEmail;
    }
}
