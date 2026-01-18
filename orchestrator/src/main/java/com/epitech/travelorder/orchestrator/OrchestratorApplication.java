package com.epitech.travelorder.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.epitech.travelorder.orchestrator",
        "com.epitech.travelorder.pathfinder",
        "com.epitech.travelorder.nlu",
        "com.epitech.travelorder.audio"
})
@EnableJpaRepositories(basePackages = {
        "com.epitech.travelorder.pathfinder.repository",
})
@EntityScan(basePackages = {
        "com.epitech.travelorder.common.domain.sncf"
})
public class OrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }
}
