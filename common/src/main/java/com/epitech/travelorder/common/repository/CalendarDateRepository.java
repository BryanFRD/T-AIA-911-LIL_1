package com.epitech.travelorder.common.repository;

import com.epitech.travelorder.common.db.CalendarDate;
import com.epitech.travelorder.common.db.serializable.CalendarDateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarDateRepository extends JpaRepository<CalendarDate, CalendarDateId> {

}
