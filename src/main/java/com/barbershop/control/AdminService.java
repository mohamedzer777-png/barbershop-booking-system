package com.barbershop.control;

import com.barbershop.dao.AdminDao;
import com.barbershop.dao.AdminDao.AppointmentRef;
import com.barbershop.dao.BookingDao;
import com.barbershop.dao.NotificationDao;
import com.barbershop.exception.SlotUnavailableException;
import com.barbershop.util.Validation;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** שכבת ה-Control של המנהל. בהזזה/ביטול נשלחת הודעה ללקוח. */
@Service
public class AdminService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final AdminDao adminDao;
    private final BookingDao bookingDao;
    private final NotificationDao notificationDao;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${app.admin.password:}")
    private String configuredAdminPassword;

    public AdminService(AdminDao adminDao, BookingDao bookingDao, NotificationDao notificationDao) {
        this.adminDao = adminDao;
        this.bookingDao = bookingDao;
        this.notificationDao = notificationDao;
    }

    public record CalendarEntry(long id, String time, String name, String phone) {}
    public record BlockView(long id, String label, String reason) {}

    public boolean checkLogin(String username, String rawPassword) {
        if (username == null || rawPassword == null || configuredAdminPassword == null || configuredAdminPassword.isBlank()) return false;
        String hash = adminDao.findPasswordHash(username);
        if (hash == null) return false;
        return encoder.matches(rawPassword, hash);
    }

    public List<CalendarEntry> getAppointments(LocalDate date) {
        return adminDao.findBookedByDate(date).stream()
                .map(a -> new CalendarEntry(a.id(), HHMM.format(a.time()),
                        a.firstName() + " " + a.lastName(), a.phone()))
                .toList();
    }

    // ---------- חסימות ----------
    public void addBlock(String dateStr, String startStr, String endStr, String reason) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalTime start = (startStr == null || startStr.isBlank()) ? null : LocalTime.parse(startStr);
        LocalTime end   = (endStr == null || endStr.isBlank())     ? null : LocalTime.parse(endStr);
        if (start != null && (end == null || !start.isBefore(end))) {
            throw new SlotUnavailableException("שעת ההתחלה חייבת להיות לפני שעת הסיום");
        }
        adminDao.insertBlock(date, start, end, reason);
    }

    public List<BlockView> getBlocks(LocalDate date) {
        return adminDao.findBlocksByDate(date).stream()
                .map(b -> {
                    String label = (b.start() == null) ? "כל היום"
                            : HHMM.format(b.start()) + "–" + (b.end() == null ? "" : HHMM.format(b.end()));
                    return new BlockView(b.id(), label, b.reason() == null ? "" : b.reason());
                })
                .toList();
    }

    public void removeBlock(long id) {
        adminDao.deleteBlock(id);
    }

    // ---------- שליטה ידנית בתורים ----------
    public void adminBook(String phone, String firstName, String lastName, String dateStr, String timeStr) {
        String normPhone = Validation.requirePhone(phone);
        String fName = Validation.requireName(firstName, "שם פרטי");
        String lName = Validation.requireName(lastName, "שם משפחה");
        LocalDate date = LocalDate.parse(dateStr);
        LocalTime time = LocalTime.parse(timeStr);
        if (!bookingDao.customerExists(normPhone)) {
            bookingDao.insertCustomer(normPhone, fName, lName);
        }
        bookingDao.insertAppointment(normPhone, date, time);
    }

    /** הזזת תור + הודעה ללקוח. אם ההזזה נכשלת (שעה תפוסה) — לא נשלחת הודעה. */
    public void moveAppointment(long id, String dateStr, String timeStr) {
        AppointmentRef before = adminDao.findById(id);
        LocalDate newDate = LocalDate.parse(dateStr);
        LocalTime newTime = LocalTime.parse(timeStr);

        adminDao.move(id, newDate, newTime);   // זורק SlotTakenException אם תפוס

        if (before != null) {
            String msg = "התור שלך הוזז על ידי המספרה מ-" +
                    before.date() + " בשעה " + HHMM.format(before.time()) +
                    " ל-" + newDate + " בשעה " + HHMM.format(newTime) + ".";
            notificationDao.insert(before.phone(), msg);
        }
    }

    /** ביטול תור ע"י המנהל + הודעה ללקוח */
    public void adminCancel(long id) {
        AppointmentRef ref = adminDao.findById(id);

        adminDao.cancelById(id);

        if (ref != null) {
            String msg = "התור שלך בתאריך " + ref.date() + " בשעה " + HHMM.format(ref.time()) +
                    " בוטל על ידי המספרה.";
            notificationDao.insert(ref.phone(), msg);
        }
    }
}
