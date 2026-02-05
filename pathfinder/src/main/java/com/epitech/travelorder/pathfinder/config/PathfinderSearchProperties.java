package com.epitech.travelorder.pathfinder.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "pathfinder.search")
public class PathfinderSearchProperties {

    private int maxResults = 10;
    private int maxTransfers = 3;
    private int searchHorizonDays = 2;
    private int minTransferMinutes = 5;
    private int maxTransferMinutes = 120;
    private double transferPenaltyMinutes = 15.0;
}
