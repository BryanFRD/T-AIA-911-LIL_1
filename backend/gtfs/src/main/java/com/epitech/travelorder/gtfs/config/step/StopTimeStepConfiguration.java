package com.epitech.travelorder.gtfs.config.step;

import com.epitech.travelorder.common.db.StopTime;
import com.epitech.travelorder.common.repository.StopTimeRepository;
import com.epitech.travelorder.gtfs.processor.StopTimeProcessor;
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
public class StopTimeStepConfiguration {

    private static final int CHUNK_SIZE = 10000;
    private static final int SKIP_LIMIT = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final StopTimeRepository stopTimeRepository;
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
    public RepositoryItemWriter<StopTime> stopTimeWriter() {
        return new RepositoryItemWriterBuilder<StopTime>()
                .repository(stopTimeRepository)
                .methodName("save")
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

