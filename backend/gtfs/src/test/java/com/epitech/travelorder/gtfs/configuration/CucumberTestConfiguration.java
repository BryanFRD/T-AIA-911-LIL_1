package com.epitech.travelorder.gtfs.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CucumberTestConfiguration {

    @Value("${wiremock.server.port}")
    private int wiremockPort;
}
