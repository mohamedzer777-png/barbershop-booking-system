package com.barbershop.boundary;

import com.barbershop.control.AdminService;
import com.barbershop.control.AdminService.BlockView;
import com.barbershop.control.AdminService.CalendarEntry;
import com.barbershop.control.SettingsService;
import com.barbershop.control.SettingsService.SettingsView;
import com.barbershop.exception.SlotTakenException;
import com.barbershop.exception.SlotUnavailableException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * שכבת ה-Boundary של המנהל. כל פעולה בודקת תחילה ש-המנהל מחובר (Session) — אחרת 401.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String SESSION_KEY = "admin";
    private static final String CSRF_KEY = "admin_csrf";

    private final AdminService adminService;
    private final SettingsService settingsService;

    public AdminController(AdminService adminService, SettingsService settingsService) {
        this.adminService = adminService;
        this.settingsService = settingsService;
    }

    public record LoginRequest(String username, String password) {}
    public record BlockRequest(String date, String start, String end, String reason) {}
    public record AdminBookRequest(String phone, String firstName, String lastName, String date, String time) {}
    public record MoveRequest(String date, String time) {}
    public record DayHoursRequest(int dayOfWeek, String open, String close, boolean isOpen) {}
    public record SlotRequest(int minutes) {}

    // ---------- התחברות ----------

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req, HttpSession session) {
        if (adminService.checkLogin(req.username(), req.password())) {
            session.changeSessionId();
            session.setAttribute(SESSION_KEY, req.username());
            String csrf = java.util.UUID.randomUUID().toString();
            session.setAttribute(CSRF_KEY, csrf);
            return Map.of("ok", true, "username", req.username(), "csrfToken", csrf);
        }
        throw new BadLoginException();
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return Map.of("ok", true);
    }

    @GetMapping("/session")
    public Map<String, Object> session(HttpSession session) {
        Object admin = session.getAttribute(SESSION_KEY);
        if (admin != null) {
            Object csrf = session.getAttribute(CSRF_KEY);
            if (csrf == null) {
                csrf = java.util.UUID.randomUUID().toString();
                session.setAttribute(CSRF_KEY, csrf);
            }
            return Map.of("loggedIn", true, "username", admin.toString(), "csrfToken", csrf);
        }
        return Map.of("loggedIn", false);
    }

    // ---------- יומן ----------

    @GetMapping("/appointments")
    public List<CalendarEntry> appointments(@RequestParam String date, HttpSession session) {
        requireAdmin(session);
        return adminService.getAppointments(LocalDate.parse(date));
    }

    // ---------- חסימות ----------

    @PostMapping("/blocks")
    public Map<String, Object> addBlock(@RequestBody BlockRequest req, HttpSession session) {
        requireAdmin(session);
        adminService.addBlock(req.date(), req.start(), req.end(), req.reason());
        return Map.of("ok", true);
    }

    @GetMapping("/blocks")
    public List<BlockView> blocks(@RequestParam String date, HttpSession session) {
        requireAdmin(session);
        return adminService.getBlocks(LocalDate.parse(date));
    }

    @PostMapping("/blocks/{id}/delete")
    public Map<String, Object> deleteBlock(@PathVariable long id, HttpSession session) {
        requireAdmin(session);
        adminService.removeBlock(id);
        return Map.of("ok", true);
    }

    // ---------- שליטה ידנית בתורים ----------

    // POST /api/admin/appointments — קביעת תור ידנית (כוח עליון)
    @PostMapping("/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> adminBook(@RequestBody AdminBookRequest req, HttpSession session) {
        requireAdmin(session);
        adminService.adminBook(req.phone(), req.firstName(), req.lastName(), req.date(), req.time());
        return Map.of("ok", true);
    }

    // POST /api/admin/appointments/{id}/move — הזזת תור
    @PostMapping("/appointments/{id}/move")
    public Map<String, Object> move(@PathVariable long id, @RequestBody MoveRequest req, HttpSession session) {
        requireAdmin(session);
        adminService.moveAppointment(id, req.date(), req.time());
        return Map.of("ok", true);
    }

    // POST /api/admin/appointments/{id}/cancel — ביטול תור ע"י המנהל
    @PostMapping("/appointments/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable long id, HttpSession session) {
        requireAdmin(session);
        adminService.adminCancel(id);
        return Map.of("ok", true);
    }

    // ---------- הגדרות (שעות פעילות + משך תור) ----------

    @GetMapping("/settings")
    public SettingsView getSettings(jakarta.servlet.http.HttpSession session) {
        requireAdmin(session);
        return settingsService.get();
    }

    @PostMapping("/settings/day")
    public Map<String, Object> updateDay(@RequestBody DayHoursRequest req, jakarta.servlet.http.HttpSession session) {
        requireAdmin(session);
        settingsService.updateDay(req.dayOfWeek(), req.open(), req.close(), req.isOpen());
        return Map.of("ok", true);
    }

    @PostMapping("/settings/slot")
    public Map<String, Object> updateSlot(@RequestBody SlotRequest req, jakarta.servlet.http.HttpSession session) {
        requireAdmin(session);
        settingsService.updateSlotMinutes(req.minutes());
        return Map.of("ok", true);
    }

    // ---------- עזר + חריגות ----------

    private void requireAdmin(HttpSession session) {
        if (session.getAttribute(SESSION_KEY) == null) {
            throw new NotLoggedInException();
        }
    }

    static class BadLoginException extends RuntimeException {}
    static class NotLoggedInException extends RuntimeException {}

    @ExceptionHandler(BadLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> badLogin(BadLoginException e) {
        return Map.of("error", "שם משתמש או סיסמה שגויים");
    }

    @ExceptionHandler(NotLoggedInException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> notLoggedIn(NotLoggedInException e) {
        return Map.of("error", "יש להתחבר תחילה");
    }

    @ExceptionHandler(SlotTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> slotTaken(SlotTakenException e) {
        return Map.of("error", "השעה הזו כבר תפוסה");
    }

    @ExceptionHandler(SlotUnavailableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> slotUnavailable(SlotUnavailableException e) {
        return Map.of("error", e.getMessage());
    }
}
