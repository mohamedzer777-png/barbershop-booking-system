package com.barbershop.control;

import com.barbershop.dao.VerificationDao;
import com.barbershop.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerificationServiceTest {
    private VerificationDao dao;
    private SmsSender sms;
    private VerificationService service;

    @BeforeEach
    void setUp() {
        dao = mock(VerificationDao.class);
        sms = mock(SmsSender.class);
        service = new VerificationService(dao, sms);
    }

    @Test
    void generatedOtpIsSixDigitsAndStoredHashed() {
        service.sendCode("050-123-4567");
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(dao).upsert(eq("0501234567"), hash.capture(), any(), any());
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(sms).send(eq("0501234567"), message.capture());
        String code = message.getValue().replaceAll(".*: ", "");
        assertTrue(code.matches("\\d{6}"));
        assertFalse(hash.getValue().contains(code));
        assertTrue(new BCryptPasswordEncoder().matches(code, hash.getValue()));
    }

    @Test
    void resendWithinSixtySecondsIsRejected() {
        when(dao.find("0501234567")).thenReturn(new VerificationDao.Code(
                "hash", LocalDateTime.now().plusMinutes(4), 0, LocalDateTime.now()));
        assertThrows(ValidationException.class, () -> service.sendCode("0501234567"));
        verify(dao, never()).upsert(any(), any(), any(), any());
        verifyNoInteractions(sms);
    }

    @Test
    void wrongOtpIncrementsAttemptCounter() {
        String hash = new BCryptPasswordEncoder().encode("123456");
        when(dao.find("0501234567")).thenReturn(new VerificationDao.Code(
                hash, LocalDateTime.now().plusMinutes(5), 0, LocalDateTime.now().minusMinutes(1)));
        assertThrows(ValidationException.class, () -> service.verify("0501234567", "111111"));
        verify(dao).incrementAttempts("0501234567");
    }

    @Test
    void validOtpPassesWithinTtl() {
        String hash = new BCryptPasswordEncoder().encode("123456");
        when(dao.find("0501234567")).thenReturn(new VerificationDao.Code(
                hash, LocalDateTime.now().plusMinutes(5), 0, LocalDateTime.now().minusMinutes(1)));
        assertDoesNotThrow(() -> service.verify("0501234567", "123456"));
    }

    @Test
    void fiveFailedAttemptsLockTheCode() {
        when(dao.find("0501234567")).thenReturn(new VerificationDao.Code(
                "hash", LocalDateTime.now().plusMinutes(5), 5, LocalDateTime.now().minusMinutes(1)));
        assertThrows(ValidationException.class, () -> service.verify("0501234567", "123456"));
        verify(dao, never()).incrementAttempts(any());
    }
}
