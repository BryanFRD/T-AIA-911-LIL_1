package com.epitech.travelorder.gtfs.listener;

import com.epitech.travelorder.common.infrastructure.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobCompletionListener implements JobExecutionListener {

    private final Log LOG_CTX = Log.of(Log.GTFS, "JobCompletionListener");

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info(LOG_CTX.format("Job has completed successfully."));
        } else {
            log.error(LOG_CTX.format("Job has failed with status: %s", jobExecution.getStatus()));
            jobExecution.getAllFailureExceptions().forEach(exception ->
                    log.error("Exception during job execution: ", exception));
        }
    }

}
