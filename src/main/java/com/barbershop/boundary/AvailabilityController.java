package com.barbershop.boundary;

import com.barbershop.control.AvailabilityService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * שכבת ה-Boundary (צד השרת): נקודות הקצה ש-Frontend (HTML/Bootstrap) יקרא להן.
 * כאן אין לוגיקה עסקית — רק קבלת בקשה, קריאה ל-Control, והחזרת JSON.
 */
@RestController
@RequestMapping("/api")
public class AvailabilityController {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    // צורת התשובה ב-JSON:  { "date": "2026-05-28", "slots": ["09:00", "09:30", ...] }
    public record AvailabilityResponse(String date, List<String> slots) {}

    // GET /api/availability?date=2026-05-28
    @GetMapping("/availability")
    public AvailabilityResponse availability(@RequestParam String date) {
        LocalDate parsed = LocalDate.parse(date);   // פורמט ISO: YYYY-MM-DD
        List<String> slots = availabilityService.availableSlots(parsed)
                .stream()
                .map(HHMM::format)
                .toList();
        return new AvailabilityResponse(date, slots);
    }
}
