package com.amalitech.pulsecheck.controller;

import com.amalitech.pulsecheck.dto.MonitorResponse;
import com.amalitech.pulsecheck.dto.RegisterMonitorRequest;
import com.amalitech.pulsecheck.model.Monitor;
import com.amalitech.pulsecheck.service.MonitorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monitors")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    /**
     * User Story 1: Register a new monitor.
     * POST /monitors
     * Body: {"id": "device-123", "timeout": 60, "alert_email": "admin@critmon.com"}
     */
    @PostMapping
    public ResponseEntity<MonitorResponse> register(@Valid @RequestBody RegisterMonitorRequest request) {
        Monitor monitor = monitorService.register(
                request.getId(),
                request.getTimeout(),
                request.getAlertEmail()
        );
        MonitorResponse body = new MonitorResponse(monitor, "Monitor registered successfully.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * User Story 2: Heartbeat - reset the countdown for a device.
     * POST /monitors/{id}/heartbeat
     */
    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<MonitorResponse> heartbeat(@PathVariable String id) {
        Monitor monitor = monitorService.heartbeat(id);
        MonitorResponse body = new MonitorResponse(monitor, "Heartbeat received. Timer reset.");
        return ResponseEntity.ok(body);
    }

    /**
     * Bonus User Story: Pause monitoring for a device.
     * POST /monitors/{id}/pause
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<MonitorResponse> pause(@PathVariable String id) {
        Monitor monitor = monitorService.pause(id);
        MonitorResponse body = new MonitorResponse(monitor, "Monitor paused. No alerts will fire until next heartbeat.");
        return ResponseEntity.ok(body);
    }

    /**
     * Developer's Choice: on-demand status lookup for a single monitor.
     * GET /monitors/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<MonitorResponse> getStatus(@PathVariable String id) {
        Monitor monitor = monitorService.getStatus(id);
        MonitorResponse body = new MonitorResponse(monitor, "Current monitor status.");
        return ResponseEntity.ok(body);
    }
}
