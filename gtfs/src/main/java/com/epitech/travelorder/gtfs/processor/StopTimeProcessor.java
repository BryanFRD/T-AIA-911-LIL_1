package com.epitech.travelorder.gtfs.processor;

import com.epitech.travelorder.common.domain.sncf.StopTime;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import static com.epitech.travelorder.gtfs.util.TimeFormatter.formatStopTime;

@Component
public class StopTimeProcessor implements ItemProcessor<StopTime, StopTime> {

    @Override
    public StopTime process(@NonNull StopTime stopTime) {
        if (isInvalid(stopTime)) {
            return null;
        }

        stopTime.setArrivalTime(formatStopTime(stopTime.getArrivalTime()));
        stopTime.setDepartureTime(formatStopTime(stopTime.getDepartureTime()));

        return stopTime;
    }

    private boolean isInvalid(StopTime stopTime) {
        return stopTime.getTripId() == null || stopTime.getStopId() == null;
    }
}