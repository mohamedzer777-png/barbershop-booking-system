package com.barbershop.exception;

/** נזרק כשהשעה המבוקשת אינה משבצת פנויה חוקית (עברה, סגור, חסום וכד'). */
public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String message) {
        super(message);
    }
}
