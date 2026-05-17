-- ============================================================
-- V1__create_schema.sql — Auction Service Schema
-- ============================================================

-- ENUMs
CREATE TYPE auction_type        AS ENUM ('SPOT','BULK','LOT');
CREATE TYPE auction_status      AS ENUM ('DRAFT','LIVE','COMPLETED','AWARDED','NO_BIDS','CANCELLED');
CREATE TYPE auction_event_type  AS ENUM ('CREATED','LAUNCHED','EXTENDED','COMPLETED','AWARDED','CANCELLED','PLACEMENT_FAILURE','OVERRIDE','CONTRACT_CREATED');
CREATE TYPE rate_unit           AS ENUM ('PER_TRIP','PER_MT','PER_KM');
CREATE TYPE allocation_mode     AS ENUM ('SINGLE','SPLIT');
CREATE TYPE allocation_rank     AS ENUM ('L1','L2','L3');
CREATE TYPE contract_status     AS ENUM ('ACTIVE','EXPIRING_SOON','EXPIRED','TERMINATED');
CREATE TYPE contract_type       AS ENUM ('BULK','LOT');
CREATE TYPE rfi_status          AS ENUM ('DRAFT','PUBLISHED','CLOSED');
CREATE TYPE rfq_status          AS ENUM ('DRAFT','PUBLISHED','EVALUATING','AWARDED','CANCELLED');
CREATE TYPE vendor_resp_status  AS ENUM ('PENDING','RESPONDED','DECLINED');
CREATE TYPE booking_status      AS ENUM ('PENDING_AUCTION','READY_FOR_DISPATCH');
CREATE TYPE notif_category      AS ENUM ('AUCTIONS','AWARDS','CONTRACTS','SOURCING','SYSTEM');
CREATE TYPE internal_role       AS ENUM ('ADMIN','OPS','PROCUREMENT','FINANCE','EXECUTIVE');

-- implicit casts so Hibernate @Enumerated(EnumType.STRING) works with native PG enums
CREATE CAST (varchar AS auction_type)       WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS auction_status)     WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS auction_event_type) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS rate_unit)          WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS allocation_mode)    WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS allocation_rank)    WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS contract_status)    WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS contract_type)      WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS rfi_status)         WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS rfq_status)         WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS vendor_resp_status) WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS booking_status)     WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS notif_category)     WITH INOUT AS IMPLICIT;
CREATE CAST (varchar AS internal_role)      WITH INOUT AS IMPLICIT;

-- internal_users
CREATE TABLE internal_users (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    mobile        VARCHAR(20),
    role          internal_role NOT NULL DEFAULT 'OPS',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_internal_users PRIMARY KEY (id)
);

-- vendors (reference copy)
CREATE TABLE vendors (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255),
    score      NUMERIC(4,1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_vendors PRIMARY KEY (id)
);

-- bookings
CREATE TABLE bookings (
    id           UUID           NOT NULL DEFAULT gen_random_uuid(),
    lane         VARCHAR(255)   NOT NULL,
    vehicle_type VARCHAR(100),
    commodity    VARCHAR(255),
    quantity     NUMERIC(12,2),
    uom          VARCHAR(50),
    loading_date DATE,
    status       booking_status NOT NULL DEFAULT 'PENDING_AUCTION',
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT pk_bookings PRIMARY KEY (id)
);

-- auctions
CREATE TABLE auctions (
    id                         UUID           NOT NULL DEFAULT gen_random_uuid(),
    title                      VARCHAR(500)   NOT NULL,
    type                       auction_type   NOT NULL,
    status                     auction_status NOT NULL DEFAULT 'DRAFT',
    created_by_id              UUID,
    created_by_role            VARCHAR(50),
    created_at                 TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ    NOT NULL DEFAULT now(),
    start_at                   TIMESTAMPTZ,
    completed_at               TIMESTAMPTZ,
    contract_start_date        DATE,
    contract_end_date          DATE,
    min_bid_decrement          NUMERIC(12,2)  DEFAULT 0,
    extension_trigger_minutes  INT            DEFAULT 5,
    extension_duration_minutes INT            DEFAULT 10,
    max_extensions             INT            DEFAULT 3,
    bidding_window_minutes     INT            DEFAULT 60,
    booking_id                 UUID,
    region                     VARCHAR(255),
    award_deadline             TIMESTAMPTZ,
    CONSTRAINT pk_auctions PRIMARY KEY (id)
);
CREATE INDEX idx_auctions_status ON auctions(status);
CREATE INDEX idx_auctions_type   ON auctions(type);

