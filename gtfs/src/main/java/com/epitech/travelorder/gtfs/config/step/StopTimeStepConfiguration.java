package com.epitech.travelorder.gtfs.config.step;

import com.epitech.travelorder.common.entity.StopTime;
import com.epitech.travelorder.gtfs.processor.StopTimeProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class StopTimeStepConfiguration {

    private static final int CHUNK_SIZE = 10000;
    private static final int SKIP_LIMIT = 100;

    private static final String INSERT_SQL = """
            INSERT INTO stop_times (trip_id, arrival_time, departure_time, stop_id, stop_sequence, pickup_type, drop_off_type)
            VALUES (:tripId, :arrivalTime, :departureTime, :stopId, :stopSequence, :pickupType, :dropOffType)
            """;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final StopTimeProcessor stopTimeProcessor;

    @Value("${gtfs.local-directory}")
    private String localDirectory;

    @Bean
    public FlatFileItemReader<StopTime> stopTimeReader() {
        return new FlatFileItemReaderBuilder<StopTime>()
                .name("stopTimeReader")
                .resource(new FileSystemResource(localDirectory + "stop_times.txt"))
                .delimited()
                .includedFields(0, 1, 2, 3, 4, 6, 7)
                .names("tripId", "arrivalTime", "departureTime", "stopId", "stopSequence", "pickupType", "dropOffType")
                .targetType(StopTime.class)
                .linesToSkip(1)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<StopTime> stopTimeWriter() {
        return new JdbcBatchItemWriterBuilder<StopTime>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(INSERT_SQL)
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Step importStopTimesStep() {
        return new StepBuilder("importStopTimesStep", jobRepository)
                .<StopTime, StopTime>chunk(CHUNK_SIZE, transactionManager)
                .reader(stopTimeReader())
                .processor(stopTimeProcessor)
                .writer(stopTimeWriter())
                .faultTolerant()
                .skipLimit(SKIP_LIMIT)
                .skip(Exception.class)
                .build();
    }
}