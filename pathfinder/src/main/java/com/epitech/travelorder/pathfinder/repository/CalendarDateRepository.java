package com.epitech.travelorder.pathfinder.repository;

import com.epitech.travelorder.common.domain.sncf.CalendarDate;
import com.epitech.travelorder.common.domain.sncf.serializable.CalendarDateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarDateRepository extends JpaRepository<CalendarDate, CalendarDateId> {

}
