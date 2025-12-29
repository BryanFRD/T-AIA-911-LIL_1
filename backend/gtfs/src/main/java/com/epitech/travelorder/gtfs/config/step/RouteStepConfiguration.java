package com.epitech.travelorder.gtfs.config.step;

import com.epitech.travelorder.common.entity.Route;
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
public class RouteStepConfiguration {

    private static final int CHUNK_SIZE = 2000;

    private static final String INSERT_SQL = """
            INSERT INTO routes (route_id, route_short_name, route_long_name, route_type, route_color, route_text_color)
            VALUES (:routeId, :routeShortName, :routeLongName, :routeType, :routeColor, :routeTextColor)
            """;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

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
    public JdbcBatchItemWriter<Route> routeWriter() {
        return new JdbcBatchItemWriterBuilder<Route>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(INSERT_SQL)
                .dataSource(dataSource)
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