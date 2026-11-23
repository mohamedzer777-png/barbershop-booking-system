package com.barbershop.control;

import com.barbershop.dao.ScheduleDao;
import com.barbershop.dao.ScheduleDao.Block;
import com.barbershop.dao.ScheduleDao.WorkingHours;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * שכבת ה-Control: מחשבת את המשבצות הפנויות ליום נתון =
 * כל המשבצות לפי שעות הפעילות, פחות התפוסים, פחות החסומים, פחות מה שכבר עבר.
 */
@Service
public class AvailabilityService {

    private final ScheduleDao scheduleDao;

    public AvailabilityService(ScheduleDao scheduleDao) {
        this.scheduleDao = scheduleDao;
    }

    public List<LocalTime> availableSlots(LocalDate date) {
        // אין משבצות בתאריך שעבר
        if (date.isBefore(LocalDate.now())) {
            return List.of();
        }

        // ב-Java getDayOfWeek מחזיר MONDAY(1)..SUNDAY(7). ממירים ל-0=ראשון..6=שבת.
        int dayOfWeek = date.getDayOfWeek().getValue() % 7;
        WorkingHours hours = scheduleDao.getWorkingHours(dayOfWeek);

        // יום סגור -> אין משבצות
        if (!hours.isOpen() || hours.open() == null || hours.close() == null) {
            return List.of();
        }

        int slotMinutes = scheduleDao.getSlotMinutes();
        Set<LocalTime> booked = scheduleDao.getBookedTimes(date);
        List<Block> blocks = scheduleDao.getBlocks(date);

        boolean isToday = date.equals(LocalDate.now());
        LocalTime now = LocalTime.now();

        List<LocalTime> slots = new ArrayList<>();
        LocalTime slot = hours.open();

        // המשבצת האחרונה חייבת להסתיים לא־יאוחר משעת הסגירה
        while (!slot.plusMinutes(slotMinutes).isAfter(hours.close())) {
            boolean free = true;
            if (booked.contains(slot))                  free = false;   // כבר תפוס
            if (isToday && !slot.isAfter(now))          free = false;   // שעה שכבר עברה היום
            if (free && isBlocked(slot, blocks))        free = false;   // חסום ע"י המנהל

            if (free) slots.add(slot);
            slot = slot.plusMinutes(slotMinutes);
        }
        return slots;
    }

    private boolean isBlocked(LocalTime slot, List<Block> blocks) {
        for (Block b : blocks) {
            if (b.start() == null) return true;  // יום שלם חסום
            boolean afterStart = !slot.isBefore(b.start());
            boolean beforeEnd  = (b.end() == null) || slot.isBefore(b.end());
            if (afterStart && beforeEnd) return true;
        }
        return false;
    }
}
