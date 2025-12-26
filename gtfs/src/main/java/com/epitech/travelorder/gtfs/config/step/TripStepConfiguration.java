package com.epitech.travelorder.gtfs.config.step;

import com.epitech.travelorder.common.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class TripStepConfiguration {

    private static final int CHUNK_SIZE = 5000;

    private static final String INSERT_SQL = """
            INSERT INTO trips (trip_id, route_id, service_id, trip_headsign, direction_id, block_id)
            VALUES (:tripId, :routeId, :serviceId, :tripHeadsign, :directionId, :blockId)
            """;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Value("${gtfs.local-directory}")
    private String localDirectory;

    @Bean
    public FlatFileItemReader<Trip> tripReader() {
        BeanWrapperFieldSetMapper<Trip> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(Trip.class);
        fieldSetMapper.setDistanceLimit(0);

        return new FlatFileItemReaderBuilder<Trip>()
                .name("tripReader")
                .resource(new FileSystemResource(localDirectory + "trips.txt"))
                .delimited()
                .includedFields(0, 1, 2, 3, 4, 5)
                .names("routeId", "serviceId", "tripId", "tripHeadsign", "directionId", "blockId")
                .fieldSetMapper(fieldSetMapper)
                .linesToSkip(1)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Trip> tripWriter() {
        return new JdbcBatchItemWriterBuilder<Trip>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(INSERT_SQL)
                .dataSource(dataSource)
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