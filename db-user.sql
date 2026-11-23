-- Optional least-privilege SQL Server login for local development.
-- In SSMS enable Query > SQLCMD Mode, then use an environment-safe value for DB_PASSWORD.
-- Run once as a SQL Server administrator; do NOT commit a completed copy with a real password.
-- Replace the placeholder in your local editor only, never in this tracked file.
USE master;
GO
-- Replace the example below with a strong, unique password before executing.
-- Example command (DO NOT run without replacing):
-- CREATE LOGIN barber_app WITH PASSWORD = '<YOUR-STRONG-UNIQUE-PASSWORD>', CHECK_POLICY = ON;
-- Then execute:
USE barbershop;
GO
-- Assumes the barber_app server login exists.
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'barber_app')
    CREATE USER barber_app FOR LOGIN barber_app;
GO
ALTER ROLE db_datareader ADD MEMBER barber_app;
ALTER ROLE db_datawriter ADD MEMBER barber_app;
GO
