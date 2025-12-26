package com.epitech.travelorder.common.entity.serializable;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class CalendarDateId implements Serializable {

    private String serviceId;

    private String date;
}
