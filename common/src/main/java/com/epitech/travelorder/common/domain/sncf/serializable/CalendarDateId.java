package com.epitech.travelorder.common.domain.sncf.serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public class CalendarDateId implements Serializable {

    private String serviceId;

    private String date;
}
