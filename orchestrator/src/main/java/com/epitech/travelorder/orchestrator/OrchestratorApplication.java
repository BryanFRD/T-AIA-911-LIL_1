package com.epitech.travelorder.orchestrator;

import com.epitech.travelorder.nlu.Gpu;
import com.epitech.travelorder.orchestrator.config.ServicesConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrchestratorApplication {

    static void main(String[] args) {
        Gpu gpu = new ServicesConfig().gpu();
        gpu.check();

        SpringApplication.run(OrchestratorApplication.class, args);
    }
}

