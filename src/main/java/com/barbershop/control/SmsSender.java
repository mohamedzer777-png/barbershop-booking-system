package com.barbershop.control;

import com.barbershop.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** DEMO ONLY: OTP codes appear in the local server console when DEMO_MODE=true. */
@Component
public class SmsSender {

    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

    @Value("${app.demo-mode:false}")
    private boolean demoMode;

    public void requireAvailable() {
        if (!demoMode) {
            throw new ValidationException("שליחת SMS אינה מוגדרת. להרצה מקומית הגדירו DEMO_MODE=true");
        }
    }

    public void send(String phone, String message) {
        requireAvailable();
        log.info("[DEMO SMS to {}] {}", phone, message);
    }
}
