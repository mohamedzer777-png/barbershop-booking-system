package com.barbershop.util;

import com.barbershop.exception.ValidationException;

/** עזרי ולידציה ונרמול קלט, משותפים לכל השכבות. */
public final class Validation {

    private Validation() {}

    /** משאיר ספרות בלבד — כך "050-123-4567" ו-"0501234567" נחשבים זהים */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("\\D", "");
    }

    /** מנרמל ומוודא טלפון תקין (9–10 ספרות). מחזיר את הצורה המנורמלת. */
    public static String requirePhone(String raw) {
        String p = normalizePhone(raw);
        if (p == null || p.length() < 9 || p.length() > 10) {
            throw new ValidationException("מספר טלפון לא תקין");
        }
        return p;
    }

    /** מוודא שם לא ריק, מקצר רווחים מיותרים, ומגביל ל-50 תווים */
    public static String requireName(String raw, String label) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException(label + " הוא שדה חובה");
        }
        String n = raw.trim();
        if (n.length() > 50) {
            throw new ValidationException(label + " ארוך מדי (עד 50 תווים)");
        }
        return n;
    }
}
