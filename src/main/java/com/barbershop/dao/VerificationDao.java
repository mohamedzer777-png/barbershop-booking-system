package com.barbershop.dao;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;

/** SQL Server-backed one-time verification codes (BCrypt hash, expiry, resend timestamp, attempts). */
@Repository
public class VerificationDao {

    private final DataSource dataSource;

    public VerificationDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public record Code(String code, LocalDateTime expiresAt, int attempts, LocalDateTime sentAt) {}

    public void upsert(String phone, String encodedCode, LocalDateTime expiresAt, LocalDateTime sentAt) {
        String sql =
            "MERGE verification_codes WITH (HOLDLOCK) AS t " +
            "USING (SELECT ? AS phone) AS s ON t.phone = s.phone " +
            "WHEN MATCHED THEN UPDATE SET code = ?, expires_at = ?, attempts = 0, last_sent_at = ? " +
            "WHEN NOT MATCHED THEN INSERT (phone, code, expires_at, attempts, last_sent_at) VALUES (?, ?, ?, 0, ?);";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, encodedCode);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.setTimestamp(4, Timestamp.valueOf(sentAt));
            ps.setString(5, phone);
            ps.setString(6, encodedCode);
            ps.setTimestamp(7, Timestamp.valueOf(expiresAt));
            ps.setTimestamp(8, Timestamp.valueOf(sentAt));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("verification upsert failed", e);
        }
    }

    public Code find(String phone) {
        String sql = "SELECT code, expires_at, attempts, last_sent_at FROM verification_codes WHERE phone = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp sentAt = rs.getTimestamp("last_sent_at");
                    return new Code(rs.getString("code"),
                            rs.getTimestamp("expires_at").toLocalDateTime(),
                            rs.getInt("attempts"),
                            sentAt == null ? null : sentAt.toLocalDateTime());
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("verification find failed", e);
        }
    }

    public void incrementAttempts(String phone) {
        String sql = "UPDATE verification_codes SET attempts = attempts + 1 WHERE phone = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("verification increment failed", e);
        }
    }
}
