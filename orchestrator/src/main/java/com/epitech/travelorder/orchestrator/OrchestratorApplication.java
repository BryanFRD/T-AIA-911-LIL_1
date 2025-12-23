package com.epitech.travelorder.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.epitech.travelorder")
public class OrchestratorApplication {

    static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }
}

