# Verification and acceptance evidence

This document records observed outcomes for the **local portfolio demo**, not a production certification.

## Automated checks

- Java / Maven: `mvn clean verify` completed successfully locally on 2026-11-23.
- 15 JUnit tests passed (0 failed, 0 errors, 0 skipped) locally and on GitHub Actions.
- These are primarily **unit tests**; they do not replace automated SQL Server integration or full browser E2E tests.

## Manual local acceptance checks — 2026-11-23

Environment: Spring Boot local server (`localhost:8080`), local Microsoft SQL Server (`barbershop`), `DEMO_MODE=true`. No real SMS gateway.

| Scenario | Observed result |
| --- | --- |
| Customer reservation UI and availability | Date/time slots displayed from running application |
| OTP demo flow | Code emitted to local server console; accepted for booking |
| Customer appointment creation | Success message; appointment recorded as `BOOKED` in SQL Server |
| Admin login | Login succeeded and admin calendar/settings loaded |
| Booked slot visibility | Reserved time disappeared from customer availability and appeared on admin calendar |
| Customer cancellation | Appointment status verified as `CANCELLED` with SQL query; time became available |
| Time block added and removed | Blocked slot disappeared from customer availability; returned after removal |
| Admin reschedule | Appointment moved; new time reflected in both admin and customer views |
| Customer notification | In-app notice appeared after admin reschedule and after admin cancellation |
| Admin cancellation | Admin calendar cleared; customer was notified; SQL status `CANCELLED` |
| Duplicate booking attempt | UI/API rejected a second booking for the same date/time as already taken |

The database also has a filtered unique index on active appointment start times (`uq_active_slot`) to guard against duplicate active slots. A simultaneous multi-request concurrency stress test has **not** been performed.

All acceptance results were manually observed on one developer workstation. Screenshots of live sessions are **not uploaded** because they can expose personal phone numbers or verification codes; README preview images use synthetic, mocked API data.

## Remaining limitations

- Automated tests against a live SQL Server instance and real browser-to-backend E2E automation are pending.
- Real SMS delivery, network-level rate limiting, hardened authentication, HTTPS configuration, operational monitoring and security review are not implemented/verified for production use.
- Some additional UI/edge-case scenarios (such as admin hours validation and mobile devices) have not been manually signed off.
- This is appropriate to demonstrate a student portfolio project locally, **not** to deploy as a customer-facing service without further work.
