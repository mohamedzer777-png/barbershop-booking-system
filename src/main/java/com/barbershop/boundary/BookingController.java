package com.barbershop.boundary;

import com.barbershop.control.BookingService;
import com.barbershop.control.BookingService.BookingResult;
import com.barbershop.control.BookingService.MyAppointment;
import com.barbershop.control.BookingService.Note;
import com.barbershop.control.VerificationService;
import com.barbershop.exception.SlotTakenException;
import com.barbershop.exception.SlotUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/** שכבת ה-Boundary של הלקוח: אימות טלפון, תורים, והודעות. */
@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;
    private final VerificationService verificationService;

    public BookingController(BookingService bookingService, VerificationService verificationService) {
        this.bookingService = bookingService;
        this.verificationService = verificationService;
    }

    public record SendCodeRequest(String phone) {}
    public record BookingRequest(String phone, String firstName, String lastName,
                                 String date, String time, String code) {}
    public record VerifiedRequest(String phone, String code) {}
    public record CancelRequest(String phone, String code) {}

    // POST /api/verify/send — שולח קוד אימות לטלפון
    @PostMapping("/verify/send")
    public Map<String, Object> sendCode(@RequestBody SendCodeRequest req) {
        verificationService.sendCode(req.phone());
        return Map.of("sent", true);
    }

    // POST /api/appointments — קביעת תור (כולל קוד אימות)
    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResult book(@RequestBody BookingRequest req) {
        return bookingService.book(req.phone(), req.firstName(), req.lastName(),
                LocalDate.parse(req.date()), LocalTime.parse(req.time()), req.code());
    }

    // POST /api/appointments/mine — התורים של הלקוח (אחרי אימות)
    @PostMapping("/appointments/mine")
    public Map<String, Object> myAppointments(@RequestBody VerifiedRequest req) {
        List<MyAppointment> appts = bookingService.getUpcomingVerified(req.phone(), req.code());
        List<Note> notes = bookingService.getNotifications(req.phone());
        return Map.of("appointments", appts, "notifications", notes);
    }

    // POST /api/appointments/{id}/cancel — ביטול (כולל קוד אימות)
    @PostMapping("/appointments/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable long id, @RequestBody CancelRequest req) {
        bookingService.cancel(id, req.phone(), req.code());
        return Map.of("cancelled", true);
    }

    // POST /api/notifications/{id}/read — סימון הודעה כנקראה
    @PostMapping("/notifications/{id}/read")
    public Map<String, Object> readNotification(@PathVariable long id, @RequestBody VerifiedRequest req) {
        bookingService.markNotificationRead(id, req.phone(), req.code());
        return Map.of("ok", true);
    }

    // ----- תרגום חריגות -----
    @ExceptionHandler(SlotTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> slotTaken(SlotTakenException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(SlotUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> slotUnavailable(SlotUnavailableException e) {
        return Map.of("error", e.getMessage());
    }
}
