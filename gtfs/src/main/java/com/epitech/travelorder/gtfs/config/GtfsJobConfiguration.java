package com.epitech.travelorder.gtfs.config;

import com.epitech.travelorder.gtfs.listener.JobCompletionListener;
import com.epitech.travelorder.gtfs.tasklet.DownloadTasklet;
import com.epitech.travelorder.gtfs.tasklet.TruncateDatabaseTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class GtfsJobConfiguration {

    private final JobRepository jobRepository;

    private final PlatformTransactionManager transactionManager;

    private final JobCompletionListener jobCompletionListener;

    private final DownloadTasklet downloadTasklet;

    private final TruncateDatabaseTasklet truncateDatabaseTasklet;

    @Bean
    public Job importGtfsJob(Step importStopsStep,
                             Step importRoutesStep,
                             Step importCalendarDatesStep,
                             Step importTripsStep,
                             Step importStopTimesStep) {
        return new JobBuilder("importGtfsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(jobCompletionListener)
                .start(downloadStep())
                .next(truncateStep())
                .next(importStopsStep)
                .next(importRoutesStep)
                .next(importCalendarDatesStep)
                .next(importTripsStep)
                .next(importStopTimesStep)
                .build();
    }

    @Bean
    public Step downloadStep() {
        return new StepBuilder("downloadStep", jobRepository)
                .tasklet(downloadTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step truncateStep() {
        return new StepBuilder("truncateStep", jobRepository)
                .tasklet(truncateDatabaseTasklet, transactionManager)
                .build();
    }
}