-- auction_lanes
CREATE TABLE auction_lanes (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    auction_id        UUID            NOT NULL,
    lane              VARCHAR(500)    NOT NULL,
    region            VARCHAR(255),
    vehicle_type      VARCHAR(100),
    capacity_mt       NUMERIC(12,2),
    rate_unit         rate_unit       NOT NULL DEFAULT 'PER_TRIP',
    ceiling_rate      NUMERIC(12,2),
    estimated_trips   INT,
    base_price_source VARCHAR(20)     DEFAULT 'MANUAL',
    allocation_mode   allocation_mode NOT NULL DEFAULT 'SINGLE',
    l1_allocation_pct NUMERIC(5,2),
    l2_allocation_pct NUMERIC(5,2),
    l3_allocation_pct NUMERIC(5,2),
    timer_ends_at     TIMESTAMPTZ,
    extension_count   INT             NOT NULL DEFAULT 0,
    bid_count         INT             NOT NULL DEFAULT 0,
    rejection_reason  TEXT,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_auction_lanes PRIMARY KEY (id),
    CONSTRAINT fk_al_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
);
CREATE INDEX idx_al_auction_id ON auction_lanes(auction_id);

-- auction_invited_vendors
CREATE TABLE auction_invited_vendors (
    auction_id UUID NOT NULL,
    vendor_id  UUID NOT NULL,
    CONSTRAINT pk_aiv PRIMARY KEY (auction_id, vendor_id),
    CONSTRAINT fk_aiv_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
);

-- auction_lane_eligible_vendors
CREATE TABLE auction_lane_eligible_vendors (
    lane_id   UUID NOT NULL,
    vendor_id UUID NOT NULL,
    CONSTRAINT pk_alev PRIMARY KEY (lane_id, vendor_id),
    CONSTRAINT fk_alev_lane FOREIGN KEY (lane_id) REFERENCES auction_lanes(id) ON DELETE CASCADE
);

-- auction_bids
CREATE TABLE auction_bids (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    auction_id  UUID        NOT NULL,
    lane_id     UUID        NOT NULL,
    vendor_id   UUID        NOT NULL,
    vendor_name VARCHAR(255),
    amount      NUMERIC(12,2) NOT NULL,
    bid_rank    INT,
    is_current  BOOLEAN     NOT NULL DEFAULT true,
    placed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_auction_bids PRIMARY KEY (id),
    CONSTRAINT fk_ab_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    CONSTRAINT fk_ab_lane    FOREIGN KEY (lane_id)    REFERENCES auction_lanes(id) ON DELETE CASCADE
);
CREATE INDEX idx_ab_lane_id    ON auction_bids(lane_id);
CREATE INDEX idx_ab_auction_id ON auction_bids(auction_id);

-- auction_events
CREATE TABLE auction_events (
    id              UUID               NOT NULL DEFAULT gen_random_uuid(),
    auction_id      UUID               NOT NULL,
    event_type      auction_event_type NOT NULL,
    message         TEXT,
    actor           VARCHAR(255),
    event_timestamp TIMESTAMPTZ        NOT NULL DEFAULT now(),
    CONSTRAINT pk_auction_events PRIMARY KEY (id),
    CONSTRAINT fk_ae_auction FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE
);
CREATE INDEX idx_ae_auction_id ON auction_events(auction_id);

-- award_decisions
CREATE TABLE award_decisions (
    id                 UUID            NOT NULL DEFAULT gen_random_uuid(),
    lane_id            UUID            NOT NULL,
    auction_id         UUID            NOT NULL,
    vendor_id          UUID            NOT NULL,
    vendor_name        VARCHAR(255),
    allocation_rank    allocation_rank NOT NULL,
    awarded_bid_rank   allocation_rank,
    awarded_amount     NUMERIC(12,2)   NOT NULL,
    override_reason    TEXT,
    allocation_percent NUMERIC(5,2),
    decided_at         TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_award_decisions PRIMARY KEY (id),
    CONSTRAINT fk_ad_lane    FOREIGN KEY (lane_id)    REFERENCES auction_lanes(id) ON DELETE CASCADE,
    CONSTRAINT fk_ad_auction FOREIGN KEY (auction_id) REFERENCES auctions(id)      ON DELETE CASCADE
);
CREATE INDEX idx_ad_auction_id ON award_decisions(auction_id);

-- contracts
CREATE TABLE contracts (
    id                        UUID            NOT NULL DEFAULT gen_random_uuid(),
    source_auction_id         UUID,
    contract_type             contract_type,
    vendor_id                 UUID            NOT NULL,
    vendor_name               VARCHAR(255),
    lane                      VARCHAR(500),
    region                    VARCHAR(255),
    vehicle_type              VARCHAR(100),
    contracted_rate           NUMERIC(12,2)   NOT NULL,
    rate_unit                 rate_unit       NOT NULL DEFAULT 'PER_TRIP',
    volume_allocation_percent NUMERIC(5,2),
    allocation_rank           allocation_rank,
    start_date                DATE,
    end_date                  DATE,
    estimated_trips           INT,
    status                    contract_status NOT NULL DEFAULT 'ACTIVE',
    l1_override_reason        TEXT,
    rate_synced_to_tms        BOOLEAN         NOT NULL DEFAULT false,
    rate_deviation_open       BOOLEAN         NOT NULL DEFAULT false,
    created_at                TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_contracts PRIMARY KEY (id)
);
CREATE INDEX idx_contracts_status ON contracts(status);

