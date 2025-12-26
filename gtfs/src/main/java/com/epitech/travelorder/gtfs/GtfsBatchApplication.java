package com.epitech.travelorder.gtfs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = {"com.epitech.travelorder.gtfs"})
@EntityScan(basePackages = "com.epitech.travelorder.common.entity")
public class GtfsBatchApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(GtfsBatchApplication.class, args)));
    }
}
