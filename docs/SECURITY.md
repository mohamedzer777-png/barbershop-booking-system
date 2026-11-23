# Security notes / demo boundaries

This is a student portfolio MVP, **not production-ready**. Do not collect real customer phone numbers until the following are implemented and independently reviewed.

## Implemented

- Parameterized JDBC queries; appointments protected by a filtered SQL Server uniqueness index.
- Customer OTP gate for booking, viewing, cancelling, and dismissing notices.
- Six-digit, five-minute OTPs stored as BCrypt hashes; five verification attempts; sixty-second resend cooldown per phone.
- Admin password comes from `ADMIN_PASSWORD`, not tracked source; BCrypt hash in database. Admin password is rotated to this environment value at startup.
- Admin session ID rotated on login, HttpOnly + SameSite=Lax cookie, and CSRF token required for admin POST operations.
- Basic server-side validation and HTML escaping of customer-provided text in the admin interface.

## Not yet suitable for production

- No real SMS gateway. Demo OTPs are printed in server logs when `DEMO_MODE=true`; never enable this in a public deployment.
- Missing IP-/network-level login and OTP rate limits, fraud monitoring and automated abuse controls.
- No proof of end-to-end testing against a real SQL Server instance in the supplied environment.
- No audited migrations, database backup automation, secret rotation strategy or TLS/hosting configuration.
- Authentication/session design is intentionally lightweight; a production app should use an established auth/security solution and undergo penetration testing.
- SQL Server connections currently trust the local development server certificate by default; production must validate the certificate.

Never commit `.env` files, SQL backups, real credentials, customer tables or console logs containing OTPs.

- Current 3.5.x Spring Boot line is past OSS maintenance; move to a supported 4.x release after compatibility testing.
