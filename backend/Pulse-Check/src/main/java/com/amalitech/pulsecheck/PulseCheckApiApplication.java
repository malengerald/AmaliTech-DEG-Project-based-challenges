package com.amalitech.pulsecheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulseCheckApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulseCheckApiApplication.class, args);
    }
}
