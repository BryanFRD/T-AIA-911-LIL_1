package com.epitech.travelorder.gtfs.tasklet;

import com.epitech.travelorder.common.util.LogUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import static com.epitech.travelorder.common.util.LogUtils.GTFS;

@Slf4j
@Component
@RequiredArgsConstructor
public class TruncateDatabaseTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    private final LogUtils LOG_CTX = LogUtils.of(GTFS, "TruncateDatabaseTasklet");

    @Override
    public RepeatStatus execute(@NonNull final StepContribution contribution, @NonNull final ChunkContext chunkContext) {
        log.info(LOG_CTX.format("Truncating GTFS tables in the database"));

        try {
            DataSource dataSource = jdbcTemplate.getDataSource();
            if (dataSource == null) {
                log.error(LOG_CTX.format("DataSource is null, cannot truncate tables"));
                return RepeatStatus.FINISHED;
            }

            String databaseProductName = dataSource.getConnection().getMetaData().getDatabaseProductName();
            boolean isPostgreSQL = "postgresql".equalsIgnoreCase(databaseProductName);

            if (isPostgreSQL) {
                log.debug(LOG_CTX.format("Using PostgreSQL TRUNCATE CASCADE syntax"));
                jdbcTemplate.execute("TRUNCATE TABLE stop_times, trips, calendar_dates, transfers, routes, stops CASCADE");
            } else {
                log.debug(LOG_CTX.format("Using H2-compatible DELETE FROM syntax"));
                jdbcTemplate.execute("DELETE FROM stop_times");
                jdbcTemplate.execute("DELETE FROM trips");
                jdbcTemplate.execute("DELETE FROM calendar_dates");
                jdbcTemplate.execute("DELETE FROM transfers");
                jdbcTemplate.execute("DELETE FROM routes");
                jdbcTemplate.execute("DELETE FROM stops");
            }

            log.info(LOG_CTX.format("Successfully truncated GTFS tables"));
        } catch (Exception e) {
            log.warn(LOG_CTX.format("Could not truncate tables (maybe they don't exist yet): %s", e.getMessage()));
        }

        return RepeatStatus.FINISHED;
    }
}