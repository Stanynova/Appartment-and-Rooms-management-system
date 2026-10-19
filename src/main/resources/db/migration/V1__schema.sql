-- =====================================================================
-- Apartment and Room Management System (ARMS) - physical schema
-- Target: PostgreSQL 14+ (SRS section 2.4)
-- Derived from: SRS section 7.1 (data dictionary) and section 6.2 (ER model)
-- =====================================================================

-- ---------------------------------------------------------------------
-- FR-1.1: apartment records (name, location, address, description)
-- ---------------------------------------------------------------------
CREATE TABLE apartments (
    apartment_id   BIGSERIAL       PRIMARY KEY,
    apartment_name VARCHAR(100)    NOT NULL,
    location       VARCHAR(120),
    address        VARCHAR(255),
    description    VARCHAR(500),
    active         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP,
    CONSTRAINT uq_apartments_name UNIQUE (apartment_name)
);

-- ---------------------------------------------------------------------
-- FR-1.2 / FR-1.3: rooms with status model and rent amount
-- ---------------------------------------------------------------------
CREATE TABLE rooms (
    room_id            BIGSERIAL       PRIMARY KEY,
    apartment_id       BIGINT          NOT NULL,
    room_number        VARCHAR(10)     NOT NULL,
    room_type          VARCHAR(50),
    room_status        VARCHAR(20)     NOT NULL DEFAULT 'AVAILABLE',
    maintenance_reason VARCHAR(255),
    rent_amount        NUMERIC(12, 2)  NOT NULL,
    active             BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP,
    CONSTRAINT fk_rooms_apartment
        FOREIGN KEY (apartment_id) REFERENCES apartments (apartment_id),
    -- FR-1.2: room number unique within its apartment
    CONSTRAINT uq_rooms_apartment_number
        UNIQUE (apartment_id, room_number),
    CONSTRAINT ck_rooms_status
        CHECK (room_status IN ('AVAILABLE', 'OCCUPIED', 'UNDER_MAINTENANCE')),
    CONSTRAINT ck_rooms_rent_positive
        CHECK (rent_amount > 0)
);

CREATE INDEX idx_rooms_status ON rooms (room_status);
CREATE INDEX idx_rooms_apartment ON rooms (apartment_id);

-- ---------------------------------------------------------------------
-- FR-2.1: tenant profiles
-- ---------------------------------------------------------------------
CREATE TABLE tenants (
    tenant_id  BIGSERIAL       PRIMARY KEY,
    full_name  VARCHAR(120)    NOT NULL,
    phone      VARCHAR(20)     NOT NULL,
    email      VARCHAR(120),
    address    VARCHAR(255),
    created_at TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_tenants_phone ON tenants (phone);

-- ---------------------------------------------------------------------
-- FR-7.1 / FR-7.2 / FR-7.4: user accounts (passwords stored only as salted hashes)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id              BIGSERIAL       PRIMARY KEY,
    username             VARCHAR(50)     NOT NULL,
    full_name            VARCHAR(120),
    password_hash        VARCHAR(255)    NOT NULL,
    role                 VARCHAR(20)     NOT NULL,
    enabled              BOOLEAN         NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'PROPERTY_STAFF'))
);

-- ---------------------------------------------------------------------
-- FR-3.1: rental records
-- rent_amount is a snapshot of the room rent taken when the rental is
-- created so that FR-1.7 (new rent applies only to later rentals) holds.
-- ---------------------------------------------------------------------
CREATE TABLE rentals (
    rental_id     BIGSERIAL       PRIMARY KEY,
    tenant_id     BIGINT          NOT NULL,
    room_id       BIGINT          NOT NULL,
    start_date    DATE            NOT NULL,
    end_date      DATE            NOT NULL,
    rental_status VARCHAR(12)     NOT NULL,
    rent_amount   NUMERIC(12, 2)  NOT NULL,
    created_by    BIGINT,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,
    CONSTRAINT fk_rentals_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (tenant_id),
    CONSTRAINT fk_rentals_room
        FOREIGN KEY (room_id) REFERENCES rooms (room_id),
    -- FR-3.6: identity of the user who created the rental
    CONSTRAINT fk_rentals_created_by
        FOREIGN KEY (created_by) REFERENCES users (user_id),
    CONSTRAINT ck_rentals_status
        CHECK (rental_status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_rentals_dates
        CHECK (end_date > start_date),
    CONSTRAINT ck_rentals_rent_positive
        CHECK (rent_amount > 0)
);

-- Supports the double-booking check (FR-3.2 / BR-1)
CREATE INDEX idx_rentals_room_dates
    ON rentals (room_id, rental_status, start_date, end_date);
CREATE INDEX idx_rentals_tenant ON rentals (tenant_id);
CREATE INDEX idx_rentals_dates ON rentals (start_date, end_date);

-- ---------------------------------------------------------------------
-- FR-4.1: payment records.
-- A reversing entry (FR-4.6) carries a negative amount and points at the
-- payment it reverses; ordinary payments always have a positive amount.
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    payment_id          BIGSERIAL       PRIMARY KEY,
    rental_id           BIGINT          NOT NULL,
    amount              NUMERIC(12, 2)  NOT NULL,
    payment_date        DATE            NOT NULL,
    payment_method      VARCHAR(14)     NOT NULL,
    payment_status      VARCHAR(10)     NOT NULL,
    reverses_payment_id BIGINT,
    reversal_reason     VARCHAR(255),
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Payment extends TimestampedEntity, which writes updated_at on persist
    updated_at          TIMESTAMP,
    CONSTRAINT fk_payments_rental
        FOREIGN KEY (rental_id) REFERENCES rentals (rental_id),
    CONSTRAINT fk_payments_reverses
        FOREIGN KEY (reverses_payment_id) REFERENCES payments (payment_id),
    CONSTRAINT ck_payments_method
        CHECK (payment_method IN ('CASH', 'MOBILE_MONEY', 'BANK_TRANSFER')),
    CONSTRAINT ck_payments_status
        CHECK (payment_status IN ('PAID', 'PENDING', 'OVERDUE')),
    CONSTRAINT ck_payments_amount
        CHECK (amount > 0 OR reverses_payment_id IS NOT NULL)
);

CREATE INDEX idx_payments_rental ON payments (rental_id);
CREATE INDEX idx_payments_date ON payments (payment_date, payment_status);

-- ---------------------------------------------------------------------
-- FR-7.6: security-relevant events (sign-in, failed sign-in,
-- user account changes, rental cancellations)
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    audit_id   BIGSERIAL       PRIMARY KEY,
    event_type VARCHAR(40)     NOT NULL,
    username   VARCHAR(50),
    user_id    BIGINT,
    details    VARCHAR(500),
    event_time TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE INDEX idx_audit_time ON audit_logs (event_time DESC);
CREATE INDEX idx_audit_type ON audit_logs (event_type);
