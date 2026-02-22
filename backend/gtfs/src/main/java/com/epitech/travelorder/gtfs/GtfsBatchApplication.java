package com.epitech.travelorder.gtfs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.epitech.travelorder.gtfs"})
@EnableJpaRepositories(basePackages = {"com.epitech.travelorder.common.repository"})
@EntityScan(basePackages = "com.epitech.travelorder.common.db")
public class GtfsBatchApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(GtfsBatchApplication.class, args)));
    }
}
