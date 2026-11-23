package com.barbershop.dao;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** שכבת ה-Entity של ההודעות ללקוח. */
@Repository
public class NotificationDao {

    private final DataSource dataSource;

    public NotificationDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public record Notification(long id, String message) {}

    /** יצירת הודעה חדשה ללקוח */
    public void insert(String phone, String message) {
        String sql = "INSERT INTO notifications (phone, message) VALUES (?, ?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("notification insert failed", e);
        }
    }

    /** ההודעות שהלקוח עוד לא קרא */
    public List<Notification> findUnread(String phone) {
        String sql = "SELECT id, message FROM notifications WHERE phone = ? AND is_read = 0 ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Notification(rs.getLong("id"), rs.getString("message")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("findUnread failed", e);
        }
        return list;
    }

    /** סימון הודעה כנקראה (הלקוח סגר אותה) */
    public void markRead(long id, String phone) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE id = ? AND phone = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, phone);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("markRead failed", e);
        }
    }
}
