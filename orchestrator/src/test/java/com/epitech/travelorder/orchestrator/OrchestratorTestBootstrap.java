package com.epitech.travelorder.orchestrator;

import com.epitech.travelorder.orchestrator.configuration.CucumberTestConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ExtendWith({SpringExtension.class})
@ActiveProfiles("test")
@EnableAutoConfiguration
@CucumberContextConfiguration
@SpringBootTest(classes = CucumberTestConfiguration.class, webEnvironment = RANDOM_PORT)
@Import(CucumberTestConfiguration.class)
@EnableJpaRepositories(basePackages = {"com.epitech.travelorder.common.repository"})
@EntityScan(basePackages = "com.epitech.travelorder.common.db")
@WireMockTest
@Testcontainers
public class OrchestratorTestBootstrap {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("travelorder_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

}
