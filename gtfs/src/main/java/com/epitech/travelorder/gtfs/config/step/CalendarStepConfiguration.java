package com.epitech.travelorder.gtfs.config.step;

import com.epitech.travelorder.common.db.CalendarDate;
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
public class CalendarStepConfiguration {

    private static final int CHUNK_SIZE = 2000;

    private static final String INSERT_SQL = """
            INSERT INTO calendar_dates (service_id, date, exception_type)
            VALUES (:serviceId, :date, :exceptionType)
            """;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Value("${gtfs.local-directory}")
    private String localDirectory;

    @Bean
    public FlatFileItemReader<CalendarDate> calendarReader() {
        return new FlatFileItemReaderBuilder<CalendarDate>()
                .name("calendarReader")
                .resource(new FileSystemResource(localDirectory + "calendar_dates.txt"))
                .delimited()
                .includedFields(0, 1, 2)
                .names("serviceId", "date", "exceptionType")
                .targetType(CalendarDate.class)
                .linesToSkip(1)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<CalendarDate> calendarWriter() {
        return new JdbcBatchItemWriterBuilder<CalendarDate>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(INSERT_SQL)
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public Step importCalendarDatesStep() {
        return new StepBuilder("importCalendarDatesStep", jobRepository)
                .<CalendarDate, CalendarDate>chunk(CHUNK_SIZE, transactionManager)
                .reader(calendarReader())
                .writer(calendarWriter())
                .build();
    }
}