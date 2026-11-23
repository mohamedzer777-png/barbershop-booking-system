package com.barbershop.control;

import com.barbershop.dao.ScheduleDao;
import com.barbershop.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsServiceTest {
    @Test
    void changingDurationIsBlockedWhenFutureBookingsExist() {
        ScheduleDao schedule = mock(ScheduleDao.class);
        when(schedule.getSlotMinutes()).thenReturn(30);
        when(schedule.hasFutureAppointments()).thenReturn(true);
        SettingsService service = new SettingsService(schedule);

        assertThrows(ValidationException.class, () -> service.updateSlotMinutes(45));
        verify(schedule, never()).updateSlotMinutes(anyInt());
    }
}
