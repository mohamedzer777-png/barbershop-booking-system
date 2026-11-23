package com.barbershop.control;

import com.barbershop.dao.VerificationDao;
import com.barbershop.dao.VerificationDao.Code;
import com.barbershop.exception.ValidationException;
import com.barbershop.util.Validation;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/** Six-digit OTP validation for the demo. Production requires a real SMS gateway and network rate limiting. */
@Service
public class VerificationService {

    private static final int CODE_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationDao verificationDao;
    private final SmsSender smsSender;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public VerificationService(VerificationDao verificationDao, SmsSender smsSender) {
        this.verificationDao = verificationDao;
        this.smsSender = smsSender;
    }

    public void sendCode(String rawPhone) {
        String phone = Validation.requirePhone(rawPhone);
        LocalDateTime now = LocalDateTime.now();
        Code previous = verificationDao.find(phone);
        if (previous != null && previous.sentAt() != null &&
                now.isBefore(previous.sentAt().plusSeconds(RESEND_COOLDOWN_SECONDS))) {
            throw new ValidationException("ניתן לבקש קוד נוסף בעוד דקה");
        }
        smsSender.requireAvailable();
        String code = String.format(java.util.Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
        verificationDao.upsert(phone, encoder.encode(code), now.plusMinutes(CODE_TTL_MINUTES), now);
        smsSender.send(phone, "קוד האימות שלך למספרה: " + code);
    }

    public void verify(String rawPhone, String inputCode) {
        String phone = Validation.requirePhone(rawPhone);
        Code stored = verificationDao.find(phone);

        if (stored == null) {
            throw new ValidationException("לא נמצא קוד. בקשו קוד חדש");
        }
        if (stored.attempts() >= MAX_ATTEMPTS) {
            throw new ValidationException("יותר מדי ניסיונות. בקשו קוד חדש");
        }
        if (!LocalDateTime.now().isBefore(stored.expiresAt())) {
            throw new ValidationException("הקוד פג תוקף. בקשו קוד חדש");
        }
        if (inputCode == null || !inputCode.matches("\\d{6}") ||
                !encoder.matches(inputCode, stored.code())) {
            verificationDao.incrementAttempts(phone);
            throw new ValidationException("קוד שגוי");
        }
        // The OTP is reusable for up to 5 minutes to view/cancel within the same flow.
    }
}
