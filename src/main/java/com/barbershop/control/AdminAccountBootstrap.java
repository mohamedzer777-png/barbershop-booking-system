package com.barbershop.control;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds/rotates the single demo admin account from environment variables, never from source control. */
@Component
public class AdminAccountBootstrap implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final String username;
    private final String password;

    public AdminAccountBootstrap(JdbcTemplate jdbc,
                                 @Value("${app.admin.username:admin}") String username,
                                 @Value("${app.admin.password:}") String password) {
        this.jdbc = jdbc;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            return; // Login intentionally disabled until ADMIN_PASSWORD is configured.
        }
        if (password.length() < 12) {
            throw new IllegalArgumentException("ADMIN_PASSWORD must be at least 12 characters");
        }
        String hash = new BCryptPasswordEncoder().encode(password);
        int updated = jdbc.update("UPDATE admin_users SET password_hash = ? WHERE username = ?", hash, username);
        if (updated == 0) {
            jdbc.update("INSERT INTO admin_users (username, password_hash) VALUES (?, ?)", username, hash);
        }
    }
}
