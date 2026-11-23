# Architecture

The app follows a three-layer ECB-inspired structure:

```text
Browser (Hebrew RTL, PWA)
   | JSON over same-origin HTTP
   v
Boundary / Spring REST controllers
   | validate request + check session / CSRF
   v
Control / application services
   | availability, booking, verification, admin workflows
   v
DAO / SQL Server via JDBC PreparedStatements
   | customers, appointments, working_hours, blocks,
   | notifications, verification_codes, admin_users
```

## Main workflows

- Customer selects a day -> server computes available time slots from the weekly schedule, current bookings and time blocks.
- Customer requests a six-digit OTP -> demo prints it to server console. The hash is stored in SQL Server with a 5-minute TTL, five verification attempts and a 60-second resend cooldown.
- Customer submits contact details, selected time and OTP -> ownership verification, availability recheck and insert. A filtered unique index prevents duplicate active appointment start times. Slot duration changes are blocked while future appointments exist.
- Customer verifies the same phone to retrieve/cancel appointments and dismiss notices. Cancellation is disallowed within two hours of the appointment.
- Admin authenticates with a BCrypt password -> session cookie and CSRF token protect booking operations, blocks and settings.

## Technical decisions and limits

- SQL Server is required. No in-memory database is configured.
- The demo SMS adapter only works with `DEMO_MODE=true`. It is not an SMS integration.
- Server-side date/time logic uses the JVM time zone. For the Israeli sample schedule, run with `-Duser.timezone=Asia/Jerusalem` or set the host TZ accordingly.
- A real deployment requires HTTPS, production SMS, network-level throttling, monitoring, backups and a security review. See `docs/SECURITY.md`.
