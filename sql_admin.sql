-- Admin table is already defined in schema.sql.
-- The application provisions/rotates its admin via ADMIN_USERNAME and ADMIN_PASSWORD.
-- No public or reusable default password is distributed.
USE barbershop;
GO
IF OBJECT_ID('admin_users','U') IS NULL
CREATE TABLE admin_users (
    username VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(100) NOT NULL
);
GO
