package com.barbershop.boundary;

import com.barbershop.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * טיפול מרוכז בשגיאות קלט שאינן שייכות לבקר מסוים.
 * מבטיח שקלט שגוי מחזיר 400 עם הודעה ברורה, במקום שגיאת 500 מכוערת.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // קלט לא תקין מהמשתמש (טלפון/שם וכו')
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(ValidationException e) {
        return Map.of("error", e.getMessage());
    }

    // תאריך/שעה במבנה שגוי, גוף בקשה פגום, או פרמטר חובה חסר
    @ExceptionHandler({
            DateTimeParseException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadInput(Exception e) {
        return Map.of("error", "הבקשה אינה תקינה");
    }
}
