package com.barbershop.dao;

import com.barbershop.exception.SlotTakenException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * שכבת ה-Entity לכתיבה וקריאה של תורים. כל גישה למסד דרך PreparedStatement בלבד.
 */
@Repository
public class BookingDao {

    private final DataSource dataSource;

    public BookingDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public record AppointmentView(long id, LocalDate date, LocalTime time) {}
    public record AppointmentDetail(LocalDate date, LocalTime time, String status) {}

    /** האם הלקוח כבר קיים במסד */
    public boolean customerExists(String phone) {
        String sql = "SELECT 1 FROM customers WHERE phone = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("customerExists failed", e);
        }
    }

    /** יצירת לקוח חדש */
    public void insertCustomer(String phone, String firstName, String lastName) {
        String sql = "INSERT INTO customers (phone, first_name, last_name) VALUES (?, ?, ?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertCustomer failed", e);
        }
    }

    /**
     * יצירת תור. אם המשבצת תפוסה, ה-Filtered Index זורק שגיאת כפילות
     * (2601 / 2627), ואנחנו הופכים אותה ל-SlotTakenException.
     */
    public long insertAppointment(String phone, LocalDate date, LocalTime time) {
        String sql = "INSERT INTO appointments (phone, appt_date, start_time) VALUES (?, ?, ?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, phone);
            ps.setDate(2, Date.valueOf(date));
            ps.setTime(3, Time.valueOf(time));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                throw new SlotTakenException();
            }
            throw new RuntimeException("insertAppointment failed", e);
        }
    }

    /** התורים הפעילים של לקוח, מהיום והלאה */
    public List<AppointmentView> findUpcomingByPhone(String phone) {
        String sql = "SELECT id, appt_date, start_time FROM appointments " +
                     "WHERE phone = ? AND status = 'BOOKED' AND appt_date >= CAST(GETDATE() AS DATE) " +
                     "ORDER BY appt_date, start_time";
        List<AppointmentView> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AppointmentView(
                            rs.getLong("id"),
                            rs.getDate("appt_date").toLocalDate(),
                            rs.getTime("start_time").toLocalTime()));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findUpcomingByPhone failed", e);
        }
        return list;
    }

    /** שליפת תור לפי מזהה + טלפון — לאימות בעלות לפני ביטול */
    public AppointmentDetail findByIdAndPhone(long id, String phone) {
        String sql = "SELECT appt_date, start_time, status FROM appointments WHERE id = ? AND phone = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AppointmentDetail(
                            rs.getDate("appt_date").toLocalDate(),
                            rs.getTime("start_time").toLocalTime(),
                            rs.getString("status"));
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByIdAndPhone failed", e);
        }
    }

    /** ביטול רך — רק אם התור שייך לטלפון ועדיין פעיל. מחזיר כמה שורות עודכנו. */
    public int cancel(long id, String phone) {
        String sql = "UPDATE appointments SET status = 'CANCELLED' " +
                     "WHERE id = ? AND phone = ? AND status = 'BOOKED'";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, phone);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("cancel failed", e);
        }
    }
}
