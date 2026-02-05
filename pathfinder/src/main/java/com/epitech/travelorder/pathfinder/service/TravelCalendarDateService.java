package com.epitech.travelorder.pathfinder.service;

import com.epitech.travelorder.common.db.CalendarDate;
import com.epitech.travelorder.common.db.serializable.CalendarDateId;
import com.epitech.travelorder.common.repository.CalendarDateRepository;
import com.epitech.travelorder.common.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelCalendarDateService {

    private final static LogUtils LOG_CTX = LogUtils.of("PATHFINDER", "TravelCalendarDateService");

    private static final DateTimeFormatter GTFS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int EXCEPTION_TYPE_ADDED = 1;
    private static final int EXCEPTION_TYPE_REMOVED = 2;

    private final CalendarDateRepository calendarDateRepository;

    @Cacheable(value = "calendarDates", key = "#serviceId + '_' + #date")
    public boolean canTravelForThisDate(String serviceId, LocalDate date) {
        String gtfsDate = date.format(GTFS_DATE_FORMATTER);

        Optional<CalendarDate> calendarDateOpt = calendarDateRepository.findById(
            new CalendarDateId(serviceId, gtfsDate)
        );

        if (calendarDateOpt.isEmpty()) {
            log.warn(LOG_CTX.format("No calendar date found for service ID %s on date %s", serviceId, gtfsDate));
            return false;
        }

        CalendarDate calendarDate = calendarDateOpt.get();

        return calendarDate.getExceptionType() == EXCEPTION_TYPE_ADDED;
    }
}
