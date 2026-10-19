# Apartment and Room Management System (ARMS)

Spring Boot implementation of the **Apartment and Room Management System**, built to the
Software Requirements Specification *SRS_Apartment_and_Room_Management_System* release 1.0.

ARMS replaces manual, paper- and spreadsheet-based records used to track apartments, rooms,
tenants, rentals and rent payments, and provides occupancy and revenue reporting on top of a
single relational database.

---

## 1. Technology

| Concern | Choice | SRS reference |
|---|---|---|
| Language / runtime | Java 17 | 2.4 (Java 17 or later) |
| Framework | Spring Boot 3.5.16 (Web, Data JPA, Security, Validation) | 2.1, C-01 (Controller / Service / Repository layers) |
| Database | PostgreSQL 14+ (Flyway-managed schema) | 2.4, C-02, C-04 |
| API style | REST, JSON, UTF-8 | 3.4 |
| Authentication | Spring Security, session cookie (HttpOnly, SameSite=Strict), BCrypt salted hashes | 3.4, FR-7.1, FR-7.2 |
| Authorization | Role-based: `ADMIN`, `PROPERTY_STAFF` | FR-7.3, BR-7 |
| API documentation | springdoc-openapi / Swagger UI | – |
| PDF export | OpenPDF | FR-6.4 |
| Build | Maven | – |

---

## 2. Getting started

### Prerequisites

