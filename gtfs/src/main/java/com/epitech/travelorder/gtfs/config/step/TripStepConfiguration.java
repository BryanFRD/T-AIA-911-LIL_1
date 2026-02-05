package com.epitech.travelorder.gtfs.config.step;

import com.epitech.travelorder.common.db.Trip;
import com.epitech.travelorder.common.repository.TripRepository;
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
public class TripStepConfiguration {

    private static final int CHUNK_SIZE = 5000;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TripRepository tripRepository;

    @Value("${gtfs.local-directory}")
    private String localDirectory;

    @Bean
    public FlatFileItemReader<Trip> tripReader() {
        return new FlatFileItemReaderBuilder<Trip>()
                .name("tripReader")
                .resource(new FileSystemResource(localDirectory + "trips.txt"))
                .delimited()
                .includedFields(0, 1, 2, 3, 4, 5)
                .names("routeId", "serviceId", "tripId", "tripHeadsign", "directionId", "blockId")
                .targetType(Trip.class)
                .linesToSkip(1)
                .build();
    }

    @Bean
    public RepositoryItemWriter<Trip> tripWriter() {
        return new RepositoryItemWriterBuilder<Trip>()
                .repository(tripRepository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step importTripsStep() {
        return new StepBuilder("importTripsStep", jobRepository)
                .<Trip, Trip>chunk(CHUNK_SIZE, transactionManager)
                .reader(tripReader())
                .writer(tripWriter())
                .build();
    }
}

