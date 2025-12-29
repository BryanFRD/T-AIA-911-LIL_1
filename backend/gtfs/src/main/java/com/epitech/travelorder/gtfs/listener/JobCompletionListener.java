package com.epitech.travelorder.gtfs.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobCompletionListener implements JobExecutionListener {

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Job has finished successfully.");
        } else {
            log.error("Job has failed with status: {}", jobExecution.getStatus());
            jobExecution.getAllFailureExceptions().forEach(exception ->
                    log.error("Exception during job execution: ", exception));
        }
    }

}
