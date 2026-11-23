# Deployment checklist — student portfolio MVP

Local development is described in [README.md](README.md). Do not expose this build to real customers yet.

- [ ] Run `mvn clean verify` successfully and manually exercise customer/admin workflows against SQL Server.
- [ ] Configure `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ADMIN_USERNAME` and `ADMIN_PASSWORD` as secrets; never put values in Git.
- [ ] Disable `DEMO_MODE` and implement a real SMS provider before public use.
- [ ] Enable HTTPS and SQL Server certificate validation (not `trustServerCertificate=true`).
- [ ] Add network/IP rate limiting for SMS, admin login and other exposed endpoints.
- [ ] Add monitoring, database migrations/backups, production auth hardening and security review.
- [ ] Run mobile browser smoke tests and capture real screenshots for the README.

The portfolio project can remain unhosted; GitHub source, CI, and screenshots are sufficient to show the work accurately.
