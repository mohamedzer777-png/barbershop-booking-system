package com.barbershop.control;

import com.barbershop.dao.ScheduleDao;
import com.barbershop.dao.ScheduleDao.DayHours;
import com.barbershop.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** שכבת ה-Control להגדרות הספר: שעות פעילות ומשך תור. */
@Service
public class SettingsService {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] DAY_NAMES = {"ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת"};

    private final ScheduleDao scheduleDao;

    public SettingsService(ScheduleDao scheduleDao) {
        this.scheduleDao = scheduleDao;
    }

    public record DaySetting(int dayOfWeek, String dayName, String open, String close, boolean isOpen) {}
    public record SettingsView(int slotMinutes, List<DaySetting> days) {}

    public SettingsView get() {
        List<DaySetting> days = scheduleDao.getAllWorkingHours().stream()
                .map(d -> new DaySetting(
                        d.dayOfWeek(),
                        DAY_NAMES[d.dayOfWeek()],
                        d.open() == null ? "" : HHMM.format(d.open()),
                        d.close() == null ? "" : HHMM.format(d.close()),
                        d.isOpen()))
                .toList();
        return new SettingsView(scheduleDao.getSlotMinutes(), days);
    }

    /** עדכון יום בודד. אם פתוח — חייבות שעות תקינות (פתיחה לפני סגירה). */
    public void updateDay(int dayOfWeek, String openStr, String closeStr, boolean isOpen) {
        if (dayOfWeek < 0 || dayOfWeek > 6) {
            throw new ValidationException("יום לא תקין");
        }
        LocalTime open = null, close = null;
        if (isOpen) {
            if (openStr == null || openStr.isBlank() || closeStr == null || closeStr.isBlank()) {
                throw new ValidationException("ליום פתוח חובה שעת פתיחה וסגירה");
            }
            open = LocalTime.parse(openStr);
            close = LocalTime.parse(closeStr);
            if (!open.isBefore(close)) {
                throw new ValidationException("שעת הפתיחה חייבת להיות לפני שעת הסגירה");
            }
        }
        scheduleDao.updateWorkingHours(dayOfWeek, open, close, isOpen);
    }

    public void updateSlotMinutes(int minutes) {
        if (minutes < 5 || minutes > 240) {
            throw new ValidationException("משך תור חייב להיות בין 5 ל-240 דקות");
        }
        if (scheduleDao.getSlotMinutes() != minutes && scheduleDao.hasFutureAppointments()) {
            throw new ValidationException("לא ניתן לשנות משך תור כשקיימים תורים עתידיים");
        }
        scheduleDao.updateSlotMinutes(minutes);
    }
}
