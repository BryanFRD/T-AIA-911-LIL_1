package com.epitech.travelorder.gtfs.config.step;

import com.epitech.travelorder.common.db.Route;
import com.epitech.travelorder.common.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class RouteStepConfiguration {

    private static final int CHUNK_SIZE = 2000;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RouteRepository routeRepository;

    @Value("${gtfs.local-directory}")
    private String localDirectory;

    @Bean
    public FlatFileItemReader<Route> routeReader() {
        return new FlatFileItemReaderBuilder<Route>()
                .name("routeReader")
                .resource(new FileSystemResource(localDirectory + "routes.txt"))
                .delimited()
                .includedFields(0, 2, 3, 5, 7, 8)
                .names("routeId", "routeShortName", "routeLongName", "routeType", "routeColor", "routeTextColor")
                .targetType(Route.class)
                .linesToSkip(1)
                .build();
    }

    @Bean
    public RepositoryItemWriter<Route> routeWriter() {
        return new RepositoryItemWriterBuilder<Route>()
                .repository(routeRepository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step importRoutesStep() {
        return new StepBuilder("importRoutesStep", jobRepository)
                .<Route, Route>chunk(CHUNK_SIZE, transactionManager)
                .reader(routeReader())
                .writer(routeWriter())
                .build();
    }
}

