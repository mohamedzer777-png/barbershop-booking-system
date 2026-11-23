package com.barbershop.control;

import com.barbershop.dao.ScheduleDao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AvailabilityServiceTest {

    @Test
    void closedDayHasNoSlots() {
        ScheduleDao dao = mock(ScheduleDao.class);
        LocalDate date = LocalDate.now().plusDays(3);
        when(dao.getWorkingHours(date.getDayOfWeek().getValue() % 7))
                .thenReturn(new ScheduleDao.WorkingHours(null, null, false));
        assertTrue(new AvailabilityService(dao).availableSlots(date).isEmpty());
    }

    @Test
    void bookedAndBlockedIntervalsAreExcluded() {
        ScheduleDao dao = mock(ScheduleDao.class);
        LocalDate date = LocalDate.now().plusDays(2);
        when(dao.getWorkingHours(date.getDayOfWeek().getValue() % 7)).thenReturn(
                new ScheduleDao.WorkingHours(LocalTime.of(9, 0), LocalTime.of(11, 0), true));
        when(dao.getSlotMinutes()).thenReturn(30);
        when(dao.getBookedTimes(date)).thenReturn(Set.of(LocalTime.of(9, 30)));
        when(dao.getBlocks(date)).thenReturn(List.of(
                new ScheduleDao.Block(LocalTime.of(10, 0), LocalTime.of(10, 30))));
        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(10, 30)),
                new AvailabilityService(dao).availableSlots(date));
    }
}