* JDK 17 or later
* Maven 3.9+ (or use your IDE's bundled Maven)
* PostgreSQL 14+ running locally

### 2.1 Create the database

```sql
CREATE ROLE arms WITH LOGIN PASSWORD 'arms';
CREATE DATABASE arms OWNER arms;
```

### 2.2 Configure (optional)

All settings can be overridden with environment variables:

| Variable | Default | Meaning |
|---|---|---|
| `ARMS_DB_URL` | `jdbc:postgresql://localhost:5432/arms` | JDBC URL |
| `ARMS_DB_USERNAME` | `arms` | Database user |
| `ARMS_DB_PASSWORD` | `arms` | Database password |
| `ARMS_SESSION_TIMEOUT` | `30m` | Inactivity timeout before the session is terminated (FR-7.5) |
| `ARMS_CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Allowed browser origins |
| `ARMS_DEFAULT_ADMIN_USERNAME` | `admin` | Seed administrator (first start only) |
| `ARMS_DEFAULT_ADMIN_PASSWORD` | `admin123` | Seed password (first start only) |

### 2.3 Run

```bash
mvn clean spring-boot:run
```

On first start the schema is created by Flyway (`src/main/resources/db/migration/V1__schema.sql`)
and an administrator account is seeded.

### 2.4 Sign in

1. `POST /api/auth/login` with `{"username": "admin", "password": "admin123"}` — or open
   **http://localhost:8080/swagger-ui.html** and use *Authorize*.
2. The account is forced to change its password at first sign-in (FR-4.7 stimulus): call
   `POST /api/auth/change-password` with `{"currentPassword": "admin123", "newPassword": "..."}`.
   The session ends and you sign in again with the new password.
3. Sign-in, failed sign-ins and account changes are written to the audit log (FR-7.6).

---

## 3. Architecture

```
com.arms
├── config        DataInitializer (seed administrator on first start)
├── controller    REST endpoints, validation, role checks (@PreAuthorize)
├── dto           Request/response records (JSON contract)
├── entity        JPA entities + status enums (data dictionary, SRS 7.1)
├── exception     ApiError document + GlobalExceptionHandler
├── mapper        Entity → DTO mapping (inside transactions)
├── repository    Spring Data JPA repositories (parameterised queries only, NFR-SE-3)
├── security      SecurityConfig, session principal, password-change gate
└── service       Business rules and transactions (SRS 4.x, 5.5)
```

Layering follows constraint **C-01**: controllers never contain business rules; services own
transactions and enforce every business rule; repositories are the only SQL-touching layer.

---

## 4. Roles and permissions

| Function | Administrator | Property staff |
|---|---|---|
| View dashboard, availability, search | ✔ | ✔ |
| Register / update tenants | ✔ | ✔ |
| Record / amend / cancel / complete rentals | ✔ | ✔ |
| Record payments, reverse payments | ✔ | ✔ |
| Apartments & rooms: create, update, rent, maintenance, deactivate (BR-7) | ✔ | ✖ |
| Reports and PDF export | ✔ | ✖ |
| User administration (FR-7.4) | ✔ | ✖ |
| Audit log (FR-7.6) | ✔ | ✖ |

---

## 5. API reference

Base URL: `http://localhost:8080/api` — all endpoints require an authenticated session
except login/logout. Errors always return the uniform `ApiError` document
(`timestamp, status, error, code, message, path[, confirmationRequired, fieldErrors]`).

### Authentication (FR-7.1, FR-7.5)

| Method & path | Description | Access |
|---|---|---|
| `POST /auth/login` | Sign in; opens the session cookie | Public |
| `POST /auth/logout` | End the session | Public |
| `GET /auth/me` | Current account | Any signed-in user |
| `POST /auth/change-password` | Change own password (ends the session) | Any signed-in user |

### Apartments (FR-1.1, FR-1.6)

| Method & path | Description | Access |
|---|---|---|
| `GET /apartments` | List all apartments | Any |
| `GET /apartments/{id}` | Read one | Any |
| `POST /apartments` | Create (unique name enforced) | ADMIN |
| `PUT /apartments/{id}` | Update | ADMIN |
| `DELETE /apartments/{id}` | **Deactivate** (never a delete, FR-1.6) | ADMIN |
| `POST /apartments/{id}/restore` | Re-activate | ADMIN |

### Rooms (FR-1.2 – FR-1.7)

| Method & path | Description | Access |
|---|---|---|
| `GET /rooms?apartmentId=&status=` | List / filter by status (FR-5.2) | Any |
| `GET /rooms/{id}` | Read one | Any |
| `POST /rooms` | Create (number unique within apartment, initial status `AVAILABLE`) | ADMIN |
| `PUT /rooms/{id}` | Update number / type | ADMIN |
| `PUT /rooms/{id}/rent` | Change rent — applies only to later rentals (FR-1.7) | ADMIN |
| `POST /rooms/{id}/maintenance` | `{reason, confirm}` — warns when an active rental exists, `confirm:true` proceeds (FR-1.4, FR-1.5) | ADMIN |
| `POST /rooms/{id}/release` | Leave maintenance → `AVAILABLE`/`OCCUPIED` | ADMIN |
| `DELETE /rooms/{id}` | Deactivate (FR-1.6) | ADMIN |
| `POST /rooms/{id}/restore` | Re-activate | ADMIN |

### Tenants (FR-2.1 – FR-2.5)

| Method & path | Description | Access |
|---|---|---|
| `GET /tenants?q=` | Search by name or phone (FR-2.3) | Any |
| `GET /tenants/{id}` | Read one | Any |
| `GET /tenants/{id}/rentals` | Active and past rentals (FR-6.3) | Any |
| `POST /tenants` | Register | Any |
| `PUT /tenants/{id}` | Update profile | Any |
| `DELETE /tenants/{id}` | Only when no rental refers to the tenant (FR-2.5) → else `409` | ADMIN |

### Rentals (FR-3.1 – FR-3.7)

| Method & path | Description | Access |
|---|---|---|
| `POST /rentals` | Record a rental: validates period, no overlap (**BR-1**), room not under maintenance (**BR-2**), snapshots the rent, sets the room `OCCUPIED` atomically, records creator (FR-3.6) | Any |
| `GET /rentals?tenantId=&roomId=&status=` | Filtered list | Any |
| `GET /rentals/{id}` | Detail incl. `rentDue`, `totalPaid`, `balanceOutstanding` (FR-4.3) | Any |
| `PUT /rentals/{id}/dates` | Amend dates — only before the start, availability re-verified (FR-3.4) | Any |
| `POST /rentals/{id}/cancel` | Only before the start; releases the room; audited (FR-3.5, FR-7.6) | Any |
| `POST /rentals/{id}/complete` | Marks completed and frees the room (FR-3.3) | Any |

### Payments (FR-4.1 – FR-4.6)

| Method & path | Description | Access |
|---|---|---|
| `POST /rentals/{id}/payments` | Record `{amount, paymentDate, method, status}` — several per rental (FR-4.5) | Any |
| `GET /rentals/{id}/payments` | Payments + rent due + balance (FR-4.3) | Any |
| `GET /payments/{id}` | One payment | Any |
| `POST /payments/{id}/reverse` | `{reason}` — reversing entry; **there is no delete** (FR-4.6) | Any |

### Search and availability (FR-5.1 – FR-5.3)

| Method & path | Description | Access |
|---|---|---|
| `GET /availability?status=&apartmentId=&from=&to=` | Rooms grouped by apartment; with a period only rooms that are `AVAILABLE` **and** free of an active rental are returned | Any |
| `GET /search?type=&q=` | `type` = `apartment` \| `room` \| `tenant` (or all); matches name, room number, phone | Any |

### Reporting (FR-6.1 – FR-6.5)

| Method & path | Description | Access |
|---|---|---|
| `GET /dashboard` | Today's occupancy, maintenance count, active rentals (FR-6.5) | Any |
| `GET /reports/occupancy?from=&to=` | Available / occupied rooms, occupancy %, room-days (FR-6.1) | ADMIN |
| `GET /reports/revenue?from=&to=` | Payments received grouped by apartment (FR-6.2) | ADMIN |
| `GET /reports/rentals?tenantId=&roomId=` | Active and past rentals for a tenant or room (FR-6.3) | ADMIN |
| `GET /reports/occupancy.pdf?from=&to=` | PDF export, A4 (FR-6.4) | ADMIN |
| `GET /reports/revenue.pdf?from=&to=` | PDF export, A4 (FR-6.4) | ADMIN |

### Administration (FR-7.4, FR-7.6)

| Method & path | Description | Access |
|---|---|---|
| `GET /users`, `GET /users/{id}` | List / read accounts (never expose password material) | ADMIN |
| `POST /users` | Create account; must change password at first sign-in | ADMIN |
| `PUT /users/{id}` | Change full name / role | ADMIN |
| `POST /users/{id}/disable`, `/enable` | Disable / re-enable (self-disable refused) | ADMIN |
| `POST /users/{id}/reset-password` | Set a new password; forces change at next sign-in | ADMIN |
| `GET /audit-logs?eventType=&limit=` | Sign-ins, failed sign-ins, account changes, rental cancellations | ADMIN |

Dates are ISO-8601 (`YYYY-MM-DD`) in requests; reports default to the current month
(`from` = first day of month, `to` = today). Monetary values are `BigDecimal`
(decimal(12,2), NFR-QA-4).

---

## 6. Business rules implemented (SRS 5.5)

| Rule | Where |
|---|---|
| BR-1 No two active rentals overlap on a room | `RentalService.assertNoOverlap` (create + amend) |
| BR-2 A room under maintenance cannot be allocated | `RentalService.create` |
| BR-3 An occupied room has an active rental | Room status is only changed together with rental state (`RentalService`) |
| BR-4 Tenants keep every rental record | Deletion only refused when rentals exist (`TenantService.delete`) |
| BR-5 Balance = rent due − payments recorded | `PaymentService.summary`, `DtoMapper` |
| BR-6 A rental cancelled before its start needs no payment | Cancellation releases the room; payments on cancelled rentals are refused |
| BR-7 Only an administrator deactivates | `@PreAuthorize("hasRole('ADMIN')")` on the delete endpoints |

---

## 7. Design notes and deliberate decisions

* **Rent snapshot** — `rentals.rent_amount` is added to the data dictionary's fields so that
  FR-1.7 (a new rent applies only to rentals created after the change) can be enforced and
  so that BR-5 has a stable "rent due".
* **Reversing entries** — a reversal stores a *negative* amount and points at the original
  payment; the schema check `amount > 0 OR reverses_payment_id IS NOT NULL` keeps ordinary
  payments positive (FR-4.6).
* **Balance counts `PAID` entries only** — `PENDING`/`OVERDUE` represent money not yet
  received; a reversal carries the original status so it cancels it out exactly.
* **Occupancy for a period** is computed as *occupied room-days / rentable room-days*
  (merged intervals of `ACTIVE`/`COMPLETED` rentals covering the period), so a partially
  occupied month is reported correctly rather than as a single snapshot.
* **Availability with dates** follows the SRS glossary strictly: `status = AVAILABLE` **and**
  no active rental for those dates.
* **No hard deletes** for apartments and rooms (FR-1.6) — deactivation with reactivation.
* **Sessions** — `server.servlet.session.timeout` is the inactivity timeout (FR-7.5);
  the cookie is HttpOnly and SameSite=Strict. CSRF protection is disabled because API
  clients do not carry CSRF tokens; the SameSite=Strict session cookie mitigates
  cross-site request forgery for browser clients. For a network-accessible deployment,
  front the application with HTTPS (NFR-SE-1) — TLS is terminated at the proxy.
* **Audit trail** — `AuditService.record` joins the caller's transaction, so the audit entry
  and the operation it describes commit or roll back together (NFR-SF-2).
* **i18n** — every user-visible message lives in `messages.properties` /
  `ValidationMessages.properties` (SRS 7.4): add a `messages_fr.properties` bundle to
  introduce a language without touching code.

## 8. Known limitations

* Occupancy history is derived from rental periods; room-status changes are not kept as a
  separate history table (only security events are audited, per FR-7.6).
* Availability with a date range issues one overlap query per candidate room — acceptable at
  the scale described in the SRS, but it would be the first thing to optimise for
  NFR-PE-1 under large inventories.
* HTTPS (NFR-SE-1) and daily database backups (NFR-SF-1) are deployment responsibilities and
  are therefore not implemented in code.
