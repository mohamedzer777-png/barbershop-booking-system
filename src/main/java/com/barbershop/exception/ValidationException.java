package com.barbershop.exception;

/** נזרק כשקלט מהמשתמש אינו תקין (טלפון/שם וכו'). ממופה ל-HTTP 400. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
