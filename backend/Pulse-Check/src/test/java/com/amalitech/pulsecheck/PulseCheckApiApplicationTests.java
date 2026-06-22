package com.amalitech.pulsecheck;

import com.amalitech.pulsecheck.dto.RegisterMonitorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PulseCheckApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registeringNewMonitorReturns201() throws Exception {
        mockMvc.perform(post("/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"device-1\",\"timeout\":60,\"alert_email\":\"a@b.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("device-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void registeringDuplicateIdReturns409() throws Exception {
        mockMvc.perform(post("/monitors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"device-2\",\"timeout\":60,\"alert_email\":\"a@b.com\"}"));

        mockMvc.perform(post("/monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"device-2\",\"timeout\":60,\"alert_email\":\"a@b.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void heartbeatOnUnknownDeviceReturns404() throws Exception {
        mockMvc.perform(post("/monitors/does-not-exist/heartbeat"))
                .andExpect(status().isNotFound());
    }

    @Test
    void heartbeatResetsTimerForKnownDevice() throws Exception {
        mockMvc.perform(post("/monitors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"device-3\",\"timeout\":60,\"alert_email\":\"a@b.com\"}"));

        mockMvc.perform(post("/monitors/device-3/heartbeat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void pausingMonitorStopsAlerting() throws Exception {
        mockMvc.perform(post("/monitors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"device-4\",\"timeout\":60,\"alert_email\":\"a@b.com\"}"));

        mockMvc.perform(post("/monitors/device-4/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void deviceGoesDownAfterTimeoutElapses() throws Exception {
        mockMvc.perform(post("/monitors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"device-5\",\"timeout\":1,\"alert_email\":\"a@b.com\"}"));

        Thread.sleep(2500);

        mockMvc.perform(get("/monitors/device-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void heartbeatUnpausesMonitor() throws Exception {
        mockMvc.perform(post("/monitors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"device-6\",\"timeout\":60,\"alert_email\":\"a@b.com\"}"));

        mockMvc.perform(post("/monitors/device-6/pause"));

        mockMvc.perform(post("/monitors/device-6/heartbeat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
