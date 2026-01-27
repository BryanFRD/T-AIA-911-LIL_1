package com.epitech.travelorder.gtfs.config.step;

import com.epitech.travelorder.common.db.Stop;
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
public class StopStepConfiguration {

    private static final int CHUNK_SIZE = 2000;

    private static final String INSERT_SQL = """
            INSERT INTO stops (stop_id, stop_name, stop_lat, stop_lon, zone_id, location_type, parent_station)
            VALUES (:stopId, :stopName, :stopLat, :stopLon, :zoneId, :locationType, :parentStation)
            """;

    private final JobRepository jobRepository;

    private final PlatformTransactionManager transactionManager;

    private final DataSource dataSource;

    @Value("${gtfs.local-directory}")
    private String localDirectory;

    @Bean
    public FlatFileItemReader<Stop> stopReader() {
        return new FlatFileItemReaderBuilder<Stop>()
                .name("stopReader")
                .resource(new FileSystemResource(localDirectory + "stops.txt"))
                .delimited()
                .includedFields(0, 1, 3, 4, 5, 7, 8)
                .names("stopId", "stopName", "stopLat", "stopLon", "zoneId", "locationType", "parentStation")
                .targetType(Stop.class)
                .linesToSkip(1)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Stop> stopWriter() {
        return new JdbcBatchItemWriterBuilder<Stop>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(INSERT_SQL)
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Step importStopsStep() {
        return new StepBuilder("importStopsStep", jobRepository)
                .<Stop, Stop>chunk(CHUNK_SIZE, transactionManager)
                .reader(stopReader())
                .writer(stopWriter())
                .build();
    }
}