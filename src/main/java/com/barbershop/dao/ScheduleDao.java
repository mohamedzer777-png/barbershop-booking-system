package com.barbershop.dao;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * שכבת ה-Entity: כל הגישה למסד מתבצעת כאן, דרך JDBC ו-PreparedStatement בלבד
 * (גם נגד SQL Injection וגם לנכונות). שום שאילתה לא נכתבת מחוץ למחלקה הזו.
 */
@Repository
public class ScheduleDao {

    private final DataSource dataSource;

    public ScheduleDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // צורות נתונים קטנות שהשכבה הזו מחזירה
    public record WorkingHours(LocalTime open, LocalTime close, boolean isOpen) {}
    public record Block(LocalTime start, LocalTime end) {}  // start == null  ->  יום שלם חסום

    /** משך תור בדקות מתוך טבלת ההגדרות */
    public int getSlotMinutes() {
        String sql = "SELECT slot_minutes FROM settings WHERE id = 1";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("slot_minutes") : 30;
        } catch (SQLException e) {
            throw new RuntimeException("getSlotMinutes failed", e);
        }
    }

    /** שעות הפעילות ליום בשבוע. 0=ראשון ... 6=שבת */
    public WorkingHours getWorkingHours(int dayOfWeek) {
        String sql = "SELECT open_time, close_time, is_open FROM working_hours WHERE day_of_week = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, dayOfWeek);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Time open = rs.getTime("open_time");
                    Time close = rs.getTime("close_time");
                    return new WorkingHours(
                            open == null ? null : open.toLocalTime(),
                            close == null ? null : close.toLocalTime(),
                            rs.getBoolean("is_open"));
                }
                return new WorkingHours(null, null, false);
            }
        } catch (SQLException e) {
            throw new RuntimeException("getWorkingHours failed", e);
        }
    }

    /** שעות ההתחלה של תורים תפוסים בתאריך נתון */
    public Set<LocalTime> getBookedTimes(LocalDate date) {
        String sql = "SELECT start_time FROM appointments WHERE appt_date = ? AND status = 'BOOKED'";
        Set<LocalTime> booked = new HashSet<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    booked.add(rs.getTime("start_time").toLocalTime());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("getBookedTimes failed", e);
        }
        return booked;
    }

    /** חסימות בתאריך נתון */
    public List<Block> getBlocks(LocalDate date) {
        String sql = "SELECT start_time, end_time FROM blocks WHERE block_date = ?";
        List<Block> blocks = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time s = rs.getTime("start_time");
                    Time e = rs.getTime("end_time");
                    blocks.add(new Block(
                            s == null ? null : s.toLocalTime(),
                            e == null ? null : e.toLocalTime()));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("getBlocks failed", e);
        }
        return blocks;
    }

    // ---------- עריכת הגדרות ע"י המנהל ----------

    public record DayHours(int dayOfWeek, LocalTime open, LocalTime close, boolean isOpen) {}

    /** כל שבע השורות של שעות הפעילות, ממוינות לפי יום */
    public List<DayHours> getAllWorkingHours() {
        String sql = "SELECT day_of_week, open_time, close_time, is_open FROM working_hours ORDER BY day_of_week";
        List<DayHours> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Time o = rs.getTime("open_time");
                Time c = rs.getTime("close_time");
                list.add(new DayHours(
                        rs.getInt("day_of_week"),
                        o == null ? null : o.toLocalTime(),
                        c == null ? null : c.toLocalTime(),
                        rs.getBoolean("is_open")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("getAllWorkingHours failed", e);
        }
        return list;
    }

    /** עדכון שעות של יום בודד */
    public void updateWorkingHours(int dayOfWeek, LocalTime open, LocalTime close, boolean isOpen) {
        String sql = "UPDATE working_hours SET open_time = ?, close_time = ?, is_open = ? WHERE day_of_week = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTime(1, open == null ? null : Time.valueOf(open));
            ps.setTime(2, close == null ? null : Time.valueOf(close));
            ps.setBoolean(3, isOpen);
            ps.setInt(4, dayOfWeek);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateWorkingHours failed", e);
        }
    }

    /** Whether future appointments exist; changing slot duration would invalidate their spacing. */
    public boolean hasFutureAppointments() {
        String sql = "SELECT CASE WHEN EXISTS (SELECT 1 FROM appointments " +
                "WHERE status = 'BOOKED' AND (appt_date > CAST(GETDATE() AS DATE) " +
                "OR (appt_date = CAST(GETDATE() AS DATE) AND start_time > CAST(GETDATE() AS TIME)))) " +
                "THEN 1 ELSE 0 END AS has_upcoming";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt("has_upcoming") == 1;
        } catch (SQLException e) {
            throw new RuntimeException("hasFutureAppointments failed", e);
        }
    }

    /** עדכון משך התור (בדקות) */
    public void updateSlotMinutes(int minutes) {
        String sql = "UPDATE settings SET slot_minutes = ? WHERE id = 1";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, minutes);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("updateSlotMinutes failed", e);
        }
    }
}
