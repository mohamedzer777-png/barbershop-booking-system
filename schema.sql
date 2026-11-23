-- =====================================================================
--  Master Barber Shop — סכמת מסד נתונים מלאה (SQL Server)
--  בטוח להרצה חוזרת: יוצר רק טבלאות/נתונים שחסרים (IF NOT EXISTS).
-- =====================================================================
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'barbershop')
    CREATE DATABASE barbershop;
GO
USE barbershop;
GO

-- לקוחות
IF OBJECT_ID('customers','U') IS NULL
CREATE TABLE customers (
    phone       VARCHAR(20)  PRIMARY KEY,
    first_name  NVARCHAR(50) NOT NULL,
    last_name   NVARCHAR(50) NOT NULL,
    created_at  DATETIME DEFAULT GETDATE() NOT NULL
);
GO

-- תורים (ביטול רך)
IF OBJECT_ID('appointments','U') IS NULL
CREATE TABLE appointments (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    phone       VARCHAR(20)  NOT NULL,
    appt_date   DATE         NOT NULL,
    start_time  TIME         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'BOOKED' CHECK (status IN ('BOOKED','CANCELLED')),
    created_at  DATETIME DEFAULT GETDATE() NOT NULL,
    CONSTRAINT fk_appt_customer FOREIGN KEY (phone) REFERENCES customers(phone)
);
GO
-- מניעת הזמנה כפולה ברמת המסד (רק כל עוד התור פעיל)
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='uq_active_slot' AND object_id=OBJECT_ID('appointments'))
    CREATE UNIQUE NONCLUSTERED INDEX uq_active_slot ON appointments (appt_date, start_time) WHERE status = 'BOOKED';
GO
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='idx_appt_date' AND object_id=OBJECT_ID('appointments'))
    CREATE INDEX idx_appt_date ON appointments (appt_date);
GO
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name='idx_appt_phone' AND object_id=OBJECT_ID('appointments'))
    CREATE INDEX idx_appt_phone ON appointments (phone);
GO

-- הגדרות (שורה אחת)
IF OBJECT_ID('settings','U') IS NULL
CREATE TABLE settings (
    id            TINYINT PRIMARY KEY DEFAULT 1,
    slot_minutes  INT     NOT NULL DEFAULT 30,
    CONSTRAINT chk_single_row CHECK (id = 1)
);
GO

-- שעות פעילות שבועיות (0=ראשון ... 6=שבת)
IF OBJECT_ID('working_hours','U') IS NULL
CREATE TABLE working_hours (
    day_of_week  TINYINT PRIMARY KEY,
    open_time    TIME,
    close_time   TIME,
    is_open      BIT NOT NULL DEFAULT 1
);
GO

-- חסימות נקודתיות (start_time NULL = יום שלם)
IF OBJECT_ID('blocks','U') IS NULL
CREATE TABLE blocks (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    block_date  DATE         NOT NULL,
    start_time  TIME         NULL,
    end_time    TIME         NULL,
    reason      NVARCHAR(100)
);
GO

-- מנהלים (סיסמה מוצפנת bcrypt בלבד)
IF OBJECT_ID('admin_users','U') IS NULL
CREATE TABLE admin_users (
    username      VARCHAR(50)  PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL
);
GO

-- הודעות ללקוח (הזזה/ביטול ע"י המנהל)
IF OBJECT_ID('notifications','U') IS NULL
CREATE TABLE notifications (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    phone       VARCHAR(20)   NOT NULL,
    message     NVARCHAR(300) NOT NULL,
    is_read     BIT NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_notif_customer FOREIGN KEY (phone) REFERENCES customers(phone)
);
GO

-- קודי אימות SMS (שורה אחת לכל טלפון)
IF OBJECT_ID('verification_codes','U') IS NULL
CREATE TABLE verification_codes (
    phone       VARCHAR(20) PRIMARY KEY,
    code        VARCHAR(100) NOT NULL,
    expires_at  DATETIME    NOT NULL,
    attempts    INT NOT NULL DEFAULT 0,
    last_sent_at DATETIME NOT NULL DEFAULT GETDATE()
);
GO

-- Compatible migration from the earlier four-digit OTP schema.
IF COL_LENGTH('verification_codes', 'last_sent_at') IS NULL
    ALTER TABLE verification_codes ADD last_sent_at DATETIME NOT NULL DEFAULT GETDATE();
GO
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('verification_codes')
           AND name='code' AND max_length < 100)
    ALTER TABLE verification_codes ALTER COLUMN code VARCHAR(100) NOT NULL;
GO

-- =====================================================================
--  נתוני התחלה (Seed) — רק אם הטבלה ריקה
-- =====================================================================
IF NOT EXISTS (SELECT 1 FROM settings WHERE id = 1)
    INSERT INTO settings (id, slot_minutes) VALUES (1, 30);
GO
-- שעות: א',ב',ד',ה',שבת 09:00-21:00 · ג' חופש · ו' 13:00-22:00
IF NOT EXISTS (SELECT 1 FROM working_hours)
    INSERT INTO working_hours (day_of_week, open_time, close_time, is_open) VALUES
        (0,'09:00','21:00',1),(1,'09:00','21:00',1),(2,NULL,NULL,0),
        (3,'09:00','21:00',1),(4,'09:00','21:00',1),(5,'13:00','22:00',1),(6,'09:00','21:00',1);
GO
-- Admin account is provisioned/rotated by ADMIN_USERNAME and ADMIN_PASSWORD at application startup.

