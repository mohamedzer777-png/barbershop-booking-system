package com.barbershop.control;

import com.barbershop.dao.BookingDao;
import com.barbershop.dao.NotificationDao;
import com.barbershop.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {
    private AvailabilityService availability;
    private BookingDao bookings;
    private NotificationDao notifications;
    private VerificationService verification;
    private BookingService service;

    @BeforeEach
    void setUp() {
        availability = mock(AvailabilityService.class);
        bookings = mock(BookingDao.class);
        notifications = mock(NotificationDao.class);
        verification = mock(VerificationService.class);
        service = new BookingService(availability, bookings, notifications, verification);
    }

    @Test
    void bookRequiresVerificationAndCreatesAppointment() {
        LocalDate date = LocalDate.now().plusDays(3);
        LocalTime time = LocalTime.of(11, 0);
        when(availability.availableSlots(date)).thenReturn(List.of(time));
        when(bookings.customerExists("0501234567")).thenReturn(true);
        when(bookings.insertAppointment("0501234567", date, time)).thenReturn(42L);

        BookingService.BookingResult result = service.book("050-123-4567", "Dana", "Levi", date, time, "123456");

        assertEquals(42L, result.id());
        verify(verification).verify("0501234567", "123456");
        verify(bookings).insertAppointment("0501234567", date, time);
    }

    @Test
    void invalidOtpBlocksBookingBeforeAnyDatabaseWrites() {
        doThrow(new ValidationException("invalid")).when(verification).verify("0501234567", "000000");
        LocalDate date = LocalDate.now().plusDays(1);
        assertThrows(ValidationException.class, () ->
                service.book("0501234567", "Dana", "Levi", date, LocalTime.of(11, 0), "000000"));
        verifyNoInteractions(bookings);
    }

    @Test
    void viewingAppointmentsRequiresOtp() {
        when(bookings.findUpcomingByPhone("0501234567")).thenReturn(List.of());
        service.getUpcomingVerified("0501234567", "123456");
        verify(verification).verify("0501234567", "123456");
        verify(bookings).findUpcomingByPhone("0501234567");
    }

    @Test
    void cancellationCannotReadAnotherCustomersAppointmentWithoutOtp() {
        doThrow(new ValidationException("invalid")).when(verification).verify("0501234567", "000000");
        assertThrows(ValidationException.class, () -> service.cancel(99L, "0501234567", "000000"));
        verifyNoInteractions(bookings);
    }

    @Test
    void markingNotificationReadChecksOtpAndPhoneOwnership() {
        service.markNotificationRead(4L, "0501234567", "123456");
        verify(verification).verify("0501234567", "123456");
        verify(notifications).markRead(4L, "0501234567");
    }
}
