package com.barbershop.exception;

/** נזרק כשמשבצת התור נתפסה ממש ברגע ההזמנה — התנגשות בין שני לקוחות. */
public class SlotTakenException extends RuntimeException {
    public SlotTakenException() {
        super("המשבצת נתפסה זה עתה");
    }
}
