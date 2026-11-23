package com.barbershop.control;

import com.barbershop.dao.BookingDao;
import com.barbershop.dao.BookingDao.AppointmentDetail;
import com.barbershop.dao.NotificationDao;
import com.barbershop.exception.SlotUnavailableException;
import com.barbershop.exception.ValidationException;
import com.barbershop.util.Validation;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** שכבת ה-Control של הלקוח: קביעה, צפייה, ביטול, וקריאת הודעות. */
@Service
public class BookingService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final AvailabilityService availabilityService;
    private final BookingDao bookingDao;
    private final NotificationDao notificationDao;
    private final VerificationService verificationService;

    public BookingService(AvailabilityService availabilityService, BookingDao bookingDao,
                          NotificationDao notificationDao, VerificationService verificationService) {
        this.availabilityService = availabilityService;
        this.bookingDao = bookingDao;
        this.notificationDao = notificationDao;
        this.verificationService = verificationService;
    }

    public record BookingResult(long id, String phone, String date, String time) {}
    public record MyAppointment(long id, String date, String time) {}
    public record Note(long id, String message) {}

    /** קביעת תור — דורשת קוד אימות תקף שנשלח לטלפון */
    public BookingResult book(String phone, String firstName, String lastName,
                              LocalDate date, LocalTime time, String code) {
        String normPhone = Validation.requirePhone(phone);
        String fName = Validation.requireName(firstName, "שם פרטי");
        String lName = Validation.requireName(lastName, "שם משפחה");

        // אימות בעלות על הטלפון לפני כל פעולה
        verificationService.verify(normPhone, code);

        if (date.isBefore(LocalDate.now())) {
            throw new ValidationException("לא ניתן לקבוע תור בתאריך שעבר");
        }
        if (!availabilityService.availableSlots(date).contains(time)) {
            throw new SlotUnavailableException("השעה שנבחרה אינה פנויה לקביעה");
        }

        if (!bookingDao.customerExists(normPhone)) {
            bookingDao.insertCustomer(normPhone, fName, lName);
        }
        long id = bookingDao.insertAppointment(normPhone, date, time);
        return new BookingResult(id, normPhone, date.toString(), time.toString());
    }

    /** צפייה בתורים — דורשת קוד אימות תקף (כדי שלא כל אחד יראה תורים של אחר) */
    public List<MyAppointment> getUpcomingVerified(String phone, String code) {
        String normPhone = Validation.requirePhone(phone);
        verificationService.verify(normPhone, code);
        return upcomingFor(normPhone);
    }

    private List<MyAppointment> upcomingFor(String normPhone) {
        LocalDateTime now = LocalDateTime.now();
        return bookingDao.findUpcomingByPhone(normPhone).stream()
                .filter(a -> LocalDateTime.of(a.date(), a.time()).isAfter(now))
                .map(a -> new MyAppointment(a.id(), a.date().toString(), HHMM.format(a.time())))
                .toList();
    }

    /** ביטול — דורש קוד אימות תקף + כלל השעתיים */
    public void cancel(long id, String phone, String code) {
        String normPhone = Validation.requirePhone(phone);
        verificationService.verify(normPhone, code);

        AppointmentDetail appt = bookingDao.findByIdAndPhone(id, normPhone);
        if (appt == null) {
            throw new SlotUnavailableException("התור לא נמצא");
        }
        if (!"BOOKED".equals(appt.status())) {
            throw new SlotUnavailableException("התור כבר בוטל");
        }
        LocalDateTime apptTime = LocalDateTime.of(appt.date(), appt.time());
        if (apptTime.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new SlotUnavailableException("לא ניתן לבטל תור פחות משעתיים לפני מועדו");
        }
        int rows = bookingDao.cancel(id, normPhone);
        if (rows == 0) {
            throw new SlotUnavailableException("הביטול נכשל, נסו שוב");
        }
    }

    /** ההודעות שטרם נקראו עבור הלקוח (נטען יחד עם התורים, אחרי אימות) */
    public List<Note> getNotifications(String phone) {
        String normPhone = Validation.requirePhone(phone);
        return notificationDao.findUnread(normPhone).stream()
                .map(n -> new Note(n.id(), n.message()))
                .toList();
    }

    public void markNotificationRead(long id, String phone, String code) {
        String normPhone = Validation.requirePhone(phone);
        verificationService.verify(normPhone, code);
        notificationDao.markRead(id, normPhone);
    }
}
