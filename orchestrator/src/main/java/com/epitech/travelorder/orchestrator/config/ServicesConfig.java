package com.epitech.travelorder.orchestrator.config;

import com.epitech.travelorder.audio.AudioService;
import com.epitech.travelorder.nlu.Gpu;
import com.epitech.travelorder.pathfinder.PathfinderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hexagonal Architecture - Service Configuration
 * This class configures the core services used in the orchestrator module*/
@Configuration
public class ServicesConfig {

    @Bean
    public AudioService audioService() {
        return new AudioService();
    }

    @Bean
    public Gpu gpu() {
        return new Gpu();
    }

    @Bean
    public PathfinderService pathfinderService() {
        return new PathfinderService();
    }
}

