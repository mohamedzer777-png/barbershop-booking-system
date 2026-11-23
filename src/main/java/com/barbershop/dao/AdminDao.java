package com.barbershop.dao;

import com.barbershop.exception.SlotTakenException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** שכבת ה-Entity של המנהל: סיסמה, יומן, חסימות, ושינוי תורים ידני. */
@Repository
public class AdminDao {

    private final DataSource dataSource;

    public AdminDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public record AdminAppointment(long id, LocalTime time, String firstName, String lastName, String phone) {}
    public record Block(long id, LocalTime start, LocalTime end, String reason) {}
    public record AppointmentRef(String phone, LocalDate date, LocalTime time) {}

    public String findPasswordHash(String username) {
        String sql = "SELECT password_hash FROM admin_users WHERE username = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("password_hash") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findPasswordHash failed", e);
        }
    }

    public List<AdminAppointment> findBookedByDate(LocalDate date) {
        String sql = "SELECT a.id, a.start_time, c.first_name, c.last_name, a.phone " +
                     "FROM appointments a JOIN customers c ON a.phone = c.phone " +
                     "WHERE a.appt_date = ? AND a.status = 'BOOKED' " +
                     "ORDER BY a.start_time";
        List<AdminAppointment> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AdminAppointment(
                            rs.getLong("id"), rs.getTime("start_time").toLocalTime(),
                            rs.getString("first_name"), rs.getString("last_name"), rs.getString("phone")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findBookedByDate failed", e);
        }
        return list;
    }

    /** שליפת פרטי תור לפי מזהה בלבד — לפני הזזה/ביטול, כדי לדעת למי לשלוח הודעה */
    public AppointmentRef findById(long id) {
        String sql = "SELECT phone, appt_date, start_time FROM appointments WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AppointmentRef(
                            rs.getString("phone"),
                            rs.getDate("appt_date").toLocalDate(),
                            rs.getTime("start_time").toLocalTime());
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed", e);
        }
    }

    // ---------- חסימות ----------
    public void insertBlock(LocalDate date, LocalTime start, LocalTime end, String reason) {
        String sql = "INSERT INTO blocks (block_date, start_time, end_time, reason) VALUES (?, ?, ?, ?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setTime(2, start == null ? null : Time.valueOf(start));
            ps.setTime(3, end == null ? null : Time.valueOf(end));
            ps.setString(4, (reason == null || reason.isBlank()) ? null : reason);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("insertBlock failed", e);
        }
    }

    public List<Block> findBlocksByDate(LocalDate date) {
        String sql = "SELECT id, start_time, end_time, reason FROM blocks WHERE block_date = ? ORDER BY start_time";
        List<Block> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time s = rs.getTime("start_time");
                    Time e = rs.getTime("end_time");
                    list.add(new Block(rs.getLong("id"),
                            s == null ? null : s.toLocalTime(),
                            e == null ? null : e.toLocalTime(),
                            rs.getString("reason")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findBlocksByDate failed", e);
        }
        return list;
    }

    public void deleteBlock(long id) {
        String sql = "DELETE FROM blocks WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteBlock failed", e);
        }
    }

    // ---------- שינוי תורים ידני ----------
    public void move(long id, LocalDate date, LocalTime time) {
        String sql = "UPDATE appointments SET appt_date = ?, start_time = ? WHERE id = ? AND status = 'BOOKED'";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setTime(2, Time.valueOf(time));
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) throw new SlotTakenException();
            throw new RuntimeException("move failed", e);
        }
    }

    public void cancelById(long id) {
        String sql = "UPDATE appointments SET status = 'CANCELLED' WHERE id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("cancelById failed", e);
        }
    }
}
