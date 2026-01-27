package com.epitech.travelorder.gtfs.processor;

import com.epitech.travelorder.common.db.StopTime;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static com.epitech.travelorder.gtfs.util.TimeFormatterUtils.formatStopTime;

@Component
public class StopTimeProcessor implements ItemProcessor<StopTime, StopTime> {

    @Override
    public StopTime process(@NonNull StopTime stopTime) {
        if (isStopTimeInvalid(stopTime)) {
            return null;
        }

        stopTime.setArrivalTime(formatStopTime(stopTime.getArrivalTime()));
        stopTime.setDepartureTime(formatStopTime(stopTime.getDepartureTime()));

        return stopTime;
    }

    private boolean isStopTimeInvalid(StopTime stopTime) {
        return stopTime.getTripId() == null || stopTime.getStopId() == null;
    }
}