-- placement_failures
CREATE TABLE placement_failures (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid(),
    contract_id          UUID        NOT NULL,
    failed_vendor        VARCHAR(255),
    replacement_vendor   VARCHAR(255),
    original_rate        NUMERIC(12,2),
    replacement_rate     NUMERIC(12,2),
    differential         NUMERIC(12,2),
    debit_note_triggered BOOLEAN     DEFAULT false,
    recorded_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_placement_failures PRIMARY KEY (id),
    CONSTRAINT fk_pf_contract FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE
);

-- rfis
CREATE TABLE rfis (
    id                 UUID        NOT NULL DEFAULT gen_random_uuid(),
    title              VARCHAR(500) NOT NULL,
    description        TEXT,
    deadline           TIMESTAMPTZ,
    status             rfi_status  NOT NULL DEFAULT 'DRAFT',
    message_to_vendor  TEXT,
    template_file_name VARCHAR(500),
    created_by_id      UUID,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_rfis PRIMARY KEY (id)
);
CREATE INDEX idx_rfis_status ON rfis(status);

-- rfi_target_emails
CREATE TABLE rfi_target_emails (
    rfi_id UUID        NOT NULL,
    email  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_rte PRIMARY KEY (rfi_id, email),
    CONSTRAINT fk_rte_rfi FOREIGN KEY (rfi_id) REFERENCES rfis(id) ON DELETE CASCADE
);

-- rfi_vendor_tracking
CREATE TABLE rfi_vendor_tracking (
    id                 UUID               NOT NULL DEFAULT gen_random_uuid(),
    rfi_id             UUID               NOT NULL,
    vendor_id_or_email VARCHAR(255)       NOT NULL,
    name               VARCHAR(255),
    status             vendor_resp_status NOT NULL DEFAULT 'PENDING',
    CONSTRAINT pk_rvt PRIMARY KEY (id),
    CONSTRAINT fk_rvt_rfi FOREIGN KEY (rfi_id) REFERENCES rfis(id) ON DELETE CASCADE
);
CREATE INDEX idx_rvt_rfi_id ON rfi_vendor_tracking(rfi_id);

-- rfqs
CREATE TABLE rfqs (
    id                 UUID        NOT NULL DEFAULT gen_random_uuid(),
    title              VARCHAR(500) NOT NULL,
    deadline           TIMESTAMPTZ,
    status             rfq_status  NOT NULL DEFAULT 'DRAFT',
    message_to_vendor  TEXT,
    template_file_name VARCHAR(500),
    created_by_id      UUID,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_rfqs PRIMARY KEY (id)
);
CREATE INDEX idx_rfqs_status ON rfqs(status);

-- rfq_target_emails
CREATE TABLE rfq_target_emails (
    rfq_id UUID        NOT NULL,
    email  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_qte PRIMARY KEY (rfq_id, email),
    CONSTRAINT fk_qte_rfq FOREIGN KEY (rfq_id) REFERENCES rfqs(id) ON DELETE CASCADE
);

-- rfq_vendor_tracking
CREATE TABLE rfq_vendor_tracking (
    id                 UUID               NOT NULL DEFAULT gen_random_uuid(),
    rfq_id             UUID               NOT NULL,
    vendor_id_or_email VARCHAR(255)       NOT NULL,
    name               VARCHAR(255),
    status             vendor_resp_status NOT NULL DEFAULT 'PENDING',
    CONSTRAINT pk_qvt PRIMARY KEY (id),
    CONSTRAINT fk_qvt_rfq FOREIGN KEY (rfq_id) REFERENCES rfqs(id) ON DELETE CASCADE
);
CREATE INDEX idx_qvt_rfq_id ON rfq_vendor_tracking(rfq_id);

-- rfq_responses
CREATE TABLE rfq_responses (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    rfq_id      UUID,
    vendor_name VARCHAR(255),
    file_name   VARCHAR(500),
    uploaded_by VARCHAR(255),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_rfq_responses PRIMARY KEY (id),
    CONSTRAINT fk_qr_rfq FOREIGN KEY (rfq_id) REFERENCES rfqs(id) ON DELETE SET NULL
);

-- rfq_response_rows
CREATE TABLE rfq_response_rows (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    response_id  UUID        NOT NULL,
    lane         VARCHAR(500) NOT NULL,
    vehicle_type VARCHAR(100),
    price        NUMERIC(12,2) NOT NULL,
    CONSTRAINT pk_qrr PRIMARY KEY (id),
    CONSTRAINT fk_qrr_response FOREIGN KEY (response_id) REFERENCES rfq_responses(id) ON DELETE CASCADE
);
CREATE INDEX idx_qrr_response_id ON rfq_response_rows(response_id);

-- notifications
CREATE TABLE notifications (
    id         UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID            NOT NULL,
    category   notif_category  NOT NULL,
    title      VARCHAR(500)    NOT NULL,
    message    TEXT,
    is_read    BOOLEAN         NOT NULL DEFAULT false,
    deep_link  VARCHAR(1000),
    created_at TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);
CREATE INDEX idx_notif_user_id ON notifications(user_id);
CREATE INDEX idx_notif_is_read ON notifications(is_read);
