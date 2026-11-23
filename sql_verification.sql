-- טבלת קודי אימות SMS (הרץ פעם אחת ב-SSMS)
USE barbershop;
GO
IF OBJECT_ID('verification_codes', 'U') IS NULL
CREATE TABLE verification_codes (
    phone       VARCHAR(20)  PRIMARY KEY,
    code        VARCHAR(8)   NOT NULL,
    expires_at  DATETIME     NOT NULL,
    attempts    INT          NOT NULL DEFAULT 0
);
GO
