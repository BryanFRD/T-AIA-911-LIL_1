package com.epitech.travelorder.gtfs.tasklet;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TruncateDatabaseTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public RepeatStatus execute(@NonNull final StepContribution contribution, @NonNull final ChunkContext chunkContext) {
        log.info("Truncating GTFS tables before import");

        try {
            jdbcTemplate.execute("TRUNCATE TABLE stop_times CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE trips CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE routes CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE calendar_dates CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE stops CASCADE");
            jdbcTemplate.execute("TRUNCATE TABLE transfers CASCADE");
            log.info("GTFS tables truncated successfully");
        } catch (Exception e) {
            log.warn("Could not truncate tables (may not exist yet): {}", e.getMessage());
        }

        return RepeatStatus.FINISHED;
    }
}