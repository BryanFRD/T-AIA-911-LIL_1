package com.epitech.travelorder.common.domain.gtfs;

import com.epitech.travelorder.common.domain.gtfs.serializable.CalendarDateId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Entity
@Table(name = "calendar_dates")
@IdClass(CalendarDateId.class)
@Getter
@Setter
public class CalendarDate {

    @Id
    @Column(name = "service_id")
    private String serviceId;

    @Id
    @Column(name = "date")
    private String date;

    @Column(name = "exception_type")
    private Integer exceptionType;
}
